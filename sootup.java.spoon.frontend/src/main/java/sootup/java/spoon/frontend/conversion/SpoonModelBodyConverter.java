package sootup.java.spoon.frontend.conversion;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sootup.core.graph.BasicBlock;
import sootup.core.graph.MutableControlFlowGraph;
import sootup.core.jimple.Jimple;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.Immediate;
import sootup.core.jimple.common.LValue;
import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.Value;
import sootup.core.jimple.common.constant.BooleanConstant;
import sootup.core.jimple.common.constant.Constant;
import sootup.core.jimple.common.constant.DoubleConstant;
import sootup.core.jimple.common.constant.FloatConstant;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.constant.LogicalConstant;
import sootup.core.jimple.common.constant.LongConstant;
import sootup.core.jimple.common.constant.NullConstant;
import sootup.core.jimple.common.constant.NumericConstant;
import sootup.core.jimple.common.constant.ShiftableConstant;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.AbstractBinopExpr;
import sootup.core.jimple.common.expr.AbstractConditionExpr;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JAddExpr;
import sootup.core.jimple.common.expr.JInstanceOfExpr;
import sootup.core.jimple.common.expr.JInterfaceInvokeExpr;
import sootup.core.jimple.common.expr.JLengthExpr;
import sootup.core.jimple.common.expr.JNeExpr;
import sootup.core.jimple.common.expr.JNewArrayExpr;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.ref.JThisRef;
import sootup.core.jimple.common.stmt.BranchingStmt;
import sootup.core.jimple.common.stmt.FallsThroughStmt;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JGotoStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.JNopStmt;
import sootup.core.jimple.common.stmt.JReturnStmt;
import sootup.core.jimple.common.stmt.JReturnVoidStmt;
import sootup.core.jimple.common.stmt.JThrowStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body.BodyBuilder;
import sootup.core.signatures.FieldSignature;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ArrayType;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;
import sootup.core.types.VoidType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.jimple.basic.JavaLocal;
import sootup.java.core.language.JavaJimple;
import sootup.java.core.types.JavaClassType;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtArrayRead;
import spoon.reflect.code.CtArrayWrite;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtBreak;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtContinue;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtJavaDoc;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtLoop;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtSuperAccess;
import spoon.reflect.code.CtSwitch;
import spoon.reflect.code.CtSwitchExpression;
import spoon.reflect.code.CtSynchronized;
import spoon.reflect.code.CtTextBlock;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtThrow;
import spoon.reflect.code.CtTry;
import spoon.reflect.code.CtTryWithResource;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.reference.CtParameterReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

/**
 * Proof of Concept converter of a SpoonModel Method into a SootUp
 * JavaSootMethod.
 *
 * <p>
 * For now uses a very simple CtScanner based conversion and don't care for the
 * exceptional flow.
 *
 * @author Keve Müller
 */
public class SpoonModelBodyConverter extends CtScanner implements SpoonConverter {
  private static final Logger LOG = LoggerFactory.getLogger(SpoonModelBodyConverter.class);
  private final JavaIdentifierFactory identifierFactory;
  private final BodyBuilder bodyBuilder;
  private final MutableControlFlowGraph controlFlowGraph;

  private final StmtPositionInfo noPos = StmtPositionInfo.getNoStmtPositionInfo();
  private List<Stmt> currentBlock;
  private final Deque<Frame> frames;
  private Frame currentFrame;
  private Value result;

  private int dummyCounter;
  private JavaLocal thisLocal;
  private Map<String, Local> paramLocals;
  private Map<String, Local> executableLocals;
  private final Map<Stmt, CtElement> stmtElementLink;

  private enum BreakKind {
    TOP, BODY, CONTINUE, JOIN;
  }

  private record Break(BreakKind breakKind, BranchingStmt jiBranchingStmt, int idx) {
    public static Break of(final BreakKind breakKind, final JGotoStmt jiGotoStmt) {
      return of(breakKind, jiGotoStmt, JGotoStmt.BRANCH_IDX);
    }

    public static Break of(final BreakKind breakKind, final BranchingStmt jiBranchingStmt,
        final int idx) {
      return new Break(breakKind, jiBranchingStmt, idx);
    }
  }

  private record Frame(CtStatement ctStatement, String label, Stmt jiBeginMark,
      List<Break> breaks) {

    public static Frame of(final CtStatement ctLabeledStatement, final Stmt jiBeginMark) {
      return new Frame(ctLabeledStatement, ctLabeledStatement.getLabel(), jiBeginMark,
          new ArrayList<>());
    }

    public static Frame of(final CtLoop ctLoop, final Stmt jiBeginMark) {
      return new Frame(ctLoop, ctLoop.getLabel(), jiBeginMark, new ArrayList<>());
    }

    public void add(final Break breaker) {
      breaks.add(breaker);
    }

  }

  public SpoonModelBodyConverter(final JavaIdentifierFactory identifierFactory,
      final BodyBuilder bodyBuilder) {
    this.bodyBuilder = bodyBuilder;
    this.identifierFactory = identifierFactory;
    this.controlFlowGraph = bodyBuilder.getControlFlowGraph();
    this.stmtElementLink = new LinkedHashMap<>();
    this.dummyCounter = 0;
    this.frames = new ArrayDeque<>();
  }

  public <T extends CtElement> Value evaluate(final T element) {
    if (element == null) {
      return null;
    }
    scan(element);
    if (result != null) {
      final Value r = result;
      result = null;
      return r;
    }
    LOG.warn("Did not evaluate {}//{}", element.getClass().getSimpleName(), element);
    return null;
  }

  @Override
  public JavaIdentifierFactory identifierFactory() {
    return identifierFactory;
  }

  @Override
  public void visitCtAnonymousExecutable(final CtAnonymousExecutable anonymousExec) {
    // FIXME: implement
  }

  @Override
  public <T> void visitCtArrayRead(final CtArrayRead<T> arrayRead) {
    final Value base = evaluate(arrayRead.getTarget());
    final Value idx = evaluate(arrayRead.getIndexExpression());
    final JArrayRef newArrayRef = Jimple.newArrayRef(makeLocal(base, arrayRead),
        makeImmediate(idx, arrayRead));
    setResult(applyCasts(newArrayRef, arrayRead.getTypeCasts()));
  }

  @Override
  public <T> void visitCtArrayWrite(final CtArrayWrite<T> arrayWrite) {
    final Value value = evaluate(arrayWrite.getTarget());
    final Value idx = evaluate(arrayWrite.getIndexExpression());
    final JArrayRef arrayRef = Jimple.newArrayRef(makeLocal(value, arrayWrite),
        makeImmediate(idx, arrayWrite));
    setResult(applyCasts(arrayRef, arrayWrite.getTypeCasts()));
  }

  @Override
  public <T> void visitCtAssert(final CtAssert<T> ctAssert) {
    final FieldSignature fieldSignature = identifierFactory.getFieldSignature("$assertionsDisabled",
        (ClassType) thisLocal.getType(),
        identifierFactory.getPrimitiveType(FQN_BOOLEAN_TYPE).get());
    final JNeExpr jiTestExpr = Jimple.newNeExpr(
        makeImmediate(Jimple.newStaticFieldRef(fieldSignature), ctAssert),
        IntConstant.getInstance(0));
    final JIfStmt jiTest = Jimple.newIfStmt(jiTestExpr, noPos);
    emit(jiTest, ctAssert);
    final JNopStmt doAssert = mark();
    final CtExpression<Boolean> ctAssertExpression = ctAssert.getAssertExpression();
    final AbstractConditionExpr assertCond = makeConditional(ctAssertExpression);
    final JIfStmt jiAssert = Jimple.newIfStmt(assertCond, noPos);
    emit(jiAssert, ctAssert);
    final JNopStmt jiAssertFail = mark();
    final JavaClassType assertionErrorType = toSootClassType(FQN_ASSERTIONERROR);
    final Local jiError = makeLocal(Jimple.newNewExpr(assertionErrorType), ctAssert);
    final MethodSignature methodSignature = identifierFactory.getMethodSignature(assertionErrorType,
        "<init>", VoidType.getInstance(), List.of());
    final JInvokeStmt inv = Jimple
        .newInvokeStmt(Jimple.newSpecialInvokeExpr(jiError, methodSignature), noPos);
    emit(inv, ctAssert);
    final JThrowStmt throwStmt = Jimple.newThrowStmt(jiError, noPos);
    emit(throwStmt, ctAssert);

    final JNopStmt done = mark();
    controlFlowGraph.putEdge(jiTest, JIfStmt.TRUE_BRANCH_IDX, done);
    controlFlowGraph.putEdge(jiTest, JIfStmt.FALSE_BRANCH_IDX, doAssert);
    controlFlowGraph.putEdge(jiAssert, JIfStmt.TRUE_BRANCH_IDX, done);
    controlFlowGraph.putEdge(jiAssert, JIfStmt.FALSE_BRANCH_IDX, jiAssertFail);
  }

  @Override
  public <T, A extends T> void visitCtAssignment(final CtAssignment<T, A> ctAssignment) {
    final Value op1 = evaluate(ctAssignment.getAssigned());
    final Value op2 = evaluate(ctAssignment.getAssignment());
    final JAssignStmt newAssignStmt = Jimple.newAssignStmt(makeLValue(op1), op2, noPos);
    emit(newAssignStmt, ctAssignment);
    setResult(op1);
  }

