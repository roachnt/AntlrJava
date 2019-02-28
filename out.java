public class Sum {
  public int sum(int a, int b) {
    int c_version = -1;
    int i_version = -1;
    int a_version = 0;
    int b_version = 0;
    int c = a;
    c_version = 0;
    record("", "Sum", "sum", 3, 1, "c", c, c_version);
    a = a * 2;
    a_version = 1;
    record("", "Sum", "sum", 4, 1, "a", a, a_version);
    b = b - 1;
    b_version = 1;
    record("", "Sum", "sum", 5, 1, "b", b, b_version);

    a = 9;
    a_version = 2;
    record("", "Sum", "sum", 7, 1, "a", a, a_version);
    b = 2;
    b_version = 2;
    record("", "Sum", "sum", 8, 1, "b", b, b_version);
    a++;
    a_version = 3;
    record("", "Sum", "sum", 9, 1, "a", a, a_version);
    if (b < 5) {
      a = 4;
      a_version = 4;
      record("", "Sum", "sum", 11, 2, "a", a, a_version);
      a = a * 2;
      a_version = 5;
      record("", "Sum", "sum", 12, 2, "a", a, a_version);
    }
    a_version = 6;
    record("", "Sum", "sum", 13, 1, "a", a, a_version);

    if (a > 1) {
      b = 9;
      b_version = 3;
      record("", "Sum", "sum", 16, 2, "b", b, b_version);
      c = a;
      c_version = 1;
      record("", "Sum", "sum", 17, 2, "c", c, c_version);
    }
    b_version = 4;
    record("", "Sum", "sum", 18, 1, "b", b, b_version);
    c_version = 2;
    record("", "Sum", "sum", 18, 1, "c", c, c_version);

    while (true) {
      a_version = 8;
      record("", "Sum", "sum", 20, 1, "a", a, a_version);
      if (!(a < 100)) {
        break;
      }
      b = 0 + c++ + ++c + c-- + --c;
      b_version = 5;
      record("", "Sum", "sum", 21, 2, "b", b, b_version);
      c_version = 3;
      record("", "Sum", "sum", 21, 2, "c", c, c_version);
      a = a + 4;
      a_version = 7;
      record("", "Sum", "sum", 22, 2, "a", a, a_version);
    }
    a_version = 9;
    record("", "Sum", "sum", 23, 1, "a", a, a_version);
    b_version = 6;
    record("", "Sum", "sum", 23, 1, "b", b, b_version);
    int i;
    if (a < 5) {
      i = a--;
      i_version = 1;
      record("", "Sum", "sum", 26, 1, "i", i, i_version);
      a_version = 12;
      record("", "Sum", "sum", 26, 1, "a", a, a_version);
      while (true) {
        i_version = 3;
        record("", "Sum", "sum", 26, 1, "i", i, i_version);
        if (!(i < 10)) {
          break;
        }
        System.out.println("Hello");
      }
      i = a++;
      i_version = 2;
      record("", "Sum", "sum", 26, 1, "i", i, i_version);
      a_version = 13;
      record("", "Sum", "sum", 26, 1, "a", a, a_version);
    } else {
      System.out.println("Goodbye");
    }
    i_version = 4;
    record("", "Sum", "sum", 31, 1, "i", i, i_version);

    b = b + 8;
    b_version = 7;
    record("", "Sum", "sum", 33, 1, "b", b, b_version);
    return a + b;
  }
}
