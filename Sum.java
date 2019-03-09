public class Sum {

  public static void record(String packageName, String clazz, String method, int line, int staticScope,
      String variableName, Object value, int version) {
    System.out.println(String.format(
        "package: %s, class: %s, method: %s, line: %d, static-scope: %d, variable: %s, value: %s, version: %d",
        packageName, clazz, method, line, staticScope, variableName, value.toString(), version));
  }

  public static void main(String[] args) {
    sum(1, 2);
  }

  public static int sum(int a, int b) {
    int a_version = 0;
    int b_version = 0;
    int c_version = -1;
    int c = a;
    c_version = 0;
    record("", "Sum", "sum", 3, 2, "c", c, c_version);
    a = a * 2;
    a_version = 1;
    record("", "Sum", "sum", 4, 2, "a", a, a_version);
    b = b - 1;
    b_version = 1;
    record("", "Sum", "sum", 5, 2, "b", b, b_version);

    a = 9;
    a_version = 2;
    record("", "Sum", "sum", 7, 2, "a", a, a_version);
    b = 2;
    b_version = 2;
    record("", "Sum", "sum", 8, 2, "b", b, b_version);
    a++;
    a_version = 3;
    record("", "Sum", "sum", 9, 2, "a", a, a_version);
    if (b < 5) {
      a = 4;
      a_version = 4;
      record("", "Sum", "sum", 11, 3, "a", a, a_version);
      a = a * 2;
      a_version = 5;
      record("", "Sum", "sum", 12, 3, "a", a, a_version);
    }
    // a_6 = phi(a_3, a_5);
    a_version = 6;
    record("", "Sum", "sum", 13, 2, "a", a, a_version);

    if (a > 1) {
      b = 9;
      b_version = 3;
      record("", "Sum", "sum", 16, 3, "b", b, b_version);
      c = a;
      c_version = 1;
      record("", "Sum", "sum", 17, 3, "c", c, c_version);
    }
    // b_4 = phi(b_2, b_3);
    b_version = 4;
    record("", "Sum", "sum", 18, 2, "b", b, b_version);
    // c_2 = phi(c_0, c_1);
    c_version = 2;
    record("", "Sum", "sum", 18, 2, "c", c, c_version);

    // while (a < 100) {
    while (true) {
      // a_8 = phi(a_6, a_7);
      a_version = 8;
      record("", "Sum", "sum", 20, 2, "a", a, a_version);
      if (!(a < 100)) {
        break;
      }
      b = 0;
      b_version = 5;
      record("", "Sum", "sum", 21, 3, "b", b, b_version);
      a = a + 4;
      a_version = 7;
      record("", "Sum", "sum", 22, 3, "a", a, a_version);
    }
    // a_9 = phi(a_6, a_8);
    a_version = 9;
    record("", "Sum", "sum", 23, 2, "a", a, a_version);
    // b_6 = phi(b_4, b_5);
    b_version = 6;
    record("", "Sum", "sum", 23, 2, "b", b, b_version);

    // for (int i = 0; i < 10; i++) {
    {
      int i_version = -1;
      int i = 0;
      i_version = 0;
      record("", "Sum", "sum", 25, 3, "i", i, i_version);
      while (true) {
        // i_2 = phi(i_0, i_1);
        i_version = 2;
        record("", "Sum", "sum", 25, 3, "i", i, i_version);
        if (!(i < 10)) {
          break;
        }
        System.out.println("Hello");
        i++;
        i_version = 1;
        record("", "Sum", "sum", 25, 3, "i", i, i_version);
      }
    }
    b = b + 8;
    b_version = 7;
    record("", "Sum", "sum", 28, 2, "b", b, b_version);
    return a + b;
  }
}
