public class Sum {
  public static void record(
      String packageName,
      String clazz,
      String method,
      int line,
      int staticScope,
      String variableName,
      Object value,
      int version) {
    System.out.println(
        String.format(
            "package: %s, class: %s, method: %s, line: %d, static-scope: %d, variable: %s, value: %s, version: %d",
            packageName,
            clazz,
            method,
            line,
            staticScope,
            variableName,
            value.toString(),
            version));
  }

  public int sum(int a, int b) {
    int c_version = -1;
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

    a = --b;
    a_version = 2;
    record("", "Sum", "sum", 7, 1, "a", a, a_version);
    b_version = 2;
    record("", "Sum", "sum", 7, 1, "b", b, b_version);
    b = 2;
    b_version = 3;
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
      b_version = 4;
      record("", "Sum", "sum", 16, 2, "b", b, b_version);
      c = a;
      c_version = 1;
      record("", "Sum", "sum", 17, 2, "c", c, c_version);
    }
    b_version = 5;
    record("", "Sum", "sum", 18, 1, "b", b, b_version);
    c_version = 2;
    record("", "Sum", "sum", 18, 1, "c", c, c_version);

    while (true) {
      a_version = 8;
      record("", "Sum", "sum", 20, 1, "a", a, a_version);
      if (!(a < 100)) {
        break;
      }
      b = 0;
      b_version = 6;
      record("", "Sum", "sum", 21, 2, "b", b, b_version);
      a = a + 4;
      a_version = 7;
      record("", "Sum", "sum", 22, 2, "a", a, a_version);
    }
    a_version = 9;
    record("", "Sum", "sum", 23, 1, "a", a, a_version);
    b_version = 7;
    record("", "Sum", "sum", 23, 1, "b", b, b_version);

    {
      int i_version = -1;
      int i = 0;
      i_version = 0;
      record("", "Sum", "sum", 25, 1, "i", i, i_version);
      while (true) {
        ++i;
        i_version = 1;
        record("", "Sum", "sum", 25, 1, "i", i, i_version);
        i_version = 2;
        record("", "Sum", "sum", 25, 1, "i", i, i_version);
        if (!(i < 10)) {
          break;
        }
        System.out.println("Hello");
      }
    }
    b = b + 8;
    b_version = 8;
    record("", "Sum", "sum", 28, 1, "b", b, b_version);
    return a + b;
  }
}
