package sootup.java.sample.ll;

import java.time.Instant;

public class TopClass {
  private static String aString;
  private static String bString = "inline";
  private String aDynamic;

  private static class InnerStatic {
    String x = aString;
  }

  private class InnerDynamic {
    public class Inner2Dynamic {
      String x = aDynamic;
      static {
        Instant.now();
      }
      static {
        System.gc();
      }
    }
  }

  private interface Nested$Int {
    enum Hello {
      WORLD,
      Aꙟ𐕕;
    }

    final int x = 2;

    class Inside implements Nested$Int {
      float y = 2;
      String x = aString;
    }
  }

  static {
    aString = "hello";
  }

  {
    System.out.println("dynamic");
  }

  public String getStatic() {
    return aString;
  }
}