  @Override
  public <T> void visitCtBinaryOperator(final CtBinaryOperator<T> ctOperator) {
    final Value res = switch (ctOperator.getKind()) {
    case OR -> opLogicalOr(ctOperator);
    case AND -> opLogicalAnd(ctOperator);
    case LT -> opBinOp(ctOperator, JavaJimple::newLtExpr);
    case LE -> opBinOp(ctOperator, JavaJimple::newLeExpr);
    case EQ -> opBinOp(ctOperator, JavaJimple::newEqExpr);
    case GE -> opBinOp(ctOperator, JavaJimple::newGeExpr);
    case GT -> opBinOp(ctOperator, JavaJimple::newGtExpr);
    case NE -> opBinOp(ctOperator, JavaJimple::newNeExpr);
    case MOD -> opBinOp(ctOperator, JavaJimple::newRemExpr);
    case PLUS -> opBinOp(ctOperator, JavaJimple::newAddExpr);
    case DIV -> opBinOp(ctOperator, JavaJimple::newDivExpr);
    case MINUS -> opBinOp(ctOperator, JavaJimple::newSubExpr);
    case MUL -> opBinOp(ctOperator, JavaJimple::newMulExpr);
    case BITAND -> opBinOp(ctOperator, JavaJimple::newAndExpr);
    case BITOR -> opBinOp(ctOperator, JavaJimple::newOrExpr);
    case BITXOR -> opBinOp(ctOperator, JavaJimple::newXorExpr);
    case SL -> opBinOp(ctOperator, JavaJimple::newShlExpr);
    case SR -> opBinOp(ctOperator, JavaJimple::newShrExpr);
    case USR -> opBinOp(ctOperator, JavaJimple::newUshrExpr);
    case INSTANCEOF -> opInstanceOf(ctOperator);
    default -> throw implement(ctOperator);
    };

    setResult(applyCasts(res, ctOperator.getTypeCasts()));
  }

  @Override
  public <R> void visitCtBlock(final CtBlock<R> ctBlock) {
    Frame blockFrame = openFrame(ctBlock);
    scan(CtRole.STATEMENT, ctBlock.getStatements());
    closeFrame(blockFrame);
  }

  private void closeFrame(Frame frame) {
    if (null != frame) {
      pop(null, null, mark(frame.ctStatement()));
    }
  }

  @Override
  public void visitCtBreak(final CtBreak ctBreakStatement) {
    final Frame targetFrame;
    if (null == ctBreakStatement.getTargetLabel()) {
      targetFrame = breakTarget();
    } else {
      targetFrame = breakTarget(ctBreakStatement.getTargetLabel());
    }
    final JGotoStmt gotoEnd = jmp(ctBreakStatement);
    targetFrame.add(Break.of(BreakKind.JOIN, gotoEnd));
  }

  private Frame breakTarget() {
    if (currentFrame.ctStatement instanceof CtLoop
        || currentFrame.ctStatement instanceof CtSwitch) {
      return currentFrame;
    }
    for (final Iterator<Frame> it = frames.descendingIterator(); it.hasNext();) {
      final Frame itFrame = it.next();
      if (itFrame.ctStatement instanceof CtLoop || itFrame.ctStatement instanceof CtSwitch) {
        return itFrame;
      }
    }
    throw new NoSuchElementException("Cannot determin target for break");
  }

  private Frame breakTarget(String label) {
    if (label.equals(currentFrame.label())) {
      return currentFrame;
    }
    for (final Iterator<Frame> it = frames.descendingIterator(); it.hasNext();) {
      final Frame itFrame = it.next();
      if (label.equals(itFrame.label())) {
        return itFrame;
      }
    }
    throw new NoSuchElementException("Cannot determine target for break " + label);
  }

  @Override
  public <T> void visitCtClass(final CtClass<T> ctClass) {
    // FIXME: implement
  }

  @Override
  public void visitCtComment(final CtComment ctComment) {
    // intentionally left blank
  }

  @Override
  public <T> void visitCtConditional(final CtConditional<T> ctConditional) {
    final CtExpression<Boolean> ctCondition = ctConditional.getCondition();
    final AbstractConditionExpr condExpr = negate(makeConditional(ctCondition));
    final JIfStmt ifCond = Jimple.newIfStmt(condExpr, noPos);
    emit(ifCond, ctConditional);
    final JNopStmt thenMarker = mark();
    final CtExpression<T> ctThen = ctConditional.getThenExpression();
    final Value thenExpr = evaluate(ctThen);
    final JAssignStmt thenAssignment = makeDummyAssign(thenExpr, ctThen);
    final JGotoStmt thenToJoin = jmp(ctThen);
    controlFlowGraph.putEdge(ifCond, JIfStmt.FALSE_BRANCH_IDX, thenMarker);

    final JNopStmt elseMarker = mark();
    final CtExpression<T> ctElse = ctConditional.getElseExpression();
    final Value elseExpr = evaluate(ctElse);
    final JAssignStmt elseAssignment = Jimple.newAssignStmt(thenAssignment.getLeftOp(), elseExpr,
        noPos);
    emit(elseAssignment, ctConditional);
    final JGotoStmt elseToJoin = jmp(ctElse);

    controlFlowGraph.putEdge(ifCond, JIfStmt.TRUE_BRANCH_IDX, elseMarker);

    final JNopStmt join = mark();

    controlFlowGraph.putEdge(thenToJoin, JGotoStmt.BRANCH_IDX, join);
    controlFlowGraph.putEdge(elseToJoin, JGotoStmt.BRANCH_IDX, join);
    setResult((Local) thenAssignment.getLeftOp());
  }

  @Override
  public <T> void visitCtConstructor(final CtConstructor<T> ctConstructor) {
    final JavaClassType classType = toSootClassType(ctConstructor.getDeclaringType());
    currentBlock = new ArrayList<>();
    paramLocals = new LinkedHashMap<>();
    executableLocals = new LinkedHashMap<>();

    if (!ctConstructor.isStatic()) {
      thisLocal = JavaJimple.newLocal("this", classType);
      final JThisRef thisRef = Jimple.newThisRef(classType);
      final JIdentityStmt thisStmt = new JIdentityStmt(thisLocal, thisRef, noPos);
      bodyBuilder.addLocal(thisLocal);
      emit(thisStmt, ctConstructor);
    }

    scan(ctConstructor.getParameters());

    mark();

    scan(ctConstructor.getBody());

    finish(ctConstructor.getFactory().Type().voidPrimitiveType());
  }

  @Override
  public <T> void visitCtConstructorCall(final CtConstructorCall<T> ctConstructorCall) {
    final List<Immediate> jiArgsList = new ArrayList<>();
    for (final CtExpression<?> arg : ctConstructorCall.getArguments()) {
      jiArgsList.add(makeImmediate(evaluate(arg), arg));
    }
    final MethodSignature methodSignature = signature(ctConstructorCall.getExecutable());
    final JNewExpr newExpr = Jimple.newNewExpr(methodSignature.getDeclClassType());
    final Local newLocal = makeLocal(newExpr, ctConstructorCall);
    final JSpecialInvokeExpr newInvocation = Jimple.newSpecialInvokeExpr(newLocal, methodSignature,
        jiArgsList);
    final JInvokeStmt stmt = Jimple.newInvokeStmt(newInvocation, noPos);
    emit(stmt, ctConstructorCall);
    setResult(applyCasts(newLocal, ctConstructorCall.getTypeCasts()));
  }

  @Override
  public void visitCtContinue(final CtContinue ctContinueStatement) {
    if (null == ctContinueStatement.getTargetLabel()) {
      final Frame frame = breakTarget();
      JGotoStmt jiJmp = jmp(ctContinueStatement);
      frame.add(Break.of(BreakKind.CONTINUE, jiJmp));
    } else {
      throw implement(ctContinueStatement);
    }
  }

  private Frame push(final Frame frame) {
    if (null != currentFrame) {
      frames.add(currentFrame);
    }
    return currentFrame = frame;
  }

  private Frame pop(final Stmt markBody, final Stmt markContinue, final Stmt markDone) {
    if (null == currentFrame) {
      throw new NoSuchElementException();
    }

    currentFrame.breaks.forEach(breaker -> {
      final Stmt target = switch (breaker.breakKind) {
      case TOP -> currentFrame.jiBeginMark();
      case BODY -> markBody;
      case CONTINUE -> markContinue;
      case JOIN -> markDone;
      default -> throw new IllegalArgumentException();
      };
//      if (!controlFlowGraph.containsNode(target)) {
//        throw new IllegalArgumentException(
//            "Make sure to emit " + target + "@" + target.getPositionInfo());
//      }
      controlFlowGraph.putEdge(breaker.jiBranchingStmt(), breaker.idx(), target);
    });
    return currentFrame = frames.pollLast();
  }

  private Frame openFrame(CtLoop ctLoop) {
    return push(Frame.of(ctLoop, mark(ctLoop)));
  }

  private Frame openFrame(CtStatement ctLabeledStatement) {
    if (null != ctLabeledStatement.getLabel()) {
      return push(Frame.of(ctLabeledStatement, mark(ctLabeledStatement)));
    }
    return null;
  }

  @Override
  public void visitCtDo(final CtDo ctDoLoop) {
    JGotoStmt jmpBody = jmp(ctDoLoop);

    openFrame(ctDoLoop).add(Break.of(BreakKind.BODY, jmpBody));
    final CtExpression<Boolean> ctLoopingExpression = ctDoLoop.getLoopingExpression();
    final JIfStmt whileIf = Jimple.newIfStmt(makeConditional(ctLoopingExpression),
        toPositionInfo(ctLoopingExpression));
    emit(whileIf, ctDoLoop);
    currentFrame.add(Break.of(BreakKind.BODY, whileIf, JIfStmt.TRUE_BRANCH_IDX));
    currentFrame.add(Break.of(BreakKind.JOIN, whileIf, JIfStmt.FALSE_BRANCH_IDX));

    JNopStmt markBody = mark();
    scan(ctDoLoop.getBody());
    jmp(currentFrame.jiBeginMark(), ctDoLoop);

    pop(markBody, currentFrame.jiBeginMark(), mark());
  }

  @Override
  public <T> void visitCtFieldRead(final CtFieldRead<T> ctFieldRead) {
    handleCtFieldAccess(ctFieldRead);
  }

