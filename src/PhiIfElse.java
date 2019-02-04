public class PhiIfElse<T> {
  boolean predicate;

  public PhiIfElse(boolean predicate) {
    this.predicate = predicate;
  }

  public boolean getPredVal() {
    return predicate;
  }

  public T merge(T truePredicateValue, T originalValue) {
    return predicate ? truePredicateValue : originalValue;
  }
}