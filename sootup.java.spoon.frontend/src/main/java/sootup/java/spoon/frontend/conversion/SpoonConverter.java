package sootup.java.spoon.frontend.conversion;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import sootup.core.jimple.Jimple;
import sootup.core.jimple.basic.NoPositionInformation;
import sootup.core.jimple.basic.SimpleStmtPositionInfo;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.expr.AbstractConditionExpr;
import sootup.core.jimple.common.expr.JEqExpr;
import sootup.core.jimple.common.expr.JGeExpr;
import sootup.core.jimple.common.expr.JGtExpr;
import sootup.core.jimple.common.expr.JLeExpr;
import sootup.core.jimple.common.expr.JLtExpr;
import sootup.core.jimple.common.expr.JNeExpr;
import sootup.core.model.FullPosition;
import sootup.core.model.Position;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;
import sootup.core.types.VoidType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.types.JavaClassType;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

public interface SpoonConverter {

  String FQN_OBJECT = "java.lang.Object";
  String FQN_ITERABLE = "java.lang.Iterable";
  String FQN_ITERATOR = "java.util.Iterator";
  String FQN_SYSTEM = "java.lang.System";
  String FQN_CLASS = "java.lang.Class";
  String FQN_BOOLEAN = "java.lang.Boolean";
  String FQN_BOOLEAN_TYPE = "boolean";
  String FQN_BYTE = "java.lang.Byte";
  String FQN_BYTE_TYPE = "byte";
  String FQN_CHARACTER = "java.lang.Character";
  String FQN_CHAR_TYPE = "char";
  String FQN_SHORT = "java.lang.Short";
  String FQN_SHORT_TYPE = "short";
  String FQN_INTEGER = "java.lang.Integer";
  String FQN_INT_TYPE = "int";
  String FQN_LONG = "java.lang.Long";
  String FQN_LONG_TYPE = "long";
  String FQN_FLOAT = "java.lang.Float";
  String FQN_FLOAT_TYPE = "float";
  String FQN_DOUBLE = "java.lang.Double";
  String FQN_DOUBLE_TYPE = "double";
  String FQN_STRING = "java.lang.String";
  String FQN_VOID = "java.lang.Void";
  String FQN_VOID_TYPE = "void";
  String FQN_ASSERTIONERROR = "java.lang.AssertionError";

  default String descriptor(final CtTypeReference<?> ctTr) {
    if (null == ctTr) {
      return null;
    }
    if (ctTr.isPrimitive()) {
      return switch (ctTr.getQualifiedName()) {
      case "int" -> "I";
      case "long" -> "J";
      case "float" -> "F";
      case "double" -> "D";
      case "boolean" -> "Z";
      case "byte" -> "B";
      case "char" -> "C";
      case "short" -> "S";
      case "void" -> "V";
      default -> throw new IllegalArgumentException("" + ctTr);
      };
    }
    if (ctTr instanceof final CtArrayTypeReference<?> ctArrayTr) {
      final StringBuilder sb = new StringBuilder("[".repeat(ctArrayTr.getDimensionCount()));
      return sb.append(descriptor(ctArrayTr.getArrayType())).toString();
    }
    return "L" + ctTr.getQualifiedName().replace('.', '/') + ";";
  }

  JavaIdentifierFactory identifierFactory();

  default AbstractConditionExpr negate(final AbstractConditionExpr expr) {
    return switch (expr) {
    case final JLtExpr _dummy -> Jimple.newGeExpr(expr.getOp1(), expr.getOp2());
    case final JLeExpr _dummy -> Jimple.newGtExpr(expr.getOp1(), expr.getOp2());
    case final JEqExpr _dummy -> Jimple.newNeExpr(expr.getOp1(), expr.getOp2());
    case final JGeExpr _dummy -> Jimple.newLtExpr(expr.getOp1(), expr.getOp2());
    case final JGtExpr _dummy -> Jimple.newLeExpr(expr.getOp1(), expr.getOp2());
    case final JNeExpr _dummy -> Jimple.newEqExpr(expr.getOp1(), expr.getOp2());
    default -> throw new IllegalArgumentException("" + expr);
    };
  }

  default ClassType stringType() {
    return identifierFactory().getClassType(FQN_STRING);
  }

