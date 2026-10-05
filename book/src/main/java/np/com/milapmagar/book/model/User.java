package np.com.milapmagar.book.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name= "id", nullable = false)
    private Long id;

    @Column(name = "fullName", nullable = false)
    private String fullName;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "phone", unique = true, nullable = false)
    private String phone;

    /** CONSTRUCTORS **/
    public User(){
    }

    public User(String fullName, String email, String password, String phone){
        this.fullName = fullName;
        this.email = email;
        this.password = password ;
        this.phone = phone;
    }

    /** GETTERS & SETTERS **/
    // id
    public Long getId(){
        return id;
    }
    public void setId(Long id){ this.id = id;}

    // fullName
    public String getFullName(){
        return fullName;
    }
    public void setFullName(String fullName){this.fullName = fullName;}

    // Email
    public String getEmail(){
        return email;
    }
    public void setEmail(String email){this.email = email;}
    // Password
    public String getPassword(){
        return password;
    }
    public void setPassword(String password){this.password = password;}
    // Phone
    public String getPhone(){
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString(){
        return "User {" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
