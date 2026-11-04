# Stream
- Streams is the way of processing sequences of data in a functional style, allowing you to perform operations like filtering, mapping and reducing without modifying the original data.
  - For example:
      ```
    //returns integer(number)
    int sum = widgets.stream()
                     .filter(w -> w.getColor() == RED)
                     .mapToInt(w -> w.getWeight())
                     .sum  
    ```
    
    ```
    old java
      -------------------
      List<Integer> list = new List<Integer>(1,2,3,4,5);
      int sum = ;
      for(int x : list){ 
        if(x > 3){
         sum+=x;
         System.out.println("TOTAL: " + sum);
       }
      }
    ```
    ```
    newer Java ( 8+ ):
    (using Streams):
        --------------------
        List<Integer> list = new List<Integer>(1,2,3,4,5);
        int sum = 0;
        list.stream()
            .filter(n -> n >=3)
            .map(n -> sum+n)
            .forEach(System.out::println); 
    ```