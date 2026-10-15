package sootup.java.spoon.frontend.util;

import java.util.List;

import org.jspecify.annotations.NonNull;

import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.Value;
import sootup.core.jimple.common.constant.BooleanConstant;
import sootup.core.jimple.common.constant.ClassConstant;
import sootup.core.jimple.common.constant.Constant;
import sootup.core.jimple.common.constant.DoubleConstant;
import sootup.core.jimple.common.constant.EnumConstant;
import sootup.core.jimple.common.constant.FloatConstant;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.constant.LongConstant;
import sootup.core.jimple.common.constant.MethodHandle;
import sootup.core.jimple.common.constant.MethodType;
import sootup.core.jimple.common.constant.NullConstant;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.Expr;
import sootup.core.jimple.common.expr.JAddExpr;
import sootup.core.jimple.common.expr.JAndExpr;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.expr.JCmpExpr;
import sootup.core.jimple.common.expr.JCmpgExpr;
import sootup.core.jimple.common.expr.JCmplExpr;
import sootup.core.jimple.common.expr.JDivExpr;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;
import sootup.core.jimple.common.expr.JEqExpr;
import sootup.core.jimple.common.expr.JGeExpr;
import sootup.core.jimple.common.expr.JGtExpr;
import sootup.core.jimple.common.expr.JInstanceOfExpr;
import sootup.core.jimple.common.expr.JInterfaceInvokeExpr;
import sootup.core.jimple.common.expr.JLeExpr;
import sootup.core.jimple.common.expr.JLengthExpr;
import sootup.core.jimple.common.expr.JLtExpr;
import sootup.core.jimple.common.expr.JMulExpr;
import sootup.core.jimple.common.expr.JNeExpr;
import sootup.core.jimple.common.expr.JNegExpr;
import sootup.core.jimple.common.expr.JNewArrayExpr;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.expr.JNewMultiArrayExpr;
import sootup.core.jimple.common.expr.JOrExpr;
import sootup.core.jimple.common.expr.JPhiExpr;
import sootup.core.jimple.common.expr.JRemExpr;
import sootup.core.jimple.common.expr.JShlExpr;
import sootup.core.jimple.common.expr.JShrExpr;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.expr.JSubExpr;
import sootup.core.jimple.common.expr.JUshrExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.expr.JXorExpr;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.jimple.common.ref.JThisRef;
import sootup.core.jimple.common.ref.Ref;
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
import sootup.core.jimple.javabytecode.stmt.JBreakpointStmt;
import sootup.core.jimple.javabytecode.stmt.JEnterMonitorStmt;
import sootup.core.jimple.javabytecode.stmt.JExitMonitorStmt;
import sootup.core.jimple.javabytecode.stmt.JRetStmt;
import sootup.core.jimple.javabytecode.stmt.JSwitchStmt;
import sootup.core.jimple.visitor.Acceptor;
import sootup.core.jimple.visitor.StmtVisitor;
import sootup.core.jimple.visitor.TypeVisitor;
import sootup.core.jimple.visitor.ValueVisitor;
import sootup.core.types.ArrayType;
import sootup.core.types.ClassType;
import sootup.core.types.NullType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.PrimitiveType.BooleanType;
import sootup.core.types.PrimitiveType.ByteType;
import sootup.core.types.PrimitiveType.CharType;
import sootup.core.types.PrimitiveType.DoubleType;
import sootup.core.types.PrimitiveType.FloatType;
import sootup.core.types.PrimitiveType.IntType;
import sootup.core.types.PrimitiveType.LongType;
import sootup.core.types.PrimitiveType.ShortType;
import sootup.core.types.Type;
import sootup.core.types.UnknownType;
import sootup.core.types.VoidType;

public class JimpleScanner implements StmtVisitor, ValueVisitor, TypeVisitor {

