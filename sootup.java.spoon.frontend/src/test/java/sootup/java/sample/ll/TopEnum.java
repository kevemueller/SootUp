package sootup.java.sample.ll;

public enum TopEnum implements TopInterface {
  ONE, TWO, THREE;

  @Override
  public int hello() {
    int a  = 42;
    return new Integer(a);
  }
}