  @Override
  public <T> void visitCtFieldWrite(final CtFieldWrite<T> ctFieldWrite) {
    handleCtFieldAccess(ctFieldWrite);
  }

  @Override
  public void visitCtFor(final CtFor ctForLoop) {
    scan(ctForLoop.getForInit());

    openFrame(ctForLoop);
    final CtExpression<Boolean> ctForLoopExpression = ctForLoop.getExpression();
    final AbstractConditionExpr jiCond = makeConditional(ctForLoopExpression);
    final JIfStmt jiIf = Jimple.newIfStmt(jiCond, toPositionInfo(ctForLoopExpression));
    emit(jiIf, ctForLoop);
    currentFrame.add(Break.of(BreakKind.BODY, jiIf, JIfStmt.TRUE_BRANCH_IDX));
    currentFrame.add(Break.of(BreakKind.JOIN, jiIf, JIfStmt.FALSE_BRANCH_IDX));

    final JNopStmt forBody = mark();
    scan(ctForLoop.getBody());

    final JNopStmt forUpdate = mark();
    scan(ctForLoop.getForUpdate());

    jmp(currentFrame.jiBeginMark(), ctForLoop);

    pop(forBody, forUpdate, mark());
  }

  @Override
  public void visitCtForEach(final CtForEach ctForEach) {
    CtExpression<?> ctExpression = ctForEach.getExpression();
    CtTypeReference<?> ctIterableType = ctForEach.getFactory().Type().createReference(FQN_ITERABLE);
    final Value jiExpr = evaluate(ctExpression);
    final Local jiForEachExpr = makeLocal(jiExpr, ctForEach);
    if (ctExpression.getType().isArray()) {
      final JLengthExpr jiLenExpr = Jimple.newLengthExpr(jiForEachExpr);
      final Local jiLenVariable = makeLocal(jiLenExpr, ctForEach);
      final JavaLocal idxVariable = makeDummy(IntConstant.getInstance(0), ctForEach);
      final Local localVariable = (Local) evaluate(ctForEach.getVariable());
      openFrame(ctForEach);
      final JIfStmt jiIf = Jimple.newIfStmt(Jimple.newLtExpr(idxVariable, jiLenVariable),
          toPositionInfo(ctForEach));
      emit(jiIf, ctForEach);
      currentFrame.add(Break.of(BreakKind.BODY, jiIf, JIfStmt.TRUE_BRANCH_IDX));
      currentFrame.add(Break.of(BreakKind.JOIN, jiIf, JIfStmt.FALSE_BRANCH_IDX));

      final JNopStmt forBody = mark();
      final JAssignStmt jiAssign = Jimple.newAssignStmt(localVariable,
          Jimple.newArrayRef(jiForEachExpr, idxVariable), noPos);
      emit(jiAssign, ctForEach);

      scan(ctForEach.getBody());

      final JNopStmt forUpdate = mark();
      final JAssignStmt jiIncrement = Jimple.newAssignStmt(idxVariable,
          Jimple.newAddExpr(idxVariable, IntConstant.getInstance(1)), noPos);
      emit(jiIncrement, ctForEach);

      jmp(currentFrame.jiBeginMark(), ctForEach);

      pop(forBody, forUpdate, mark());
    } else if (ctExpression.getType().isSubtypeOf(ctIterableType)) {
      JavaClassType iteratorType = identifierFactory.getClassType(FQN_ITERATOR);
      MethodSignature iteratorMethod = identifierFactory
          .getMethodSignature((ClassType) jiExpr.getType(), "iterator", iteratorType, List.of());
      Local jiIterator = makeLocal(Jimple.newInterfaceInvokeExpr(jiForEachExpr, iteratorMethod),
          ctExpression);
      final Local localVariable = (Local) evaluate(ctForEach.getVariable());
      openFrame(ctForEach);

      PrimitiveType booleanType = identifierFactory.getPrimitiveType("boolean").get();
      MethodSignature hasNextMethod = identifierFactory
          .getMethodSignature((ClassType) jiIterator.getType(), "hasNext", booleanType, List.of());
      AbstractConditionExpr test = Jimple.newNeExpr(
          makeImmediate(Jimple.newInterfaceInvokeExpr(jiIterator, hasNextMethod), ctForEach),
          IntConstant.getInstance(0));
      final JIfStmt jiIf = Jimple.newIfStmt(test, toPositionInfo(ctForEach));
      emit(jiIf, ctForEach);
      currentFrame.add(Break.of(BreakKind.CONTINUE, jiIf, JIfStmt.TRUE_BRANCH_IDX));
      currentFrame.add(Break.of(BreakKind.JOIN, jiIf, JIfStmt.FALSE_BRANCH_IDX));

      final JNopStmt forUpdate = mark();
      JavaClassType jiObjectType = identifierFactory.getClassType(FQN_OBJECT);
      MethodSignature nextMethod = identifierFactory
          .getMethodSignature((ClassType) jiIterator.getType(), "next", jiObjectType, List.of());
      JInterfaceInvokeExpr nextInvocation = Jimple.newInterfaceInvokeExpr(jiIterator, nextMethod);
      final JAssignStmt jiAssign = Jimple.newAssignStmt(localVariable, nextInvocation,
          toPositionInfo(ctForEach.getVariable()));
      emit(jiAssign, ctForEach);

      final JNopStmt forBody = mark();
      scan(ctForEach.getBody());

      jmp(currentFrame.jiBeginMark(), ctForEach);

      pop(forBody, forUpdate, mark());
    } else {
      throw implement(ctForEach.getExpression().getType());
    }
  }

  @Override
  public void visitCtIf(final CtIf ctIfElement) {
    final CtExpression<Boolean> ctCondition = ctIfElement.getCondition();
    final AbstractConditionExpr jiCond = negate(makeConditional(ctCondition));
    final JIfStmt ifStmt = Jimple.newIfStmt(jiCond, noPos);
    emit(ifStmt, ctIfElement);

    final JNopStmt markThen;
    final JGotoStmt thenJoin;
    if (null != ctIfElement.getThenStatement()) {
      markThen = mark(ctIfElement.getThenStatement());
      final CtStatement ctThen = ctIfElement.getThenStatement();
      scan(ctThen);
      // FIXME: no paths exit the block
      thenJoin = currentBlock.isEmpty() ? null : jmp(ctThen);
    } else {
      markThen = null;
      thenJoin = null;
    }

    final JNopStmt markElse;
    final JGotoStmt elseJoin;
    if (null != ctIfElement.getElseStatement()) {
      markElse = mark(ctIfElement.getElseStatement());
      final CtStatement ctElse = ctIfElement.getElseStatement();
      scan(ctElse);
      // FIXME: no paths exit the block
      elseJoin = currentBlock.isEmpty() ? null : jmp(ctElse);
    } else {
      markElse = null;
      elseJoin = null;
    }
    final JNopStmt join = mark(ctIfElement);

    controlFlowGraph.putEdge(ifStmt, JIfStmt.FALSE_BRANCH_IDX, null == markThen ? join : markThen);
    controlFlowGraph.putEdge(ifStmt, JIfStmt.TRUE_BRANCH_IDX, null == markElse ? join : markElse);
    if (null != thenJoin) {
      controlFlowGraph.putEdge(thenJoin, JGotoStmt.BRANCH_IDX, join);
    }
    if (null != elseJoin) {
      controlFlowGraph.putEdge(elseJoin, JGotoStmt.BRANCH_IDX, join);
    }
  }

  @Override
  public <T> void visitCtInvocation(final CtInvocation<T> ctInvocation) {
    final CtExecutableReference<T> ctExecutableReference = ctInvocation.getExecutable();

    final MethodSignature methodSignature = signature(ctExecutableReference);
    final List<Immediate> jiArgs = ctInvocation.getArguments().stream()
        .map(ctArg -> makeImmediate(evaluate(ctArg), ctArg)).toList();
    final AbstractInvokeExpr jiExpr;
    final CtExpression<?> ctTarget = ctInvocation.getTarget();
    if (null == ctTarget) {
      if (!"<init>".equals(methodSignature.getName())) {
        LOG.warn("No target for invocation {} in {}", ctInvocation,
            ctInvocation.getParent(CtExecutable.class));
        return;
      }
      jiExpr = Jimple.newSpecialInvokeExpr(thisLocal, methodSignature, jiArgs);
    } else if (ctTarget instanceof CtTypeAccess<?>) {
      jiExpr = Jimple.newStaticInvokeExpr(methodSignature, jiArgs);
    } else if (ctTarget instanceof CtSuperAccess<?>) {
      jiExpr = Jimple.newSpecialInvokeExpr(thisLocal, methodSignature, jiArgs);
    } else {
      final Value base = evaluate(ctInvocation.getTarget());
      jiExpr = Jimple.newVirtualInvokeExpr(makeLocal(base, ctInvocation), methodSignature, jiArgs);
    }

    if (ctInvocation.getParent() instanceof CtBlock<?>) {
      final JInvokeStmt jiInvocation = Jimple.newInvokeStmt(jiExpr, noPos);
      emit(jiInvocation, ctInvocation);
    } else {
      setResult(applyCasts(jiExpr, ctInvocation.getTypeCasts()));
    }
  }

  @Override
  public void visitCtJavaDoc(final CtJavaDoc javaDoc) {
    // intentionally left blank
  }

  @Override
  public <T> void visitCtLambda(final CtLambda<T> lambda) {
    // FIXME: implement lambda
  }

