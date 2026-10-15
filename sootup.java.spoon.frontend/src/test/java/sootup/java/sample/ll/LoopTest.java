package sootup.java.sample.ll;

import java.util.List;

class LoopTest {

  public void simpleDo() {
    int i = 0;
    do {
      System.out.println("Hello world!");
    } while (i++ < 10);
    System.out.println("Done!");
  }

  public void simpleDoBreak() {
    int i = 0;
    do {
      if (3 == i % 5) {
        break;
      }
      System.out.println("Hello world!");
      if (2 == i % 3) {
        break;
      }
    } while (i++ < 10);
    System.out.println("Done!");
  }

  public void simpleDoContinue() {
    int i = 0;
    /**
     * Do with continue.
     */
    do {
      if (3 == i % 5) {
        continue;
      }
      System.out.println("Hello world!");
      if (2 == i % 3) {
        continue;
      }
    } while (i++ < 10);
    System.out.println("Done!");
  }

  public void simpleWhile() {
    int i = 0;
    while (i++ < 10) {
      System.out.println("Hello world!");
    }
    System.out.println("Done!");
  }

  public void simpleWhileBreak() {
    int i = 0;
    while (i++ < 10) {
      if (3 == i % 5) {
        break;
      }
      System.out.println("Hello world!");
      if (2 == i % 3) {
        break;
      }
    }
    System.out.println("Done!");
  }

  public void simpleWhileContinue() {
    int i = 0;
    while (i++ < 10) {
      if (3 == i % 5) {
        continue;
      }
      System.out.println("Hello world!");
      if (2 == i % 3) {
        continue;
      }
    }
    System.out.println("Done!");
  }

  public void simpleFor() {
    for (int i = 0; i < 10; i++) {
      System.out.println("Hello world!");
    }
    System.out.println("Done!");
  }

  public void simpleForBreak() {
    for (int i = 0; i < 10; i++) {
      if (3 == i % 5) {
        break;
      }
      System.out.println("Hello world!");
      if (2 == i % 3) {
        break;
      }
    }
    System.out.println("Done!");
  }

  public void simpleForContinue() {
    for (int i = 0; i < 10; i++) {
      if (3 == i % 5) {
        continue;
      }
      System.out.println("Hello world!");
      if (2 == i % 3) {
        continue;
      }
    }
    System.out.println("Done!");
  }

  public void simpleForEachArray() {
    for (char c : "hello".toCharArray()) {
      System.out.println(c);
    }
  }

  public void simpleForEachArrayBreak() {
    for (char c : "hello".toCharArray()) {
      if (c == 'x') {
        break;
      }
      System.out.println(c);
      if (c == 'o') {
        break;
      }
    }
  }

  public void simpleForEachArrayContinue() {
    for (char c : "hello".toCharArray()) {
      if (c == 'x') {
        continue;
      }
      System.out.println(c);
      if (c == 'o') {
        continue;
      }
    }
  }

  public void simpleForEachIterable() {
    for (String s : List.of("hello", "world")) {
      System.out.println(s);
    }
    return;
  }

  public void simpleForEachIterableBreak() {
    for (String s : List.of("hello", "world")) {
      if ("test".equals(s)) {
        break;
      }
      System.out.println(s);
      if ("world".equals(s)) {
        break;
      }
    }
    return;
  }

  public void simpleForEachIterableContinue() {
    for (String s : List.of("hello", "world")) {
      if ("test".equals(s)) {
        continue;
      }
      System.out.println(s);
      if ("world".equals(s)) {
        continue;
      }
    }
    return;
  }

  public void ppLazyAnd(boolean left, boolean right) {
    final boolean lazyAnd;
    logicalAnd: {
      if (left) {
        if (right) {
          lazyAnd = true;
          break logicalAnd;
        }
      }
      lazyAnd = false;
    }
    System.out.println(lazyAnd);
  }

  public void ppLazyOr(boolean left, boolean right) {
    final boolean lazyOr;
    logicalOr: {
      if (!left) {
        if (!right) {
          lazyOr = false;
          break logicalOr;
        }
      }
      lazyOr = true;
    }
    System.out.println(lazyOr);
  }

  public void nestedBreak() {
    for (int i = 0; i < 10; i++) {
      if (3 == i % 5) {
        break;
      }
      int j = 0;
      while (j++ < 10) {
        if (3 == j % 5) {
          break;
        }
        int k = 0;
        do {
          if (3 == k % 5) {
            break;
          }
          System.out.println("Hello world!");
          if (2 == k % 3) {
            continue;
          }
        } while (k++ < 10);

        if (2 == j % 3) {
          continue;
        }
      }
      if (2 == i % 3) {
        continue;
      }
    }
    System.out.println("Done!");

  }

}
