package sootup.java.spoon.frontend.tests;

import java.io.PrintWriter;
import java.io.StringWriter;

import sootup.core.jimple.basic.SimpleStmtPositionInfo;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.util.EscapedWriter;
import sootup.core.util.printer.JimplePrinter;
import sootup.core.util.printer.LabeledStmtPrinter;
import sootup.core.util.printer.NormalStmtPrinter;

public class CommentingPrinter {

  public static String toString(Body body) {
    StringWriter writer = new StringWriter();
    LabeledStmtPrinter stmtPrinter = new NormalStmtPrinter() {
      @Override
      public void stmt(Stmt currentStmt) {
        startStmt(currentStmt);
        currentStmt.toString(this);
        endStmt(currentStmt);
        output.append(";");
        StmtPositionInfo positionInfo = currentStmt.getPositionInfo();
        if (!positionInfo.equals(SimpleStmtPositionInfo.getNoStmtPositionInfo())) {
          output.append(" // ").append(positionInfo);
        }
        newline();
      }
    };
    try (PrintWriter writerOut = new PrintWriter(new EscapedWriter(writer))) {
      new JimplePrinter().printTo(body, writerOut, stmtPrinter);
    }
    return writer.toString();
  }
}