  @Override
  public <T> void visitCtLiteral(final CtLiteral<T> ctLiteral) {
    Object literalValue = ctLiteral.getValue();
    CtTypeReference<?> vt = ctLiteral.getType();

    for (final CtTypeReference<?> cast : ctLiteral.getTypeCasts().reversed()) {
      final String castName = cast.unbox().getQualifiedName();
      switch (castName) {
      case FQN_BOOLEAN_TYPE:
        literalValue = (Boolean) literalValue ? Boolean.TRUE : Boolean.FALSE;
        break;
      case FQN_CHAR_TYPE:
        if (literalValue instanceof final Number n) {
          literalValue = Character.valueOf((char) n.intValue());
        } else {
        }
        break;
      case FQN_STRING:
        // nop
        break;
      default:
        if (literalValue instanceof final Character c) {
          literalValue = Integer.valueOf(c);
        }
        final Number nv = (Number) literalValue;
        literalValue = switch (castName) {
        case FQN_BYTE_TYPE -> Byte.valueOf(nv.byteValue());
        case FQN_SHORT_TYPE -> Short.valueOf(nv.shortValue());
        case FQN_INT_TYPE -> Integer.valueOf(nv.intValue());
        case FQN_LONG_TYPE -> Long.valueOf(nv.longValue());
        case FQN_FLOAT_TYPE -> Float.valueOf(nv.floatValue());
        case FQN_DOUBLE_TYPE -> Double.valueOf(nv.doubleValue());
        default -> throw implement(cast);
        };
        break;
      }
      vt = cast;
    }

    // literalValue contains the value boxed with proper type
    // vt contains the cast type

    // CAVE: there is no point in using BooleanConstant, as we will fail to compare
    // to the reference code using IntConstant(0/1)
    // CAVE: there is no ByteConstant/ShortConstant/CharConstant propagating type
    // information is tricky
    final Immediate jiImmediate = switch (literalValue) {
    // CAVE: null may be cast to numbers, other types.
    case null -> NullConstant.getInstance();
    case final Boolean b -> BooleanConstant.getInstance(b);
//    case Boolean b -> b ? IntConstant.getInstance(1) : IntConstant.getInstance(0);
    case final Byte y -> IntConstant.getInstance(y);
    case final Short h -> IntConstant.getInstance(h);
    case final Integer i -> IntConstant.getInstance(i);
    case final Long l -> LongConstant.getInstance(l);
    case final Float f -> FloatConstant.getInstance(f);
    case final Double d -> DoubleConstant.getInstance(d);
    case final Character c -> IntConstant.getInstance(c);
    case final String s -> new StringConstant(s, stringType());
    default -> throw implement(ctLiteral);
    };

    if (null == literalValue || vt.isPrimitive() || literalValue instanceof String) {
      setResult(jiImmediate);
    } else {
      setResult(boxingConversion(jiImmediate, ctLiteral));
    }
  }

  @Override
  public <T> void visitCtLocalVariable(final CtLocalVariable<T> ctLocalVariable) {
    final Local newLocal = JavaJimple.newLocal(ctLocalVariable.getSimpleName(),
        toSootType(ctLocalVariable.getType()));
    bodyBuilder.addLocal(newLocal);
    final Local existed = executableLocals.put(newLocal.getName(), newLocal);
    assert null == existed;
    final CtExpression<T> ctDefaultExpression = ctLocalVariable.getDefaultExpression();
    if (null != ctDefaultExpression) {
      final Value expr = evaluate(ctDefaultExpression);
      final JAssignStmt newAssignStmt = Jimple.newAssignStmt(newLocal,
          assignmentContext(newLocal.getType(), expr, ctDefaultExpression), noPos);
      emit(newAssignStmt, ctLocalVariable);
    }
    setResult(newLocal);
  }

  @Override
  public <T> void visitCtLocalVariableReference(
      final CtLocalVariableReference<T> ctVariableReference) {
    final Local localReference = executableLocals.get(ctVariableReference.getSimpleName());
    setResult(localReference);
  }

  @Override
  public <T> void visitCtMethod(final CtMethod<T> ctMethod) {
    final JavaClassType classType = toSootClassType(ctMethod.getDeclaringType());
    currentBlock = new ArrayList<>();
    paramLocals = new LinkedHashMap<>();
    executableLocals = new LinkedHashMap<>();

    if (!ctMethod.isStatic()) {
      thisLocal = JavaJimple.newLocal("this", classType);
      final JThisRef thisRef = Jimple.newThisRef(classType);
      final JIdentityStmt thisStmt = new JIdentityStmt(thisLocal, thisRef, noPos);
      bodyBuilder.addLocal(thisLocal);
      emit(thisStmt, ctMethod);
    }

    scan(ctMethod.getParameters());

    mark();

    scan(ctMethod.getBody());

    finish(ctMethod.getType());
  }

  @Override
  public <T> void visitCtNewArray(final CtNewArray<T> ctNewArray) {
    final CtArrayTypeReference<?> ctArrayType = (CtArrayTypeReference<?>) ctNewArray.getType();

    final List<CtExpression<?>> elements = ctNewArray.getElements();
    final Value expr;
    if (null == elements || elements.isEmpty()) {
      final int dimensions = ctArrayType.getDimensionCount();
      final Type baseType = toSootType(ctArrayType.getArrayType());
      if (1 == dimensions) {
        final List<CtExpression<Integer>> dimExpr = ctNewArray.getDimensionExpressions();
        final Value jiDim;
        final CtExpression<Integer> ctDim;
        if (dimExpr.isEmpty()) {
          ctDim = null;
          jiDim = IntConstant.getInstance(0);
        } else {
          ctDim = dimExpr.getFirst();
          jiDim = evaluate(ctDim);
        }
        expr = Jimple.newNewArrayExpr(baseType, makeImmediate(jiDim, ctDim), identifierFactory);
      } else {
        final ArrayType arrayType = Type.createArrayType(baseType, dimensions);
        final List<Immediate> dims = new ArrayList<>(dimensions);
        for (final CtExpression<Integer> ctDimExpression : ctNewArray.getDimensionExpressions()) {
          dims.add(makeImmediate(evaluate(ctDimExpression), ctDimExpression));
        }
        expr = Jimple.newNewMultiArrayExpr(arrayType, dims);
      }
    } else {
      expr = initArray(toSootType(ctArrayType.getComponentType()), ctNewArray);
    }
    setResult(applyCasts(expr, ctNewArray.getTypeCasts()));
  }

  @Override
  public <T> void visitCtNewClass(final CtNewClass<T> ctNewClass) {
    // FIXME: implement anonymous class
  }

  @Override
  public <T, A extends T> void visitCtOperatorAssignment(
      final CtOperatorAssignment<T, A> ctOperatorAssignment) {
    final CtExpression<T> ctAssigned = ctOperatorAssignment.getAssigned();
    final Value leftExpr = evaluate(ctAssigned);
    final Local leftSaved = makeDummy(leftExpr, ctAssigned);
    final Value right = evaluate(ctOperatorAssignment.getAssignment());

    Value res = switch (ctOperatorAssignment.getKind()) {
    case SL -> opBinOp(ctOperatorAssignment.getKind(), leftSaved, right, JavaJimple::newShlExpr,
        ctOperatorAssignment);
    case SR -> opBinOp(ctOperatorAssignment.getKind(), leftSaved, right, JavaJimple::newShrExpr,
        ctOperatorAssignment);
    case USR -> opBinOp(ctOperatorAssignment.getKind(), leftSaved, right, JavaJimple::newUshrExpr,
        ctOperatorAssignment);

    case MINUS -> opBinOp(ctOperatorAssignment.getKind(), leftSaved, right, JavaJimple::newSubExpr,
        ctOperatorAssignment);
    case DIV -> opBinOp(ctOperatorAssignment.getKind(), leftSaved, right, JavaJimple::newDivExpr,
        ctOperatorAssignment);
    case MUL -> opBinOp(ctOperatorAssignment.getKind(), leftSaved, right, JavaJimple::newMulExpr,
        ctOperatorAssignment);
    case MOD -> opBinOp(ctOperatorAssignment.getKind(), leftSaved, right, JavaJimple::newRemExpr,
        ctOperatorAssignment);
    case PLUS -> opBinOp(ctOperatorAssignment.getKind(), leftSaved, right, JavaJimple::newAddExpr,
        ctOperatorAssignment);
    case BITAND -> opBinOp(ctOperatorAssignment.getKind(), leftSaved, right, JavaJimple::newAndExpr,
        ctOperatorAssignment);
    case BITOR -> opBinOp(ctOperatorAssignment.getKind(), leftSaved, right, JavaJimple::newOrExpr,
        ctOperatorAssignment);
    case BITXOR -> opBinOp(ctOperatorAssignment.getKind(), leftSaved, right, JavaJimple::newXorExpr,
        ctOperatorAssignment);

    default -> throw implement(ctOperatorAssignment);
    };
    res = applyCasts(res, ctOperatorAssignment.getTypeCasts());
    res = applyCasts(res, List.of(ctOperatorAssignment.getAssigned().getType()));

    final JAssignStmt stmt = Jimple.newAssignStmt(makeLValue(leftExpr), res, noPos);
    emit(stmt, ctOperatorAssignment);

    setResult(leftExpr);
  }

  @Override
  public <T> void visitCtParameter(final CtParameter<T> ctParameter) {
    final Type paramType = toSootType(ctParameter.getType());
    final JParameterRef paramRef = Jimple.newParameterRef(paramType, paramLocals.size());
    final Local paramLocal = JavaJimple.newLocal(ctParameter.getSimpleName(), paramType);
    paramLocals.put(ctParameter.getSimpleName(), paramLocal);
    final JIdentityStmt paramStmt = new JIdentityStmt(paramLocal, paramRef, noPos);
    bodyBuilder.addLocal(paramLocal);
    emit(paramStmt, ctParameter);
  }

  @Override
  public <T> void visitCtParameterReference(final CtParameterReference<T> ctParameterReference) {
    final Local localReference = paramLocals.get(ctParameterReference.getSimpleName());
    setResult(localReference);
  }

