package sootup.java.spoon.frontend.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;

import sootup.core.graph.BasicBlock;
import sootup.core.graph.ControlFlowGraph;
import sootup.core.graph.MutableControlFlowGraph;
import sootup.core.jimple.basic.SimpleStmtPositionInfo;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.util.EscapedWriter;
import sootup.core.util.printer.JimplePrinter;
import sootup.core.util.printer.NormalStmtPrinter;

public class CommentingPrinter {

  private static class LabeledStmtPrinter extends NormalStmtPrinter {

    public LabeledStmtPrinter(ControlFlowGraph<?> graph) {
      this.graph = graph;
    }

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

    @Override
    public void stmtRef(Stmt stmt, boolean branchTarget) {
      if (null == labels) {
        output.append(" Ref");
        StmtPositionInfo positionInfo = stmt.getPositionInfo();
        if (!positionInfo.equals(SimpleStmtPositionInfo.getNoStmtPositionInfo())) {
          output.append(" // ").append(positionInfo);
        }
      } else {
        super.stmtRef(stmt, branchTarget);
      }
    }
  };

  public static String toString(Body body) {
    StringWriter writer = new StringWriter();
    try (PrintWriter writerOut = new PrintWriter(new EscapedWriter(writer))) {
      new JimplePrinter().printTo(body, writerOut,
          new LabeledStmtPrinter(body.getControlFlowGraph()));
    }
    return writer.toString();
  }

  public static String toString(ControlFlowGraph<?> cfg) {
    StringWriter writer = new StringWriter();
    try (PrintWriter writerOut = new PrintWriter(new EscapedWriter(writer))) {
      new JimplePrinter().printTo(cfg, writerOut, new LabeledStmtPrinter(cfg));
    }
    return writer.toString();
  }

  public static String toString(BasicBlock<?> basicBlock, ControlFlowGraph<?> controlFlowGraph) {
    final StringWriter writer = new StringWriter();
    try (PrintWriter writerOut = new PrintWriter(new EscapedWriter(writer))) {
      basicBlock.getStmts().forEach(s -> {
        LabeledStmtPrinter p = new LabeledStmtPrinter(controlFlowGraph);
        s.toString(p);
        writer.append(p.toString()).append("; ");
      });
    }
    return writer.toString();
  }

  public static <T extends BasicBlock<?>> String toString(Collection<T> basicBlocks,
      MutableControlFlowGraph controlFlowGraph) {
    final StringWriter writer = new StringWriter();
    writer.append("[\n");
    basicBlocks.forEach(b -> writer.append(toString(b, controlFlowGraph)).append('\n'));
    writer.append("]");
    return writer.toString();
  }

}
