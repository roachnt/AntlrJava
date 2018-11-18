public class PhiWhile {
  boolean predicate;

  public PhiWhile(boolean predicate) {
    this.predicate = predicate;
  }

  public boolean evaluatePredicate() {
    return predicate;
  }

  public void updatePredicate(boolean newPredicate) {
    predicate = newPredicate;
  }
}