  @Override
  public <R> void visitCtReturn(final CtReturn<R> ctReturnStatement) {
    final Type jiReturnType = toSootType(ctReturnStatement.getParent(CtMethod.class).getType());
    if (jiReturnType instanceof VoidType) {
      final JReturnVoidStmt returnStmt = Jimple.newReturnVoidStmt(noPos);
      emit(returnStmt, ctReturnStatement);
    } else {
      final CtExpression<R> ctReturnedExpression = ctReturnStatement.getReturnedExpression();
      final Value assignedExpr = assignmentContext(jiReturnType, evaluate(ctReturnedExpression),
          ctReturnedExpression);
      final JReturnStmt returnStmt = Jimple
          .newReturnStmt(makeImmediate(assignedExpr, ctReturnedExpression), noPos);
      emit(returnStmt, ctReturnStatement);
    }
    flush();
  }

  @Override
  public <T> void visitCtSuperAccess(final CtSuperAccess<T> ctSuperAccess) {
    // FIXME: implement
  }

  @Override
  public <S> void visitCtSwitch(final CtSwitch<S> ctSwitchStatement) {
    // FIXME: implement
  }

  @Override
  public <T, S> void visitCtSwitchExpression(final CtSwitchExpression<T, S> ctSwitchExpression) {
    // FIXME: implement
  }

  @Override
  public void visitCtSynchronized(final CtSynchronized ctSynchronized) {
    // FIXME: implement
  }

  @Override
  public void visitCtTextBlock(final CtTextBlock ctTextBlock) {
    final StringConstant jiStringConstant = JavaJimple.newStringConstant(ctTextBlock.getValue(),
        identifierFactory);
    setResult(applyCasts(jiStringConstant, ctTextBlock.getTypeCasts()));
  }

  @Override
  public <T> void visitCtThisAccess(final CtThisAccess<T> thisAccess) {
    setResult(thisLocal);
  }

  @Override
  public void visitCtThrow(final CtThrow ctThrowStatement) {
    final CtExpression<? extends Throwable> ctThrownExpression = ctThrowStatement
        .getThrownExpression();
    final Value jiThrown = evaluate(ctThrownExpression);
    final JThrowStmt jiThrowStmt = Jimple.newThrowStmt(makeImmediate(jiThrown, ctThrownExpression),
        noPos);
    emit(jiThrowStmt, ctThrowStatement);
  }

  @Override
  public void visitCtTry(final CtTry tryBlock) {
    // FIXME: for now only the happy path
    scan(CtRole.BODY, tryBlock.getBody());
  }

  @Override
  public void visitCtTryWithResource(final CtTryWithResource tryWithResource) {
    // FIXME: implement
    scan(tryWithResource.getBody());
  }

  @Override
  public <T> void visitCtUnaryOperator(final CtUnaryOperator<T> operator) {
    final CtExpression<?> ctOperand = operator.getOperand();
    final Value res = switch (operator.getKind()) {
    case POS -> {
      final Value expr = evaluate(ctOperand);
      yield unaryNumericPromote(expr);
    }
    case NEG -> {
      final Value op1 = evaluate(ctOperand);
      if (op1 instanceof final NumericConstant<?> numeric1) {
        yield numeric1.negate();
      } else {
        yield Jimple.newNegExpr(makeImmediate(op1, ctOperand));
      }
    }
    case NOT -> {
      final Value op1 = evaluate(ctOperand);
      if (op1 instanceof final BooleanConstant bc1) {
        yield bc1.xor(BooleanConstant.getTrue());
      } else {
        yield Jimple.newXorExpr(makeImmediate(op1, ctOperand), BooleanConstant.getTrue());
      }
    }
    case COMPL -> {
      final Value op1 = evaluate(ctOperand);
      yield switch (op1) {
      case final IntConstant ic -> ic.xor(IntConstant.getInstance(-1));
      case final LongConstant lc -> lc.xor(LongConstant.getInstance(-1));
      default -> Jimple.newXorExpr(makeImmediate(op1, ctOperand), IntConstant.getInstance(-1));
      };
    }
    case PREDEC -> opFix(operator, true, -1);
    case POSTDEC -> opFix(operator, false, -1);
    case PREINC -> opFix(operator, true, 1);
    case POSTINC -> opFix(operator, false, 1);
    default -> throw implement(operator);
    };
    setResult(applyCasts(res, operator.getTypeCasts()));
  }

  @Override
  public <T> void visitCtVariableRead(final CtVariableRead<T> variableRead) {
    handleCtVariableAccess(variableRead);
  }

  @Override
  public <T> void visitCtVariableWrite(final CtVariableWrite<T> variableWrite) {
    handleCtVariableAccess(variableWrite);
  }

  @Override
  public void visitCtWhile(final CtWhile ctWhileLoop) {
    openFrame(ctWhileLoop);

    final CtExpression<Boolean> ctLoopingExpression = ctWhileLoop.getLoopingExpression();
    final JIfStmt whileIf = Jimple.newIfStmt(makeConditional(ctLoopingExpression),
        toPositionInfo(ctLoopingExpression));
    emit(whileIf, ctWhileLoop);
    currentFrame.add(Break.of(BreakKind.BODY, whileIf, JIfStmt.TRUE_BRANCH_IDX));
    currentFrame.add(Break.of(BreakKind.JOIN, whileIf, JIfStmt.FALSE_BRANCH_IDX));

    final JNopStmt whileBody = mark();
    scan(ctWhileLoop.getBody());
    jmp(currentFrame.jiBeginMark(), ctWhileLoop);

    pop(whileBody, currentFrame.jiBeginMark(), mark());
  }

  @Override
  protected void enter(final CtElement ctElement) {
    throw implement(ctElement);
  }

  protected <T extends Value> T setResult(final T jiValue) {
    this.result = jiValue;
    LOG.trace("result={}", jiValue);
    return jiValue;
  }

  private Value applyCasts(final Value jiExpr, final List<CtTypeReference<?>> ctTypeCasts) {
    if (null == ctTypeCasts || ctTypeCasts.isEmpty()) {
      return jiExpr;
    }
    Value expr = jiExpr;
    for (final CtTypeReference<?> ctType : ctTypeCasts.reversed()) {
      if (ctType.isPrimitive()) {
        expr = unboxingConversion(expr);
        final Immediate dummy = makeImmediate(expr, ctType);
        expr = Jimple.newCastExpr(dummy, toSootType(ctType));
      } else {
        final Immediate dummy = makeImmediate(expr, ctType);
        expr = switch (ctType.getQualifiedName()) {
        case FQN_BOOLEAN, FQN_BYTE, FQN_SHORT, FQN_INTEGER, FQN_LONG, FQN_FLOAT, FQN_DOUBLE -> {
          final JavaClassType jiBoxedType = toSootClassType(ctType);
          yield Jimple.newStaticInvokeExpr(identifierFactory.getMethodSignature(jiBoxedType,
              "valueOf", jiBoxedType, List.of(expr.getType())), dummy);
        }
        default -> Jimple.newCastExpr(dummy, toSootType(ctType));
        };
      }
    }
    return expr;
  }

  private Value assignmentContext(@NonNull final Type jiType, final Value jiValue,
      final CtElement ctElement) {
    // 5.1.1 Identity Conversion
    if (jiType.equals(jiValue.getType())) {
      return jiValue;
    }
    // 5.1.2. Widening primitive
    if (jiType instanceof final PrimitiveType castType) {
      final Optional<Value> widened = wideningConversion(castType, jiValue, null);
      if (widened.isPresent()) {
        return widened.get();
      }
    }

    // 5.1.5. Widening reference conversion

    // Widening reference + unboxing

    // Widening reference + unboxing + widening primitive

    // 5.1.7. Boxing conversion
    if (jiValue.getType() instanceof PrimitiveType && jiType instanceof JavaClassType) {
      return boxingConversion(jiValue, ctElement);
    }

    // Boxing conversion + widening reference conversion

    // 5.1.8. Unboxing conversion
    if (jiType instanceof final PrimitiveType pt
        && jiValue.getType() instanceof final JavaClassType ct) {
      return unboxingConversion(jiValue);
    }

    // Unboxing conversion + widening primitive

    // Unchecked conversion

    // Narrowing constant conversion
    if (jiValue instanceof final IntConstant ic && jiType instanceof final PrimitiveType pt) {
      switch (pt.getName()) {
      case FQN_BYTE_TYPE:
        if (ic.getValue() >= Byte.MIN_VALUE && ic.getValue() <= Byte.MAX_VALUE) {
          return ic;
        }
        break;
      case FQN_SHORT_TYPE:
        if (ic.getValue() >= Short.MIN_VALUE && ic.getValue() <= Short.MAX_VALUE) {
          return ic;
        }
        break;
      case FQN_CHAR_TYPE:
        if (ic.getValue() >= Character.MIN_VALUE && ic.getValue() <= Character.MAX_VALUE) {
          return ic;
        }
        break;
      default:
        break;
      }
    }

    // FIXME: this is not ok.
    return Jimple.newCastExpr(makeImmediate(jiValue, ctElement), jiType);
  }

  /**
   * 5.1.7 Boxing Conversion.
   *
   * @param expr      the Value to box
   * @param ctElement
   * @return boxed value, or same
   */
  private Value boxingConversion(final Value expr, final CtElement ctElement) {
    // fix this flawed code, see https://github.com/soot-oss/SootUp/pull/1815
//  identifierFactory.getBoxedType(pt);
    if (expr.getType() instanceof final PrimitiveType jiPrimitiveType) {
      final JavaClassType jiBoxedType = toBoxedType(jiPrimitiveType);
      final MethodSignature methodSignature = identifierFactory.getMethodSignature(jiBoxedType,
          "valueOf", jiBoxedType, List.of(expr.getType()));
      final JStaticInvokeExpr valueOf = Jimple.newStaticInvokeExpr(methodSignature,
          makeImmediate(expr, ctElement));
      return valueOf;
    }
    return expr;
  }

