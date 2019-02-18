public class Sum {
  public int sum(int a, int b) {
    
    
    c_version = -1;
    i_version = -1;

    a_version = 0;
    b_version = 0;

    int c = a;c_version++;record("","Sum","sum",3,1,"c",c,c_version);
    a = a * 2;a_version++;record("","Sum","sum",4,1,"a",a,a_version);
    b = b - 1;b_version++;record("","Sum","sum",5,1,"b",b,b_version);

    a = 9;a_version++;record("","Sum","sum",7,1,"a",a,a_version);
    b = 2;b_version++;record("","Sum","sum",8,1,"b",b,b_version);
    a++;
    if (b < 5) {
      a = 4;a_version++;record("","Sum","sum",11,2,"a",a,a_version);
      a = a * 2;a_version++;record("","Sum","sum",12,2,"a",a,a_version);
    }
    

    if (a > 1) {
      b = 9;b_version++;record("","Sum","sum",16,2,"b",b,b_version);
      c = a;c_version++;record("","Sum","sum",17,2,"c",c,c_version);
    }
        

    while (a < 100) {
      b = 0;b_version++;record("","Sum","sum",21,2,"b",b,b_version);
      a = a + 4;a_version++;record("","Sum","sum",22,2,"a",a,a_version);
    }

    for (int i = 0; i < 10; i++) {
      System.out.println("Hello");
    }
    b = b + 8;b_version++;record("","Sum","sum",28,1,"b",b,b_version);
    return a + b;
  }
}