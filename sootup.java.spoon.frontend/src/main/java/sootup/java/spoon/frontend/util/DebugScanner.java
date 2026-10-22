package sootup.java.spoon.frontend.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.stmt.Stmt;

public final class DebugScanner extends JimpleScanner<Appendable> {
  final String indent = " ".repeat(42);
  int indentLevel = 0;

  final Appendable out;

  public DebugScanner() {
    this(new StringBuilder());
  }

  public DebugScanner(final Appendable out) {
    this.out = out;
  }

  public static String identify(Object element) {
    String posString = "";
    if (element instanceof Stmt stmt) {
      StmtPositionInfo pos = stmt.getPositionInfo();
      if (!pos.equals(StmtPositionInfo.getNoStmtPositionInfo())) {
        posString = "@" + pos;
      }
    }
    return String.format("%s#%xd%s", element.getClass().getSimpleName(),
        System.identityHashCode(element), posString);
  }

  @Override
  protected Appendable scanResult() {
    return out;
  }

  @Override
  protected void enter(final Object element) {
    try {
      String elementString;
      try {
        elementString = "YYY"; //element.toString();
      } catch (RuntimeException ex) {
        elementString = "XXX";
      }
      out.append("%s->%s//%s\n".formatted(indent.substring(0, indentLevel), identify(element),
          elementString));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      indentLevel++;
    }
  }

  @Override
  protected void exit(final Object element) {
    indentLevel--;
    try {
      out.append("%s<-%s\n".formatted(indent.substring(0, indentLevel), identify(element)));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void caseEdge(Stmt stmt, List<Stmt> successors) {
    enter(stmt);

    successors.forEach(s -> {
      try {
        out.append("%s **>%s\n".formatted(indent.substring(0, indentLevel), identify(s)));
      } catch (final IOException e) {
        throw new UncheckedIOException(e);
      }
    });

    exit(stmt);
  }

}