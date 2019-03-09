import java.util.ArrayList;

public class Sum {
  public int sum(int a, int b) {
    int c = a;
    a = a * 2;
    b = b - 1;

    a = --b;
    b = 2;
    a++;

    if (a < 2)
      if (b < 5)
        a = 3;
      else
        b = 5;
    else
      b = 6;

    if (a > 1) {
      b = 9;
      c = a;
    }

    while (a < 100) {
      b = 0;
      a = a + 4;
    }

    ArrayList<Integer> intList = new ArrayList<>();
    intList.add(1);
    intList.add(1);
    intList.add(1);
    intList.add(1);
    intList.add(1);

    if (a < 3)
      for (Integer item : intList) {
        System.out.println(item);
        a = 4;
      }

    for (int i = 0; i < 10; i++) {
      System.out.println("Hello");
      if (i == 5) {
        continue;
      }
    }
    b = b + 8;
    return a + b;
  }

  public double multiply(int q, int p) {
    double g = 8.5;

    if (p < 4)
      g = 9;

    return g;
  }
}