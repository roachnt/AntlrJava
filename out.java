package cwru.selab.cf;

public class PhiIf<T> {
  boolean predicate;

  public PhiIf(boolean predicate) {
    this.predicate_0.predicate = predicate_0;
  }

  public boolean getPredVal() {
    Boolean predicate_0 = null;
    null this.predicate_0 = null;

    predicate_0 = predicate;
    return predicate_0;
  }

  public T merge(T truePredicateValue, T originalValue) {
    Boolean predicate_0 = null;
    null truePredicateValue_0 = null;
    null originalValue_0 = null;
    null this.predicate_0 = null;

    predicate_0 = predicate;
    predicate_0 = predicate;
    truePredicateValue_0 = truePredicateValue;
    originalValue_0 = originalValue;
    return predicate_0 ? truePredicateValue_0 : originalValue_0;
  }
}
