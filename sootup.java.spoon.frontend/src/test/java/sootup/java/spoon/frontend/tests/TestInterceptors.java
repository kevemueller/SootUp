package sootup.java.spoon.frontend.tests;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;

import sootup.core.graph.ControlFlowGraph;
import sootup.core.graph.MutableControlFlowGraph;
import sootup.core.jimple.Jimple;
import sootup.core.jimple.basic.SimpleStmtPositionInfo;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.stmt.JGotoStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.ReturnStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.Body.BodyBuilder;
import sootup.core.model.LinePosition;
import sootup.core.signatures.MethodSignature;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.spoon.frontend.impl.CommentingPrinter;
import sootup.java.spoon.frontend.interceptors.GotoRemover;
import sootup.java.spoon.frontend.interceptors.NopRemover;
import sootup.java.spoon.frontend.util.DebugScanner;

@Timeout(value = 3)
public class TestInterceptors {
  private JavaIdentifierFactory identifierFactory = new JavaIdentifierFactory();
  private Random rnd = Random.from(RandomGenerator.getDefault());

  private int line;
  private int i;

  StmtPositionInfo nextPos() {
    return new SimpleStmtPositionInfo(new LinePosition(++line));
  }

  private Stmt stmt() {
    return rnd.nextBoolean() ? Jimple.newNopStmt(nextPos())
        : Jimple.newAssignStmt(Jimple.newLocal("a", identifierFactory.getIntPrimitiveType()),
            IntConstant.getInstance(i++), nextPos());
  }

  private JIfStmt ifStmt() {
    return Jimple.newIfStmt(
        Jimple.newEqExpr(IntConstant.getInstance(i += 10), IntConstant.getInstance(i += 10)),
        nextPos());
  }

  private JGotoStmt gotoStmt() {
    return Jimple.newGotoStmt(nextPos());
  }

  private ReturnStmt returnStmt() {
    return Jimple.newReturnVoidStmt(nextPos());
  }

  public String dump(ControlFlowGraph<?> cfg) {
    return new DebugScanner().scan(cfg).toString();
  }

  @RepeatedTest(50)
  void test1() {
    BodyBuilder bodyBuilder = Body.builder();
    MethodSignature methodSignature = identifierFactory.getMethodSignature("soot.test", "hello",
        "void", List.of());
    bodyBuilder.setMethodSignature(methodSignature);
    @NonNull
    MutableControlFlowGraph controlFlowGraph = bodyBuilder.getControlFlowGraph();

    final JGotoStmt gt1, gt2, gt3, gt4;
    // Stmt mark = returnStmt();
    final List<Stmt> stmt1 = List.of(stmt(), stmt(), gt1 = gotoStmt());
    final List<Stmt> stmt2 = List.of(stmt(), stmt(), gt2 = gotoStmt());
    final List<Stmt> stmt3 = List.of(stmt(), stmt(), gt3 = gotoStmt());
    final List<Stmt> stmt4 = List.of(gt4 = gotoStmt());
    controlFlowGraph.addBlock(stmt1);
    controlFlowGraph.addBlock(stmt2);
    controlFlowGraph.addBlock(stmt3);
    controlFlowGraph.addBlock(stmt4);
    controlFlowGraph.setStartingStmt(stmt1.get(0));
    controlFlowGraph.putEdge(gt1, JGotoStmt.BRANCH_IDX, stmt2.get(0));
    controlFlowGraph.putEdge(gt2, JGotoStmt.BRANCH_IDX, stmt3.get(1));
    controlFlowGraph.putEdge(gt3, JGotoStmt.BRANCH_IDX, stmt4.get(0));
    controlFlowGraph.putEdge(gt4, JGotoStmt.BRANCH_IDX, stmt3.get(0));

//    System.out.println("*****");
//    System.out.println(dump(bodyBuilder.getControlFlowGraph()));

    new NopRemover().interceptBody(bodyBuilder, null);
    System.out.println("***** postNop");
    System.out.println(CommentingPrinter.toString(bodyBuilder.getControlFlowGraph()));
    System.out.println(dump(bodyBuilder.getControlFlowGraph()));

    new GotoRemover().interceptBody(bodyBuilder, null);

    System.out.println("*****");
    System.out.println(dump(bodyBuilder.getControlFlowGraph()));

    Body body = bodyBuilder.build();
    System.out.println("*****");
    System.out.println(dump(bodyBuilder.getControlFlowGraph()));
  }

  @RepeatedTest(100)
  @Timeout(value = 3, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
  void testIf() {
    BodyBuilder bodyBuilder = Body.builder();
    MethodSignature methodSignature = identifierFactory.getMethodSignature("soot.test", "hello",
        "void", List.of());
    bodyBuilder.setMethodSignature(methodSignature);
    @NonNull
    MutableControlFlowGraph controlFlowGraph = bodyBuilder.getControlFlowGraph();

    final JIfStmt gt1, gt4;
    final JGotoStmt gt2, gt3;
    // Stmt mark = returnStmt();
    final List<Stmt> stmt1 = List.of(stmt(), stmt(), gt1 = ifStmt());
    final List<Stmt> stmt2 = List.of(stmt(), stmt(), gt2 = gotoStmt());
    final List<Stmt> stmt3 = List.of(stmt(), stmt(), gt3 = gotoStmt());
    final List<Stmt> stmt4 = List.of(gt4 = ifStmt());
    controlFlowGraph.addBlock(stmt1);
    controlFlowGraph.addBlock(stmt2);
    controlFlowGraph.addBlock(stmt3);
    controlFlowGraph.addBlock(stmt4);
    controlFlowGraph.setStartingStmt(stmt1.get(0));
    controlFlowGraph.putEdge(gt1, JIfStmt.TRUE_BRANCH_IDX, stmt2.get(0));
    controlFlowGraph.putEdge(gt1, JIfStmt.FALSE_BRANCH_IDX, stmt3.get(0));
    controlFlowGraph.putEdge(gt2, JGotoStmt.BRANCH_IDX, stmt3.get(1));
    controlFlowGraph.putEdge(gt3, JGotoStmt.BRANCH_IDX, stmt4.get(0));
    controlFlowGraph.putEdge(gt4, JIfStmt.TRUE_BRANCH_IDX, stmt2.get(0));
    controlFlowGraph.putEdge(gt4, JIfStmt.FALSE_BRANCH_IDX, stmt1.get(0));

//    System.out.println("*****");
//    System.out.println(dump(bodyBuilder.getControlFlowGraph()));

    new NopRemover().interceptBody(bodyBuilder, null);
    System.out.println("***** postNop");
    System.out.println(CommentingPrinter.toString(bodyBuilder.getControlFlowGraph()));
    System.out.println(dump(bodyBuilder.getControlFlowGraph()));

    new GotoRemover().interceptBody(bodyBuilder, null);
//
//    System.out.println("*****");
//    System.out.println(dump(bodyBuilder.getControlFlowGraph()));

    Body body = bodyBuilder.build();
    System.out.println("***** postGotoRemove");
    System.out.println(dump(bodyBuilder.getControlFlowGraph()));
    System.out.println(CommentingPrinter.toString(body));

  }

}