  private <T extends Stmt> T emit(final T jiStmt, final CtElement ctElement) {
    currentBlock.add(jiStmt);
    stmtElementLink.put(jiStmt, ctElement);
    if (jiStmt.branches() || jiStmt.isJThrowStmt()) {
      flush();
    }
    return jiStmt;
  }

  private void finish(final CtTypeReference<?> ctReturnType) {
    flush();

    // add implicit return
    if ("void".equals(ctReturnType.getQualifiedName())) {
      final List<Stmt> fix = controlFlowGraph.getNodes().stream()
          .filter(
              n -> 1 == n.getExpectedSuccessorCount() && 1 != controlFlowGraph.successors(n).size())
          .toList();
      fix.forEach(n -> {
        final JReturnVoidStmt implicitReturn = Jimple.newReturnVoidStmt(noPos);
        controlFlowGraph.addNode(implicitReturn);
        controlFlowGraph.putEdge((FallsThroughStmt) n, implicitReturn);
      });
    }

    // remove NOP glue, we first collect everything into a list, then run the
    // remover to avoid racing on the iteration
    controlFlowGraph.getNodes().stream().filter(Stmt::isJNopStmt).toList()
        .forEach(controlFlowGraph::removeNode);

    // remove redundant goto
    // FIXME: rework this

    List<BasicBlock<?>> blocks = new ArrayList<>(controlFlowGraph.getBlocksSorted());
    for (int i = 0; i < blocks.size() - 1; i++) {
      BasicBlock<?> singleBlock = blocks.get(i);
      Stmt jiBlockTail = singleBlock.getTail();
      List<? extends BasicBlock<?>> blockSuccessors = singleBlock.getSuccessors();
      if (!(jiBlockTail instanceof JGotoStmt jiSingleGoto) || 1 != blockSuccessors.size()) {
        continue;
      }
      BasicBlock<?> blockSuccessor = blockSuccessors.get(0);
      // basicblock with a single goto statement
      if (singleBlock.getHead().equals(jiBlockTail)) {
        BasicBlock<?> blockPredecessor = singleBlock.getPredecessors().get(0);
        if (blockPredecessor.getTail() instanceof JIfStmt jiPrecedingIf
            && blockPredecessor.getSuccessors().indexOf(singleBlock) == JIfStmt.TRUE_BRANCH_IDX
            && 1 == singleBlock.getPredecessors().size()
            && singleBlock.getSuccessors().get(0) == blocks.get(i + 1)) {
          LOG.debug("Removing transitive single {} at {} new target is {}", jiSingleGoto,
              jiPrecedingIf, blockSuccessor.getHead());
          controlFlowGraph.removeEdge(jiPrecedingIf, jiSingleGoto);
          controlFlowGraph.putEdge(jiPrecedingIf, JIfStmt.TRUE_BRANCH_IDX,
              blockSuccessor.getHead());
          controlFlowGraph.removeBlock(singleBlock);
        }
        continue;
      }
      // basicblock with a single successor branching to the next in sort-order
      // basicblock and that basic block has only (this) single predecessor
      BasicBlock<?> nextBlock = blocks.get(i + 1);
      if (blockSuccessor == nextBlock && 1 == nextBlock.getPredecessors().size()) {
        // remove the goto which will join the two basicblocks
        LOG.debug("Joining basicblocks at {}", jiSingleGoto.getPositionInfo());
        controlFlowGraph.removeNode(jiSingleGoto, true);
        continue;
      }

    }

    // System.out.println("EntryPoints");
    // controlFlowGraph.getEntrypoints().forEach(System.out::println);
    // System.out.println("Tails");
    // Stmt t1 = controlFlowGraph.getTails().getFirst();
    // controlFlowGraph.hasEdgeConnecting(t1, t1)
  }

  private void flush() {
    if (currentBlock.isEmpty()) {
      return;
    }
    LOG.trace("flushing");
    currentBlock.forEach(s -> LOG.trace(s.toString()));

    controlFlowGraph.addBlock(currentBlock);
    if (null == controlFlowGraph.getStartingStmt()) {
      controlFlowGraph.setStartingStmt(currentBlock.getFirst());
    }
    currentBlock.clear();
  }

  private <T> void handleCtFieldAccess(final CtFieldAccess<T> ctFieldAccess) {
    final CtFieldReference<?> f = ctFieldAccess.getVariable();

    final Value field;
    if (ctFieldAccess.getTarget() instanceof final CtTypeAccess ctTa) {
      if ("class".equals(f.getSimpleName())) {
        field = JavaJimple.newClassConstant(descriptor(ctTa.getAccessedType()), identifierFactory);
      } else {
        final CtField<?> ctField = f.getFieldDeclaration();
        if (null != ctField && ctField.hasModifier(ModifierKind.FINAL)
            && null != ctField.getDefaultExpression() && ctField.getType().isPrimitive()) {
          field = evaluate(ctField.getDefaultExpression());
        } else {
          final FieldSignature sig = identifierFactory.getFieldSignature(f.getSimpleName(),
              toSootClassType(ctTa.getAccessedType()), toSootType(ctFieldAccess.getType()));
          field = Jimple.newStaticFieldRef(sig);
        }
      }
    } else if (ctFieldAccess.getTarget().getType().isArray()
        && "length".equals(f.getSimpleName())) {
      final Value base = evaluate(ctFieldAccess.getTarget());
      field = Jimple.newLengthExpr(makeImmediate(base, ctFieldAccess));
    } else {
      final FieldSignature sig = identifierFactory.getFieldSignature(f.getSimpleName(),
          toSootClassType(ctFieldAccess.getTarget().getType()),
          toSootType(ctFieldAccess.getType()));
      final Value base = evaluate(ctFieldAccess.getTarget());
      field = Jimple.newInstanceFieldRef(makeLocal(base, ctFieldAccess), sig);
    }
    setResult(applyCasts(field, ctFieldAccess.getTypeCasts()));
  }

  private <T> void handleCtVariableAccess(final CtVariableAccess<T> ctVariableAccess) {
    setResult(
        applyCasts(evaluate(ctVariableAccess.getVariable()), ctVariableAccess.getTypeCasts()));
  }

  private IllegalArgumentException implement(final CtElement ctElement) {
    return new IllegalArgumentException(
        "implement! %s//%s".formatted(ctElement.getClass().getSimpleName(), ctElement));
  }

  private IllegalArgumentException implement(final Value jiValue) {
    return new IllegalArgumentException(
        "implement! %s//%s".formatted(jiValue.getClass().getSimpleName(), jiValue));
  }

  private Value initArray(final Type jiElementType, final CtNewArray<?> ctNewArray) {
    final List<CtExpression<?>> ctElements = ctNewArray.getElements();
    final JNewArrayExpr jiNewArrayExpr = Jimple.newNewArrayExpr(jiElementType,
        IntConstant.getInstance(ctElements.size()), identifierFactory);
    final Local dummy = makeDummy(jiNewArrayExpr, ctNewArray);
    for (int i = 0; i < ctElements.size(); i++) {
      final JArrayRef aw = Jimple.newArrayRef(dummy, IntConstant.getInstance(i));
      final CtExpression<?> ctElementIndexed = ctElements.get(i);
      final JAssignStmt aa = Jimple.newAssignStmt(aw, evaluate(ctElementIndexed), noPos);
      emit(aa, ctElementIndexed);
    }
    return dummy;
  }

  private Constant inlineConstant(final BinaryOperatorKind kind, final Immediate jiOp1,
      final Immediate jiOp2) {
    if (jiOp1 instanceof final LogicalConstant sc1
        && jiOp2 instanceof final LogicalConstant<?> sc2) {
      switch (kind) {
      case BITAND:
        return sc1.and(sc2);
      case BITOR:
        return sc1.or(sc2);
      case BITXOR:
        return sc1.xor(sc2);
      default:
        break;
      }
    }

    // FIXME: you CAN shift with a long
    // see https://github.com/soot-oss/SootUp/pull/1809
    if (jiOp1 instanceof final ShiftableConstant sc1 && jiOp2 instanceof final IntConstant sc2) {
      switch (kind) {
      case SL:
        return sc1.shiftLeft(sc2);
      case SR:
        return sc1.shiftRight(sc2);
      case USR:
        return sc1.unsignedShiftRight(sc2);
      default:
        break;
      }
    }

    if (jiOp1 instanceof final BooleanConstant bc1 && jiOp2 instanceof final BooleanConstant bc2) {
      return switch (kind) {
      case EQ -> bc1.equalEqual(bc2);
      case NE -> bc1.notEqual(bc2);
      default -> throw new IllegalArgumentException();
      };
    }

    // FIXME: FloatConstant comparisons are WRONG.
    // https://github.com/soot-oss/SootUp/pull/1809
    if (jiOp1 instanceof final NumericConstant nc1
        && jiOp2 instanceof final NumericConstant<?> nc2) {
      try {
        return switch (kind) {
        case LT -> nc1.lessThan(nc2);
        case LE -> nc1.lessThanOrEqual(nc2);
        case EQ -> nc1.equalEqual(nc2);
        case GE -> nc1.greaterThanOrEqual(nc2);
        case GT -> nc1.greaterThan(nc2);
        case NE -> nc1.notEqual(nc2);
        case PLUS -> nc1.add(nc2);
        case MINUS -> nc1.subtract(nc2);
        case MUL -> nc1.multiply(nc2);
        case DIV -> nc1.divide(nc2);
        case MOD -> nc1.remainder(nc2);
        default -> throw new IllegalArgumentException();
        };
      } catch (final ArithmeticException ex) {
        return null;
      }
    }
    return null;
  }