  @Override
  public void caseAddExpr(final JAddExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseAndExpr(final JAndExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseArrayRef(final JArrayRef ref) {
    enter(ref);
    scan(ref.getType());
    scan(ref.getBase());
    scan(ref.getIndex());
    exit(ref);
  }

  @Override
  public void caseArrayType(@NonNull final ArrayType arrayType) {
    enter(arrayType);
    scan(arrayType.getElementType());
    exit(arrayType);
  }

  @Override
  public void caseAssignStmt(final JAssignStmt stmt) {
    enter(stmt);
    scan(stmt.getType());
    scan(stmt.getLeftOp());
    scan(stmt.getRightOp());
    exit(stmt);
  }

  @Override
  public void caseBooleanConstant(@NonNull final BooleanConstant constant) {
    enter(constant);
    scan(constant.getType());
    exit(constant);
  }

  @Override
  public void caseBooleanType() {
    final BooleanType booleanType = PrimitiveType.BooleanType.getInstance();
    enter(booleanType);
    exit(booleanType);
  }

  @Override
  public void caseBreakpointStmt(final JBreakpointStmt stmt) {
    enter(stmt);
    exit(stmt);
  }

  @Override
  public void caseByteType() {
    final ByteType byteType = PrimitiveType.ByteType.getInstance();
    enter(byteType);
    exit(byteType);
  }

  @Override
  public void caseCastExpr(final JCastExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp());
    exit(expr);
  }

  @Override
  public void caseCaughtExceptionRef(final JCaughtExceptionRef ref) {
    enter(ref);
    scan(ref.getType());
    exit(ref);
  }

  @Override
  public void caseCharType() {
    final CharType charType = PrimitiveType.CharType.getInstance();
    enter(charType);
    exit(charType);
  }

  @Override
  public void caseClassConstant(@NonNull final ClassConstant constant) {
    enter(constant);
    scan(constant.getType());
    exit(constant);
  }

  @Override
  public void caseClassType(@NonNull final ClassType classType) {
    enter(classType);
    exit(classType);
  }

  @Override
  public void caseCmpExpr(final JCmpExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseCmpgExpr(final JCmpgExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseCmplExpr(final JCmplExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseDivExpr(final JDivExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseDoubleConstant(@NonNull final DoubleConstant constant) {
    enter(constant);
    scan(constant.getType());
    exit(constant);
  }

  @Override
  public void caseDoubleType() {
    final DoubleType doubleType = PrimitiveType.DoubleType.getInstance();
    enter(doubleType);
    exit(doubleType);
  }

  @Override
  public void caseDynamicInvokeExpr(final JDynamicInvokeExpr expr) {
    enter(expr);
    scan(expr.getType());
    scanValues(expr.getBootstrapArgs());
    scanValues(expr.getArgs());
    exit(expr);
  }

  @Override
  public void caseEnterMonitorStmt(final JEnterMonitorStmt stmt) {
    enter(stmt);
    scan(stmt.getOp());
    exit(stmt);
  }

  @Override
  public void caseEnumConstant(@NonNull final EnumConstant constant) {
    enter(constant);
    scan(constant.getType());
    exit(constant);
  }

  @Override
  public void caseEqExpr(final JEqExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseExitMonitorStmt(final JExitMonitorStmt stmt) {
    enter(stmt);
    scan(stmt.getOp());
    exit(stmt);
  }

  @Override
  public void caseFloatConstant(@NonNull final FloatConstant constant) {
    enter(constant);
    scan(constant.getType());
    exit(constant);
  }

  @Override
  public void caseFloatType() {
    final FloatType floatType = PrimitiveType.FloatType.getInstance();
    enter(floatType);
    exit(floatType);
  }

  @Override
  public void caseGeExpr(final JGeExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseGotoStmt(final JGotoStmt stmt) {
    enter(stmt);
    exit(stmt);
  }

  @Override
  public void caseGtExpr(final JGtExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseIdentityStmt(final JIdentityStmt stmt) {
    enter(stmt);
    scan(stmt.getType());
    scan(stmt.getLeftOp());
    scan(stmt.getRightOp());
    exit(stmt);
  }

  @Override
  public void caseIfStmt(final JIfStmt stmt) {
    enter(stmt);
    scan(stmt.getCondition());
    exit(stmt);
  }

  @Override
  public void caseInstanceFieldRef(final JInstanceFieldRef ref) {
    enter(ref);
    scan(ref.getType());
    scan(ref.getBase());
    exit(ref);
  }

  @Override
  public void caseInstanceOfExpr(final JInstanceOfExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getCheckType());
    scan(expr.getOp());
    exit(expr);
  }

  @Override
  public void caseIntConstant(@NonNull final IntConstant constant) {
    enter(constant);
    scan(constant.getType());
    exit(constant);
  }

  @Override
  public void caseInterfaceInvokeExpr(final JInterfaceInvokeExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getBase());
    scanValues(expr.getArgs());
    exit(expr);
  }

  @Override
  public void caseIntType() {
    final IntType intType = PrimitiveType.IntType.getInstance();
    enter(intType);
    exit(intType);
  }

  @Override
  public void caseInvokeStmt(final JInvokeStmt stmt) {
    enter(stmt);
    stmt.getInvokeExpr().ifPresent(this::scan);
    exit(stmt);
  }

  @Override
  public void caseLeExpr(final JLeExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseLengthExpr(final JLengthExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp());
    exit(expr);
  }

  @Override
  public void caseLocal(@NonNull final Local local) {
    enter(local);
    scan(local.getType());
    exit(local);
  }

  @Override
  public void caseLongConstant(@NonNull final LongConstant constant) {
    enter(constant);
    scan(constant.getType());
    exit(constant);
  }

  @Override
  public void caseLongType() {
    final LongType longType = PrimitiveType.LongType.getInstance();
    enter(longType);
    exit(longType);
  }

  @Override
  public void caseLtExpr(final JLtExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseMethodHandle(@NonNull final MethodHandle handle) {
    enter(handle);
    scan(handle.getType());
    exit(handle);
  }

  @Override
  public void caseMethodType(@NonNull final MethodType methodType) {
    enter(methodType);
    scan(methodType.getType());
    scan(methodType.getReturnType());
    scanTypes(methodType.getParameterTypes());
    exit(methodType);
  }

  @Override
  public void caseMulExpr(final JMulExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseNeExpr(final JNeExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseNegExpr(final JNegExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp());
    exit(expr);
  }

  @Override
  public void caseNewArrayExpr(final JNewArrayExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getBaseType());
    scan(expr.getSize());
    exit(expr);
  }

  @Override
  public void caseNewExpr(final JNewExpr expr) {
    enter(expr);
    scan(expr.getType());
    exit(expr);
  }

  @Override
  public void caseNewMultiArrayExpr(final JNewMultiArrayExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getBaseType());
    scanValues(expr.getSizes());
    exit(expr);
  }

  @Override
  public void caseNopStmt(final JNopStmt stmt) {
    enter(stmt);
    exit(stmt);
  }

  @Override
  public void caseNullConstant(@NonNull final NullConstant constant) {
    enter(constant);
    scan(constant.getType());
    exit(constant);
  }

  @Override
  public void caseNullType() {
    final @NonNull NullType nullType = NullType.getInstance();
    enter(nullType);
    exit(nullType);
  }

  @Override
  public void caseOrExpr(final JOrExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseParameterRef(final JParameterRef ref) {
    enter(ref);
    scan(ref.getType());
    exit(ref);
  }

  @Override
  public void casePhiExpr(final JPhiExpr v) {
    enter(v);
    scan(v.getType());
    scanValues(v.getArgs());
    exit(v);
  }

  @Override
  public void caseRemExpr(final JRemExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseRetStmt(final JRetStmt stmt) {
    enter(stmt);
    scan(stmt.getStmtAddress());
    exit(stmt);
  }

  @Override
  public void caseReturnStmt(final JReturnStmt stmt) {
    enter(stmt);
    scan(stmt.getOp());
    exit(stmt);
  }

  @Override
  public void caseReturnVoidStmt(final JReturnVoidStmt stmt) {
    enter(stmt);
    exit(stmt);
  }

  @Override
  public void caseShlExpr(final JShlExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseShortType() {
    final ShortType shortType = PrimitiveType.ShortType.getInstance();
    enter(shortType);
    exit(shortType);
  }

  @Override
  public void caseShrExpr(final JShrExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseSpecialInvokeExpr(final JSpecialInvokeExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getBase());
    scanValues(expr.getArgs());
    exit(expr);
  }

  @Override
  public void caseStaticFieldRef(final JStaticFieldRef ref) {
    enter(ref);
    scan(ref.getType());
    exit(ref);
  }

  @Override
  public void caseStaticInvokeExpr(final JStaticInvokeExpr expr) {
    enter(expr);
    scan(expr.getType());
    scanValues(expr.getArgs());
    exit(expr);
  }

  @Override
  public void caseStringConstant(@NonNull final StringConstant constant) {
    enter(constant);
    scan(constant.getType());
    exit(constant);
  }

  @Override
  public void caseSubExpr(final JSubExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseSwitchStmt(final JSwitchStmt stmt) {
    enter(stmt);
    scan(stmt.getKey());
    scanValues(stmt.getValues());
    exit(stmt);
  }

  @Override
  public void caseThisRef(final JThisRef ref) {
    enter(ref);
    scan(ref.getType());
    exit(ref);
  }

  @Override
  public void caseThrowStmt(final JThrowStmt stmt) {
    enter(stmt);
    scan(stmt.getOp());
    exit(stmt);
  }

  @Override
  public void caseUnknownType() {
    final @NonNull UnknownType unknownType = UnknownType.getInstance();
    enter(unknownType);
    exit(unknownType);
  }

  @Override
  public void caseUshrExpr(final JUshrExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void caseVirtualInvokeExpr(final JVirtualInvokeExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getBase());
    scanValues(expr.getArgs());
    exit(expr);
  }

  @Override
  public void caseVoidType() {
    final @NonNull VoidType voidType = VoidType.getInstance();
    enter(voidType);
    exit(voidType);
  }

  @Override
  public void caseXorExpr(final JXorExpr expr) {
    enter(expr);
    scan(expr.getType());
    scan(expr.getOp1());
    scan(expr.getOp2());
    exit(expr);
  }

  @Override
  public void defaultCaseConstant(@NonNull final Constant constant) {
    enter(constant);
    scan(constant.getType());
    exit(constant);
  }

  @Override
  public void defaultCaseExpr(final Expr expr) {
    enter(expr);
    scan(expr.getType());
    exit(expr);
  }

  @Override
  public void defaultCaseRef(final Ref ref) {
    enter(ref);
    scan(ref.getType());
    exit(ref);
  }

  @Override
  public void defaultCaseStmt(final Stmt stmt) {
    enter(stmt);
    exit(stmt);
  }

  @Override
  public void defaultCaseType() {
  }

  @Override
  public void defaultCaseValue(@NonNull final Value v) {
    // see https://github.com/soot-oss/SootUp/issues/1784
    // enterValue(v);
    scan(v.getType());
    // exitValue(v);
  }

  public void scan(final List<? extends Stmt> stmts) {
    for (final Stmt stmt : stmts) {
      scan(stmt);
    }
  }

  public void scanValues(final List<? extends Value> values) {
    for (final Value value : values) {
      scan(value);
    }
  }

  protected void enter(final Acceptor<?> acceptor) {
  }

  protected void exit(final Acceptor<?> acceptor) {
  }

  protected void scan(final Value op) {
    op.accept(this);
  }

  private void scan(final Stmt stmt) {
    stmt.accept(this);
  }

  private void scan(final Type type) {
    type.accept(this);
  }

  private void scanTypes(final List<? extends Type> types) {
    for (final Type type : types) {
      scan(type);
    }
  }
}
