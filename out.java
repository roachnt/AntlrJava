import java.util.ArrayList;

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
    int intList_version = -1;
    int a_version = 0;
    int b_version = 0;
    int c = a;
    c_version = 0;
    record("", "Sum", "sum", 5, 1, "c", c, c_version);
    a = a * 2;
    a_version = 1;
    record("", "Sum", "sum", 6, 1, "a", a, a_version);
    b = b - 1;
    b_version = 1;
    record("", "Sum", "sum", 7, 1, "b", b, b_version);

    a = --b;
    a_version = 2;
    record("", "Sum", "sum", 9, 1, "a", a, a_version);
    b_version = 2;
    record("", "Sum", "sum", 9, 1, "b", b, b_version);
    b = 2;
    b_version = 3;
    record("", "Sum", "sum", 10, 1, "b", b, b_version);
    a++;
    a_version = 3;
    record("", "Sum", "sum", 11, 1, "a", a, a_version);

    if (a < 2) {
      if (b < 5) {
        a = 3;
        a_version = 4;
        record("", "Sum", "sum", 15, 1, "a", a, a_version);
      } else {
        b = 5;
        b_version = 4;
        record("", "Sum", "sum", 17, 1, "b", b, b_version);
      }
      a_version = 5;
      record("", "Sum", "sum", 17, 1, "a", a, a_version);
      b_version = 5;
      record("", "Sum", "sum", 17, 1, "b", b, b_version);
    } else {
      b = 6;
      b_version = 6;
      record("", "Sum", "sum", 19, 1, "b", b, b_version);
    }
    a_version = 6;
    record("", "Sum", "sum", 19, 1, "a", a, a_version);
    b_version = 7;
    record("", "Sum", "sum", 19, 1, "b", b, b_version);

    if (a > 1) {
      {
        b = 9;
        b_version = 8;
        record("", "Sum", "sum", 22, 2, "b", b, b_version);
        c = a;
        c_version = 1;
        record("", "Sum", "sum", 23, 2, "c", c, c_version);
      }
    }
    b_version = 9;
    record("", "Sum", "sum", 24, 1, "b", b, b_version);
    c_version = 2;
    record("", "Sum", "sum", 24, 1, "c", c, c_version);

    while (true) {
      a_version = 8;
      record("", "Sum", "sum", 26, 1, "a", a, a_version);
      if (!(a < 100)) {
        break;
      }
      b = 0;
      b_version = 10;
      record("", "Sum", "sum", 27, 2, "b", b, b_version);
      a = a + 4;
      a_version = 7;
      record("", "Sum", "sum", 28, 2, "a", a, a_version);
    }
    a_version = 9;
    record("", "Sum", "sum", 29, 1, "a", a, a_version);
    b_version = 11;
    record("", "Sum", "sum", 29, 1, "b", b, b_version);

    ArrayList<Integer> intList = new ArrayList<>();
    intList_version = 0;
    record("", "Sum", "sum", 31, 1, "intList", intList, intList_version);
    intList.add(1);
    intList.add(1);
    intList.add(1);
    intList.add(1);
    intList.add(1);

    if (a < 3) {
      {
        java.util.Iterator<Integer> intList_iterator = intList.iterator();
        int item_version = -1;
        while (intList_iterator.hasNext()) {
          Integer item = intList_iterator.next();
          item_version = 0;
          record("", "Sum", "sum", 39, 1, "item", item, item_version);
          System.out.println(item);
          a = 4;
          a_version = 10;
          record("", "Sum", "sum", 41, 2, "a", a, a_version);
        }
      }
      a_version = 11;
      record("", "Sum", "sum", 42, 1, "a", a, a_version);
    }
    a_version = 12;
    record("", "Sum", "sum", 42, 1, "a", a, a_version);

    {
      int i_version = -1;
      int i = 0;
      i_version = 0;
      record("", "Sum", "sum", 44, 1, "i", i, i_version);
      while (true) {
        i_version = 3;
        record("", "Sum", "sum", 44, 1, "i", i, i_version);
        if (!(i < 10)) {
          break;
        }
        System.out.println("Hello");
        if (i == 5) {
          {
            {
              i++;
              i_version = 2;
              record("", "Sum", "sum", 44, 1, "i", i, i_version);
              continue;
            }
          }
        }
        i++;
        i_version = 1;
        record("", "Sum", "sum", 44, 1, "i", i, i_version);
      }
    }
    b = b + 8;
    b_version = 12;
    record("", "Sum", "sum", 50, 1, "b", b, b_version);
    return a + b;
  }

  public double multiply(int q, int p) {
    int g_version = -1;
    int p_version = 0;
    double g = 8.5;
    g_version = 0;
    record("", "Sum", "multiply", 55, 1, "g", g, g_version);

    if (p < 4) {
      g = 9;
      g_version = 1;
      record("", "Sum", "multiply", 58, 1, "g", g, g_version);
    }
    g_version = 2;
    record("", "Sum", "multiply", 58, 1, "g", g, g_version);

    return g;
  }
}