  default MethodSignature signature(final Method testMethod) {
    final ClassType classType = toSootClassType(testMethod.getDeclaringClass().getName());
    return identifierFactory().getMethodSignature(classType, testMethod.getName(),
        toSootType(testMethod.getReturnType()),
        Stream.of(testMethod.getParameterTypes()).map(this::toSootType).toList());
  }

  default Type toSootType(final Class<?> c) {
    if (c.isArray()) {
      return Type.createArrayType(toSootType(c.componentType()), 1);
    }
    return switch (c.getName()) {
    case "void" -> VoidType.getInstance();
    case "boolean", "byte", "char", "short", "int", "long", "float", "double" ->
      identifierFactory().getPrimitiveType(c.getName()).get();
    default -> identifierFactory().getType(c.getName());
    };
  }

  static Position toPosition(final CtElement element) {
    return Optional.ofNullable(element).map(CtElement::getPosition)
        .filter(SourcePosition::isValidPosition)
        .map(position -> new FullPosition(position.getLine(), position.getColumn(),
            position.getEndLine(), position.getEndColumn()))
        .orElse(NoPositionInformation.getInstance());
  }
  
  default StmtPositionInfo toPositionInfo(final CtElement element) {
    return new SimpleStmtPositionInfo(toPosition(element));
  }

  default MethodSignature signature(final CtExecutableReference<?> ctExecutableReference) {
    final CtTypeReference<?> owner = ctExecutableReference.getDeclaringType();
    final boolean ctor = CtExecutableReference.CONSTRUCTOR_NAME
        .equals(ctExecutableReference.getSimpleName());
    final List<Type> parameterTypes = ctExecutableReference.getParameters().stream()
        .map(this::toSootType).toList();

    return identifierFactory().getMethodSignature(toSootClassType(owner),
        ctExecutableReference.getSimpleName(),
        ctor ? VoidType.getInstance() : toSootType(ctExecutableReference.getType()),
        parameterTypes);
  }

  default JavaClassType toBoxedType(final PrimitiveType pt) {
    final String boxedName = switch (pt.getName()) {
    case FQN_BOOLEAN_TYPE -> FQN_BOOLEAN;
    case FQN_BYTE_TYPE -> FQN_BYTE;
    case FQN_SHORT_TYPE -> FQN_SHORT;
    case FQN_CHAR_TYPE -> FQN_CHARACTER;
    case FQN_INT_TYPE -> FQN_INTEGER;
    case FQN_LONG_TYPE -> FQN_LONG;
    case FQN_FLOAT_TYPE -> FQN_FLOAT;
    case FQN_DOUBLE_TYPE -> FQN_DOUBLE;
    default -> throw new IllegalArgumentException("Cannot box " + pt);
    };
    return identifierFactory().getClassType(boxedName);
  }

  default JavaClassType toSootClassType(final CtType<?> ctT) {
    return switch (ctT) {
    case null -> null;
    // FIXME: isArray
    default -> identifierFactory().getClassType(ctT.getQualifiedName());
    };
  }

  default JavaClassType toSootClassType(final CtTypeReference<?> ctTr) {
    return switch (ctTr) {
    case null -> null;
    // case CtArrayTypeReference<?> ctATr ->
    // Type.createArrayType(toSootType(ctATr.getComponentType()), 1);
    default -> identifierFactory().getClassType(ctTr.getQualifiedName());
    };
  }

  default JavaClassType toSootClassType(final String qualifiedName) {
    return identifierFactory().getClassType(qualifiedName);
  }

  default Type toSootPrimitiveType(final CtTypeReference<?> ctPrimitiveTr) {
    return switch (ctPrimitiveTr.getQualifiedName()) {
    case "void" -> VoidType.getInstance();
    default -> identifierFactory().getPrimitiveType(ctPrimitiveTr.getQualifiedName()).get();
    };
  }

  default Type toSootType(final CtTypeReference<?> ctTr) {
    return switch (ctTr) {
    case null -> null;
    case final CtArrayTypeReference<?> ctArrayTr ->
      Type.createArrayType(toSootType(ctArrayTr.getComponentType()), 1);
    case final CtTypeReference<?> ctPrimitiveTr when ctPrimitiveTr.isPrimitive() ->
      toSootPrimitiveType(ctPrimitiveTr);
    default -> identifierFactory().getClassType(ctTr.getQualifiedName());
    };
  }
}