  private JGotoStmt jmp(final CtElement ctElement) {
    final JGotoStmt jmp = Jimple.newGotoStmt(toPositionInfo(ctElement));
    return emit(jmp, ctElement);
  }

  private JGotoStmt jmp(final Stmt back, final CtElement ctElement) {
    final JGotoStmt jmp = Jimple.newGotoStmt(toPositionInfo(ctElement));
    emit(jmp, ctElement);
    if (!controlFlowGraph.containsNode(back)) {
      throw new IllegalArgumentException("Can only jump back to a Stmt that was already emitted.");
    }
    controlFlowGraph.putEdge(jmp, JGotoStmt.BRANCH_IDX, back);
    return jmp;
  }

  private AbstractConditionExpr makeConditional(final CtExpression<Boolean> ctElement) {
    final Value jiCondition = evaluate(ctElement);
    if (jiCondition instanceof AbstractConditionExpr jiAbstractConditionExpr) {
      return jiAbstractConditionExpr;
    }
    return Jimple.newEqExpr(IntConstant.getInstance(1), makeImmediate(jiCondition, ctElement));
  }

  private JavaLocal makeDummy(final Value res, final CtElement ctElement) {
    return (JavaLocal) makeDummyAssign(res, ctElement).getLeftOp();
  }

  private JAssignStmt makeDummyAssign(final Value res, final CtElement ctElement) {
    final String dummyName = "$stack%d".formatted(dummyCounter++);
    final JavaLocal dummyLocal = JavaJimple.newLocal(dummyName, res.getType());
    bodyBuilder.addLocal(dummyLocal);
    final JAssignStmt dummy = Jimple.newAssignStmt(dummyLocal, res, noPos);
    emit(dummy, ctElement);
    return dummy;
  }

  private Immediate makeImmediate(final Value jiExpr, final CtElement ctElement) {
    if (jiExpr instanceof final Immediate jiImm) {
      return jiImm;
    }
    return makeDummy(jiExpr, ctElement);
  }

  private Local makeLocal(final Value res, final CtElement ctElement) {
    if (res instanceof final Local resLocal) {
      return resLocal;
    }
    return makeDummy(res, ctElement);
  }

  private LValue makeLValue(final Value jiValue) {
    if (jiValue instanceof final LValue jiLValue) {
      return jiLValue;
    }
    throw implement(jiValue);
  }

  private JNopStmt mark() {
    final JNopStmt jiMarker = Jimple.newNopStmt(noPos);
    emit(jiMarker, null);
    return jiMarker;
  }

  private JNopStmt mark(CtElement ctElement) {
    final JNopStmt jiMarker = Jimple.newNopStmt(toPositionInfo(ctElement));
    emit(jiMarker, null);
    return jiMarker;
  }

  private <T> Value opBinOp(final BinaryOperatorKind kind, final Value jiLeftValue,
      final Value jiRightValue, final BiFunction<Immediate, Immediate, AbstractBinopExpr> exprImpl,
      final CtElement ctElementOperator) {

    if (BinaryOperatorKind.PLUS == kind
        && (jiLeftValue.getType() instanceof final JavaClassType leftClass
            && FQN_STRING.equals(leftClass.getFullyQualifiedName())
            || jiRightValue.getType() instanceof final JavaClassType rightClass
                && FQN_STRING.equals(rightClass.getFullyQualifiedName()))) {
//      If the type of either operand of a + operator is String, then the operation is string
//      concatenation.
      return opStringConcat(kind, jiLeftValue, jiRightValue, ctElementOperator);
    }

    // perform binary numeric promotion 5.6 Numeric Contexts
    // 1. unbox
    Value left = unboxingConversion(jiLeftValue);
    Value right = unboxingConversion(jiRightValue);

    final PrimitiveType doubleType = identifierFactory.getPrimitiveType("double").get();
    final PrimitiveType floatType = identifierFactory.getPrimitiveType("float").get();
    final PrimitiveType longType = identifierFactory.getPrimitiveType("long").get();
    final PrimitiveType intType = identifierFactory.getPrimitiveType("int").get();

    // 2. widening/narrowing primitive conversion
    if (doubleType.equals(left.getType())) {
      right = wideningConversion(doubleType, right, ctElementOperator).orElse(right);
    } else if (doubleType.equals(right.getType())) {
      left = wideningConversion(doubleType, left, ctElementOperator).orElse(left);
    } else if (floatType.equals(left.getType())) {
      right = wideningConversion(floatType, right, ctElementOperator).orElse(right);
    } else if (floatType.equals(right.getType())) {
      left = wideningConversion(floatType, left, ctElementOperator).orElse(left);
    } else if (longType.equals(left.getType())) {
      right = wideningConversion(longType, right, ctElementOperator).orElse(right);
    } else if (longType.equals(right.getType())) {
      left = wideningConversion(longType, left, ctElementOperator).orElse(left);
    } else {
      // TODO: numeric choice context
      left = wideningConversion(intType, left, ctElementOperator).orElse(left);
      right = wideningConversion(intType, right, ctElementOperator).orElse(right);
    }

    final Immediate op1 = makeImmediate(left, null);
    final Immediate op2 = makeImmediate(right, null);

    final Constant res = inlineConstant(kind, op1, op2);
    if (null != res) {
      return res;
    }

    final AbstractBinopExpr jiBinOpExpr = exprImpl.apply(op1, op2);
    return jiBinOpExpr;
  }

  private Value opBinOp(final CtBinaryOperator<?> operator,
      final BiFunction<Immediate, Immediate, AbstractBinopExpr> exprImpl) {
    return opBinOp(operator.getKind(), evaluate(operator.getLeftHandOperand()),
        evaluate(operator.getRightHandOperand()), exprImpl, operator);
  }

  private Value opFix(final CtUnaryOperator<?> ctOperator, final boolean pre, final int delta) {
    final CtExpression<?> ctOperand = ctOperator.getOperand();
    final Value op1 = evaluate(ctOperand);
    final IntConstant op2 = IntConstant.getInstance(delta);
    final JAddExpr addExpr = Jimple.newAddExpr(makeImmediate(op1, ctOperand), op2);
    final JAssignStmt newAssignStmt = Jimple.newAssignStmt(makeLValue(op1), addExpr, noPos);
    if (ctOperator.getParent() instanceof CtBlock<?>) {
      emit(newAssignStmt, ctOperator);
      return null;
    }
    JavaLocal dummy;
    if (pre) {
      emit(newAssignStmt, ctOperator);
      dummy = makeDummy(op1, ctOperator);
    } else {
      dummy = makeDummy(op1, ctOperator);
      emit(newAssignStmt, ctOperator);
    }
    return dummy;
  }

  private Value opInstanceOf(final CtBinaryOperator<?> ctBinaryOperator) {
    final CtExpression<?> ctLeft = ctBinaryOperator.getLeftHandOperand();
    final Value jiLeft = evaluate(ctLeft);
    final CtTypeReference<?> right = ((CtTypeAccess<?>) ctBinaryOperator.getRightHandOperand())
        .getAccessedType();
    final Type jiRight = toSootType(right);
    final JInstanceOfExpr expr = Jimple.newInstanceOfExpr(makeImmediate(jiLeft, ctLeft), jiRight);
    return expr;
  }

  private Local opLogicalAnd(final CtBinaryOperator<?> ctBinaryOperator) {
    final CtExpression<?> ctLeft = ctBinaryOperator.getLeftHandOperand();
    final Value leftValue = evaluate(ctLeft);
    final AbstractConditionExpr left = switch (leftValue) {
    case final AbstractConditionExpr acexpr -> acexpr;
    case final Local l -> Jimple.newEqExpr(l, IntConstant.getInstance(0));
    default -> throw implement(ctBinaryOperator);
    };

    final JIfStmt ifLeftStmt = Jimple.newIfStmt(left, noPos);
    emit(ifLeftStmt, ctLeft);
    flush();

    final JNopStmt ifRightMarker = mark();
    final CtExpression<Boolean> ctRight = (CtExpression<Boolean>) ctBinaryOperator
        .getRightHandOperand();
    final AbstractConditionExpr right = makeConditional(ctRight);
    final JIfStmt ifRightStmt = Jimple.newIfStmt(right, noPos);
    emit(ifRightStmt, ctRight);
    flush();

    final JAssignStmt andTrue = makeDummyAssign(BooleanConstant.getTrue(), ctRight);
    final JGotoStmt gtEnd = jmp(ctBinaryOperator);

    final JAssignStmt andFalse = Jimple.newAssignStmt(andTrue.getLeftOp(),
        BooleanConstant.getTrue(), noPos);
    emit(andFalse, ctBinaryOperator);
    flush();

    controlFlowGraph.putEdge(ifLeftStmt, JIfStmt.FALSE_BRANCH_IDX, andFalse);
    controlFlowGraph.putEdge(ifRightStmt, JIfStmt.FALSE_BRANCH_IDX, andFalse);
    controlFlowGraph.putEdge(ifRightStmt, JIfStmt.TRUE_BRANCH_IDX, andTrue);
    controlFlowGraph.putEdge(ifLeftStmt, JIfStmt.TRUE_BRANCH_IDX, ifRightMarker);

    final JNopStmt join = mark();
    controlFlowGraph.putEdge(andFalse, join);
    controlFlowGraph.putEdge(gtEnd, JGotoStmt.BRANCH_IDX, join);
    setResult((Local) andTrue.getLeftOp());
    return (Local) andTrue.getLeftOp();
  }

