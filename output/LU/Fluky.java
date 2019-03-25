public class Fluky {

  public static void main(String[] args) {
  }

  public static double flukyDouble(double goodValue, double badValue, boolean gen_bad, double p) {
    if (!gen_bad)
      return goodValue;
    double r = Math.random();
    if (r <= p)
      return goodValue;
    else
      return badValue;
  }

  public static int flukyInt(int goodValue, int badValue, boolean gen_bad, int p) {
    if (!gen_bad)
      return goodValue;
    double r = Math.random();
    if (r <= p)
      return goodValue;
    else
      return badValue;
  }

  public static double fuzzyDouble(double good_expression, boolean gen_bad) {
    if (gen_bad) {
      return good_expression * 2 * Math.random();
    } else {
      return good_expression;
    }
  }
}
