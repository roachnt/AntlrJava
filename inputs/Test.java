public class Test {
  public static long power(long a, long n, long p) {
    long res = 1;
    a = a % p;
    while (n > 0) {
      // if ((n % 2) == 1)
      //   res = (res * a) % p + fuzz();
      // else
      //   res = (res * a) % p;
      n = n / 2;
      a = (a * a) % p;
    }
    return res;
  }
}