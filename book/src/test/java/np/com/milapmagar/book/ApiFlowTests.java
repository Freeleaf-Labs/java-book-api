package np.com.milapmagar.book;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// End-to-end tests: real HTTP layer, real security filters, real Postgres (in Docker).
// Each test walks through one feature the way a client would.
@SpringBootTest(properties = {
        "app.admin.email=admin@test.dev",
        "app.admin.password=admin12345"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiFlowTests {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mvc;

    /** DOCS **/

    @Test
    void rootOpensSwaggerAndTheDocsArePublic() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/swagger-ui/index.html"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/books']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                // public endpoints carry no padlock, protected ones do
                .andExpect(jsonPath("$.paths['/api/v1/books'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/books'].post.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].get.security[0].bearerAuth").exists());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    /** CATALOGUE (public) **/

    @Test
    void anyoneCanBrowseTheSeededCatalogue() throws Exception {
        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Business")) // sorted by name
                .andExpect(jsonPath("$[0].slug").value("business"));

        mvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content", hasSize(20)))
                .andExpect(jsonPath("$.content[0].category.slug").exists())
                .andExpect(jsonPath("$.content[0].hasFile").value(false));

        // search + category filter + sort, all at once
        mvc.perform(get("/api/v1/books?search=JAVA&category=programming&sort=title,asc&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Effective Java"))
                .andExpect(jsonPath("$.content[1].title").value("Eloquent JavaScript"))
                .andExpect(jsonPath("$.content[2].title").value("Head First Java"))
                .andExpect(jsonPath("$.totalElements").value(3));

        mvc.perform(get("/api/v1/books?author=orwell"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Nineteen Eighty-Four"));

        mvc.perform(get("/api/v1/books?size=500")).andExpect(jsonPath("$.size").value(50)); // capped
        mvc.perform(get("/api/v1/books?sort=isbn,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot sort by 'isbn'"));
        mvc.perform(get("/api/v1/books?page=abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/books/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book with id 999999 not found"));
    }

    /** AUTH **/

    @Test
    void registerLoginRefreshLogout() throws Exception {
        String email = uniqueEmail();
        String body = """
                {"email": "%s", "password": "%s", "displayName": "Asha"}""".formatted(email, PASSWORD);

        mvc.perform(json(post("/api/v1/auth/register"), body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        // same email again, in different letter case
        mvc.perform(json(post("/api/v1/auth/register"), body.replace(email, email.toUpperCase())))
                .andExpect(status().isConflict());
        mvc.perform(json(post("/api/v1/auth/register"), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.displayName").exists());

        mvc.perform(json(post("/api/v1/auth/login"), """
                        {"email": "%s", "password": "wrong-password"}""".formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        MvcResult login = mvc.perform(json(post("/api/v1/auth/login"), """
                        {"email": "%s", "password": "%s"}""".formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
                .andReturn();
        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        Cookie refreshCookie = login.getResponse().getCookie("refresh_token");
        assertThat(refreshCookie).isNotNull();

        // the token opens protected endpoints; no token or a broken one does not
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token + "tampered"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Asha"));
        mvc.perform(json(patch("/api/v1/users/me"), """
                        {"displayName": "Asha K."}""").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Asha K."));

        // refresh rotates: a new cookie comes back and the old one is dead
        mvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
        MvcResult refreshed = mvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.displayName").value("Asha K."))
                .andReturn();
        Cookie rotatedCookie = refreshed.getResponse().getCookie("refresh_token");
        assertThat(rotatedCookie).isNotNull();
        assertThat(rotatedCookie.getValue()).isNotEqualTo(refreshCookie.getValue());
        mvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie)).andExpect(status().isUnauthorized());

        // a stale Authorization header must not get in the way of the auth endpoints
        mvc.perform(post("/api/v1/auth/logout").cookie(rotatedCookie).header("Authorization", "Bearer expired.or.garbage"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
        mvc.perform(post("/api/v1/auth/refresh").cookie(rotatedCookie)).andExpect(status().isUnauthorized());
    }

    /** BOOKS + CATEGORIES (admin writes) **/

    @Test
    void onlyAdminsCanChangeTheCatalogue() throws Exception {
        String user = "Bearer " + registerAndLogin(uniqueEmail());
        String admin = "Bearer " + login("admin@test.dev", "admin12345");
        String isbn = "978" + (1000000000L + (long) (Math.random() * 8999999999L));

        MvcResult category = mvc.perform(json(post("/api/v1/categories"), """
                        {"name": "History %s"}""".formatted(isbn)).header("Authorization", admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("history-" + isbn))
                .andReturn();
        int categoryId = JsonPath.read(category.getResponse().getContentAsString(), "$.id");
        mvc.perform(json(post("/api/v1/categories"), """
                        {"name": "history %s"}""".formatted(isbn)).header("Authorization", admin))
                .andExpect(status().isConflict());

        String book = """
                {"title": "Sapiens", "author": "Yuval Noah Harari", "publisher": "Harper",
                 "publishedYear": 2011, "isbn": "%s", "description": "A brief history of humankind.",
                 "categoryId": %d}""".formatted(isbn, categoryId);

        mvc.perform(json(post("/api/v1/books"), book)).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/v1/books"), book).header("Authorization", user))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        MvcResult created = mvc.perform(json(post("/api/v1/books"), book).header("Authorization", admin))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/books/")))
                .andExpect(jsonPath("$.title").value("Sapiens"))
                .andExpect(jsonPath("$.publishedYear").value(2011))
                .andExpect(jsonPath("$.category.id").value(categoryId))
                .andExpect(jsonPath("$.ratingCount").value(0))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();
        String bookUrl = created.getResponse().getHeader("Location");

        mvc.perform(json(post("/api/v1/books"), book).header("Authorization", admin))
                .andExpect(status().isConflict()); // same ISBN
        mvc.perform(json(post("/api/v1/books"), "{}").header("Authorization", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.categoryId").exists());
        mvc.perform(json(post("/api/v1/books"), book.replace(isbn, "").replace("" + categoryId, "999999"))
                        .header("Authorization", admin))
                .andExpect(status().isBadRequest()); // unknown category

        // a category that has books cannot be deleted
        mvc.perform(delete("/api/v1/categories/" + categoryId).header("Authorization", admin))
                .andExpect(status().isConflict());

        mvc.perform(json(put(bookUrl), book.replace("Sapiens", "Sapiens (2nd ed.)")).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Sapiens (2nd ed.)"));
        mvc.perform(get(bookUrl)).andExpect(jsonPath("$.title").value("Sapiens (2nd ed.)"));

        mvc.perform(delete(bookUrl).header("Authorization", user)).andExpect(status().isForbidden());
        mvc.perform(delete(bookUrl).header("Authorization", admin)).andExpect(status().isNoContent());
        mvc.perform(get(bookUrl)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/categories/" + categoryId).header("Authorization", admin))
                .andExpect(status().isNoContent());
    }

    /** REVIEWS **/

    @Test
    void reviewsKeepTheBookRatingUpToDate() throws Exception {
        String admin = "Bearer " + login("admin@test.dev", "admin12345");
        String asha = "Bearer " + registerAndLogin(uniqueEmail());
        String bina = "Bearer " + registerAndLogin(uniqueEmail());

        // a fresh book, so other tests cannot disturb the numbers
        MvcResult categories = mvc.perform(get("/api/v1/categories")).andReturn();
        int categoryId = JsonPath.read(categories.getResponse().getContentAsString(), "$[0].id");
        MvcResult created = mvc.perform(json(post("/api/v1/books"), """
                        {"title": "Review target", "author": "Nobody", "categoryId": %d}""".formatted(categoryId))
                        .header("Authorization", admin))
                .andExpect(status().isCreated())
                .andReturn();
        String bookUrl = created.getResponse().getHeader("Location");

        mvc.perform(json(put(bookUrl + "/reviews/mine"), """
                {"rating": 5, "comment": "Loved it"}""")).andExpect(status().isUnauthorized());
        mvc.perform(get(bookUrl + "/reviews/mine").header("Authorization", asha)).andExpect(status().isNotFound());
        mvc.perform(json(put(bookUrl + "/reviews/mine"), """
                        {"rating": 9}""").header("Authorization", asha))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.rating").exists());

        MvcResult ashaReview = mvc.perform(json(put(bookUrl + "/reviews/mine"), """
                        {"rating": 5, "comment": "Loved it"}""").header("Authorization", asha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.user.displayName").value("Test User"))
                .andExpect(jsonPath("$.user.email").doesNotExist())
                .andReturn();
        int ashaReviewId = JsonPath.read(ashaReview.getResponse().getContentAsString(), "$.id");
        mvc.perform(json(put(bookUrl + "/reviews/mine"), """
                        {"rating": 3}""").header("Authorization", bina))
                .andExpect(status().isOk());

        mvc.perform(get(bookUrl))
                .andExpect(jsonPath("$.ratingAvg").value(4.0))
                .andExpect(jsonPath("$.ratingCount").value(2));
        mvc.perform(get(bookUrl + "/reviews")) // public
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // saving again changes the existing review instead of adding a second one
        mvc.perform(json(put(bookUrl + "/reviews/mine"), """
                        {"rating": 4, "comment": "Still good"}""").header("Authorization", asha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ashaReviewId));
        mvc.perform(get(bookUrl))
                .andExpect(jsonPath("$.ratingAvg").value(3.5))
                .andExpect(jsonPath("$.ratingCount").value(2));
        mvc.perform(get(bookUrl + "/reviews/mine").header("Authorization", asha))
                .andExpect(jsonPath("$.comment").value("Still good"));

        // only the owner (or an admin) may delete a review
        mvc.perform(delete("/api/v1/reviews/" + ashaReviewId).header("Authorization", bina))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/reviews/" + ashaReviewId).header("Authorization", asha))
                .andExpect(status().isNoContent());
        mvc.perform(get(bookUrl))
                .andExpect(jsonPath("$.ratingAvg").value(3.0))
                .andExpect(jsonPath("$.ratingCount").value(1));

        // deleting the book takes its remaining review with it
        mvc.perform(delete(bookUrl).header("Authorization", admin)).andExpect(status().isNoContent());
    }

    /** ADMIN **/

    @Test
    void adminsManageUsers() throws Exception {
        String email = uniqueEmail();
        String user = "Bearer " + registerAndLogin(email);
        String admin = "Bearer " + login("admin@test.dev", "admin12345");

        mvc.perform(get("/api/v1/admin/stats")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/stats").header("Authorization", user)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/stats").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isNumber())
                .andExpect(jsonPath("$.books").isNumber())
                .andExpect(jsonPath("$.resources.pending").value(0));

        MvcResult found = mvc.perform(get("/api/v1/admin/users?search=" + email.toUpperCase()).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value(email))
                .andReturn();
        int userId = JsonPath.read(found.getResponse().getContentAsString(), "$.content[0].id");

        MvcResult me = mvc.perform(get("/api/v1/users/me").header("Authorization", admin)).andReturn();
        int adminId = JsonPath.read(me.getResponse().getContentAsString(), "$.id");
        mvc.perform(json(patch("/api/v1/admin/users/" + adminId), """
                        {"enabled": false}""").header("Authorization", admin))
                .andExpect(status().isConflict()); // not yourself

        // a disabled user can no longer log in
        mvc.perform(json(patch("/api/v1/admin/users/" + userId), """
                        {"enabled": false}""").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.role").value("USER"));
        mvc.perform(json(post("/api/v1/auth/login"), """
                        {"email": "%s", "password": "%s"}""".formatted(email, PASSWORD)))
                .andExpect(status().isUnauthorized());

        // re-enable and promote: the next login carries the new role
        mvc.perform(json(patch("/api/v1/admin/users/" + userId), """
                        {"enabled": true, "role": "ADMIN"}""").header("Authorization", admin))
                .andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + login(email, PASSWORD)))
                .andExpect(status().isOk());
    }

    /** CORS **/

    @Test
    void theFrontendOriginIsAllowedAndOthersAreNot() throws Exception {
        mvc.perform(options("/api/v1/books")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/v1/books")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    /** HELPERS **/

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.dev";
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String registerAndLogin(String email) throws Exception {
        mvc.perform(json(post("/api/v1/auth/register"), """
                        {"email": "%s", "password": "%s", "displayName": "Test User"}""".formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
        return login(email, PASSWORD);
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(json(post("/api/v1/auth/login"), """
                        {"email": "%s", "password": "%s"}""".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