  private Local opLogicalOr(final CtBinaryOperator<?> ctBinaryOperator) {
    final CtExpression<Boolean> ctLeft = (CtExpression<Boolean>) ctBinaryOperator
        .getLeftHandOperand();
    final AbstractConditionExpr left = makeConditional(ctLeft);
    final JIfStmt ifLeftStmt = Jimple.newIfStmt(left, noPos);
    emit(ifLeftStmt, ctLeft);

    final JNopStmt ifRightMarker = mark();
    final CtExpression<Boolean> ctRight = (CtExpression<Boolean>) ctBinaryOperator
        .getRightHandOperand();
    final AbstractConditionExpr right = makeConditional(ctRight);
    final JIfStmt ifRightStmt = Jimple.newIfStmt(right, noPos);
    emit(ifRightStmt, ctRight);

    final JAssignStmt orFalse = makeDummyAssign(BooleanConstant.getFalse(), ctRight);
    final JGotoStmt gtEnd = jmp(ctBinaryOperator);

    controlFlowGraph.putEdge(ifLeftStmt, JIfStmt.FALSE_BRANCH_IDX, ifRightMarker);
    controlFlowGraph.putEdge(ifRightStmt, JIfStmt.FALSE_BRANCH_IDX, orFalse);

    final JAssignStmt orTrue = Jimple.newAssignStmt(orFalse.getLeftOp(), BooleanConstant.getTrue(),
        noPos);
    emit(orTrue, ctBinaryOperator);
    flush();
    controlFlowGraph.putEdge(ifRightStmt, JIfStmt.TRUE_BRANCH_IDX, orTrue);
    controlFlowGraph.putEdge(ifLeftStmt, JIfStmt.TRUE_BRANCH_IDX, orTrue);

    final JNopStmt join = mark();
    controlFlowGraph.putEdge(orTrue, join);
    controlFlowGraph.putEdge(gtEnd, JGotoStmt.BRANCH_IDX, join);
    setResult((Local) orFalse.getLeftOp());
    return (Local) orFalse.getLeftOp();
  }

  private Value opStringConcat(final BinaryOperatorKind kind, final Value jiLeftValue,
      final Value jiRightValue, final CtElement ctElement) {
    final Value leftString = stringConversion(jiLeftValue);
    final Value rightString = stringConversion(jiRightValue);
    if (leftString instanceof final StringConstant lsc
        && rightString instanceof final StringConstant rsc) {
      return JavaJimple.newStringConstant(lsc.getValue().concat(rsc.getValue()), identifierFactory);
    }
    final JavaClassType stringType = (JavaClassType) leftString.getType();
    final MethodSignature methodSignature = identifierFactory.getMethodSignature(stringType,
        "concat", stringType, List.of(stringType));
    return Jimple.newVirtualInvokeExpr(makeLocal(jiLeftValue, ctElement), methodSignature,
        makeImmediate(rightString, ctElement));
  }

  /**
   * 5.1.11 String Conversion.
   *
   * @param jiRight
   * @return the value converted to String
   */
  private Value stringConversion(Value jiRight) {
    final JavaClassType stringType = identifierFactory.getClassType(FQN_STRING);
    if (stringType.equals(jiRight.getType())) {
      return jiRight;
    }
    if (jiRight instanceof final Constant c) {
      final Object value = switch (c) {
      case final NullConstant nc -> "null";
      case final BooleanConstant bc -> Boolean.valueOf(bc.getValue());
      case final IntConstant ic -> Integer.valueOf(ic.getValue());
      case final LongConstant lc -> Long.valueOf(lc.getValue());
      case final FloatConstant fc -> Float.valueOf(fc.getValue());
      case final DoubleConstant dc -> Double.valueOf(dc.getValue());
      default -> throw implement(c);
      };
      return JavaJimple.newStringConstant(value.toString(), identifierFactory);
    }
    if (jiRight.getType() instanceof PrimitiveType) {
      jiRight = boxingConversion(jiRight, null);
    }
    // fixme handle "null"
    final MethodSignature methodSignature = identifierFactory
        .getMethodSignature((JavaClassType) jiRight.getType(), "toString", stringType, List.of());
    return Jimple.newVirtualInvokeExpr(makeLocal(jiRight, null), methodSignature);
  }

  private Value unaryNumericPromote(final Value expr) {
    final Value expr1 = unboxingConversion(expr);
    return expr1;
  }

  /**
   * 5.1.8 Unboxing Conversion.
   *
   * @param jiValue the Value to unbox
   * @return unboxed value or same
   */
  private Value unboxingConversion(final Value jiValue) {
    if (jiValue.getType() instanceof final JavaClassType jct) {
      final String unboxedNameOrNull = switch (jct.getFullyQualifiedName()) {
      case FQN_BOOLEAN -> FQN_BOOLEAN_TYPE;
      case FQN_BYTE -> FQN_BYTE_TYPE;
      case FQN_SHORT -> FQN_SHORT_TYPE;
      case FQN_CHARACTER -> FQN_CHAR_TYPE;
      case FQN_INTEGER -> FQN_INT_TYPE;
      case FQN_LONG -> FQN_LONG_TYPE;
      case FQN_FLOAT -> FQN_FLOAT_TYPE;
      case FQN_DOUBLE -> FQN_DOUBLE_TYPE;
      default -> null;
      };
      final Optional<String> unboxedName = Optional.ofNullable(unboxedNameOrNull);
      return unboxedName.map(ptn -> {
        final PrimitiveType pt = identifierFactory.getPrimitiveType(ptn).get();
        final MethodSignature method = identifierFactory.getMethodSignature(jct, ptn + "Value", pt,
            List.of());
        final JVirtualInvokeExpr jiInvokeExpr = Jimple
            .newVirtualInvokeExpr(makeLocal(jiValue, null), method);
        return (Value) jiInvokeExpr;
      }).orElse(jiValue);
    }
    return jiValue;
  }

  /**
   * 5.1.2 Widening Primitive Conversion.
   *
   * @param jiTargetType the target type to widen to
   * @param jiRight      the value to widen
   * @param ctElement
   * @return the widened value, or the same if the type is already aligned.
   */
  private Optional<Value> wideningConversion(final PrimitiveType jiTargetType, final Value jiRight,
      final CtElement ctElement) {
    if (jiTargetType.equals(jiRight.getType())) {
      return Optional.of(jiRight);
    }

    if (jiRight.getType() instanceof final PrimitiveType rightType) {
      Value widenedValue = null;
      switch (rightType.getName()) {
      case FQN_BYTE_TYPE:
        switch (jiTargetType.getName()) {
        case FQN_SHORT_TYPE:
        case FQN_INT_TYPE:
        case FQN_LONG_TYPE:
        case FQN_FLOAT_TYPE:
        case FQN_DOUBLE_TYPE:
          widenedValue = Jimple.newCastExpr(makeImmediate(jiRight, ctElement), jiTargetType);
          break;
        default:
          break;
        }
        break;
      case FQN_SHORT_TYPE:
        switch (jiTargetType.getName()) {
        case FQN_INT_TYPE:
        case FQN_LONG_TYPE:
        case FQN_FLOAT_TYPE:
        case FQN_DOUBLE_TYPE:
          widenedValue = Jimple.newCastExpr(makeImmediate(jiRight, ctElement), jiTargetType);
          break;
        default:
          break;
        }
        break;
      case FQN_CHAR_TYPE:
        switch (jiTargetType.getName()) {
        case FQN_INT_TYPE:
        case FQN_LONG_TYPE:
        case FQN_FLOAT_TYPE:
        case FQN_DOUBLE_TYPE:
          widenedValue = Jimple.newCastExpr(makeImmediate(jiRight, ctElement), jiTargetType);
          break;
        default:
          break;
        }
        break;
      case FQN_INT_TYPE:
        if (jiRight instanceof final IntConstant jiIntConstant) {
          switch (jiTargetType.getName()) {
          case FQN_LONG_TYPE:
            widenedValue = LongConstant.getInstance(jiIntConstant.getValue());
            break;
          case FQN_FLOAT_TYPE:
            widenedValue = FloatConstant.getInstance(jiIntConstant.getValue());
            break;
          case FQN_DOUBLE_TYPE:
            widenedValue = DoubleConstant.getInstance(jiIntConstant.getValue());
            break;
          default:
            break;
          }
        } else {
          switch (jiTargetType.getName()) {
          case FQN_LONG_TYPE:
          case FQN_FLOAT_TYPE:
          case FQN_DOUBLE_TYPE:
            widenedValue = Jimple.newCastExpr(makeImmediate(jiRight, ctElement), jiTargetType);
            break;
          default:
            break;
          }
        }
        break;
      case FQN_LONG_TYPE:
        if (jiRight instanceof final LongConstant jiLongConstant) {
          switch (jiTargetType.getName()) {
          case FQN_FLOAT_TYPE:
            widenedValue = FloatConstant.getInstance(jiLongConstant.getValue());
            break;
          case FQN_DOUBLE_TYPE:
            widenedValue = DoubleConstant.getInstance(jiLongConstant.getValue());
            break;
          default:
            break;
          }
        } else {
          switch (jiTargetType.getName()) {
          case FQN_FLOAT_TYPE:
          case FQN_DOUBLE_TYPE:
            widenedValue = Jimple.newCastExpr(makeImmediate(jiRight, ctElement), jiTargetType);
            break;
          default:
            break;
          }
        }
        break;
      case FQN_FLOAT_TYPE:
        if (jiRight instanceof final FloatConstant jiFloatConstant) {
          switch (jiTargetType.getName()) {
          case FQN_DOUBLE_TYPE:
            widenedValue = DoubleConstant.getInstance(jiFloatConstant.getValue());
            break;
          default:
            break;
          }
        } else {
          switch (jiTargetType.getName()) {
          case FQN_DOUBLE_TYPE:
            widenedValue = Jimple.newCastExpr(makeImmediate(jiRight, ctElement), jiTargetType);
            break;
          default:
            break;
          }
        }
        break;
      default:
        break;
      }
      return Optional.ofNullable(widenedValue);
    }
    return Optional.empty();
  }

  public Map<Stmt, CtElement> getStmtElementLink() {
    return Collections.unmodifiableMap(stmtElementLink);
  }
}
