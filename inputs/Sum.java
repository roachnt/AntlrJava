public class Sum {
  public int sum(int a, int b) {
    int c = a;
    a = a * 2;
    b = b - 1;

    a = 9;
    b = 2;
    a++;
    if (b < 5) {
      a = 4;
      a = a * 2;
    }

    if (a > 1) {
      b = 9;
      c = a;
    }

    while (a < 100) {
      b = 0 + c++ + ++c + c-- + --c;
      a = a + 4;
    }
    int i;
    if (a < 5)
      for (i = a--; i < 10; i = a++) {
        System.out.println("Hello");
      }
    else {
      System.out.println("Goodbye");
    }

    b = b + 8;
    return a + b;
  }
}