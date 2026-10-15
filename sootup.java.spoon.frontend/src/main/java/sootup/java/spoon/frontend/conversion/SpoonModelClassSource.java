package sootup.java.spoon.frontend.conversion;

import java.io.File;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sootup.core.frontend.ResolveException;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.JimpleUtils;
import sootup.core.jimple.basic.NoPositionInformation;
import sootup.core.model.ClassModifier;
import sootup.core.model.FieldModifier;
import sootup.core.model.MethodModifier;
import sootup.core.model.Position;
import sootup.core.signatures.FieldSignature;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.types.Type;
import sootup.java.core.AnnotationUsage;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootClassSource;
import sootup.java.core.JavaSootField;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtParameterReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

/**
 * A @link {@link JavaSootClassSource} backed by a Spoon {@link CtType}.
 *
 * @author Keve Müller
 */
public class SpoonModelClassSource extends JavaSootClassSource implements SpoonConverter {

  private static final Logger LOG = LoggerFactory.getLogger(SpoonModelClassSource.class);

  private static Optional<Path> toPath(final CtType<?> ctType) {
    return Optional.ofNullable(ctType.getParent(CtCompilationUnit.class))
        .map(CtCompilationUnit::getFile).map(File::toPath);
  }

  private final @NonNull JavaView view;

  private final @NonNull JavaIdentifierFactory identifierFactory;

  private final @NonNull CtType<?> ctType;

  public SpoonModelClassSource(@NonNull final JavaView view,
      @NonNull final AnalysisInputLocation inputLocation, @NonNull final ClassType classSignature,
      @NonNull final CtType<?> ctType) {
    super(inputLocation, classSignature, SpoonModelClassSource.toPath(ctType).orElse(null));
    LOG.debug("SpoonModelClassSource:{}", ctType.getSimpleName());
    this.view = view;
    this.identifierFactory = view.getIdentifierFactory();
    this.ctType = ctType;
  }

  @Override
  public JavaIdentifierFactory identifierFactory() {
    return identifierFactory;
  }

  @Override
  public @NonNull Collection<JavaSootField> resolveFields() throws ResolveException {
    final List<JavaSootField> jiFields = new ArrayList<>();

    ctType.getFields().stream().map(ctField -> resolveField(ctField.getDeclaringType(), ctField))
        .forEachOrdered(jiFields::add);

    if (ctType instanceof final CtEnum<?> ctEnum) {
      final Factory factory = ctEnum.getFactory();
      final CtArrayTypeReference<?> ctValuesType = factory.Type().createArrayReference(ctEnum);
      final CtField<?> ctField = factory.Code().createCtField("ENUM$VALUES", ctValuesType, null,
          ModifierKind.PRIVATE, ModifierKind.STATIC, ModifierKind.FINAL);
      ctField.setImplicit(true);
      jiFields.add(resolveField(ctEnum, ctField));
    }

    // synthesize the outer class this accessor
    final OptionalInt nesting = nestingLevel(ctType);
    if (nesting.isPresent()) {
      final CtType<?> outer = ctType.getDeclaringType();
      final JavaClassType jiType = toSootClassType(ctType);
      final Type jiFieldType = toSootType(outer.getReference());
      final FieldSignature jiFieldSignature = identifierFactory
          .getFieldSignature("this$" + (nesting.getAsInt() - 1), jiType, jiFieldType);

      final EnumSet<FieldModifier> modifier = EnumSet.of(FieldModifier.FINAL,
          FieldModifier.SYNTHETIC);

      final JavaSootField jiThis0Field = new JavaSootField(jiFieldSignature, modifier, List.of(),
          NoPositionInformation.getInstance());
      jiFields.add(jiThis0Field);
    }
    return jiFields;
  }

  /*
   * In SootUp the assumption is that iterating the list of types, its methods and
   * fields is a cheap operation. This trivially true for classfiles, as they have
   * all this information available. For Spoon this is only simple for
   * non-synthetic type members. Synthetic members, that are added by the compiler
   * are not readily available in the Spoon model. There are two possible
   * approaches: 1/ Ex-ante synthesize the type members when the view is created.
   * E.g. blow up CtEnum with the additional fields that are expected to be there.
   * 2/ Only synthesize the SootUp signatures and lazily create the missing type
   * members when they are accessed.
   *
   * 1/ has the clear disadvantage that it does not fit into a dynamically
   * changing CtModel, it would require a lot of ModelListeners to be plugged to
   * keep the synthetics in place. it also suffers from an upfront penalty of
   * creating type members that might never be needed for the SootUp analysis.
   *
   * 2/ is not very straightforward, as it requires "guessing" the missing pieces
   * and the work is performed at the wrong place (i.e. here).
   *
   * Once this code matures over PoC, the right choice will emerge as being
   * obvious.
   */

  @Override
  public @NonNull Set<JavaClassType> resolveInterfaces() {
    return ctType.getSuperInterfaces().stream().map(this::toSootClassType)
        .collect(Collectors.toSet());
  }

  @Override
  public @NonNull Collection<SpoonModelExecutable> resolveMethods() throws ResolveException {
    final List<SpoonModelExecutable> jiMethods = new ArrayList<>();
    final AtomicBoolean addClInit = new AtomicBoolean(false);

    ctType.accept(new CtScanner() {
      @Override
      public <T> void visitCtClass(final CtClass<T> ctClass) {
//        super.visitCtClass(ctClass);
        doCtClass(ctClass);
      }

      @Override
      public <T extends Enum<?>> void visitCtEnum(final CtEnum<T> ctEnum) {
//        super.visitCtEnum(ctEnum);
        // Spoon/JDT creates a no-parameter implicit constructor, which needs to be
        // folded into our actual Constructor
        final CtConstructor<T> fakeConstructor = ctEnum.getConstructor();
        if (null != fakeConstructor && fakeConstructor.isImplicit()) {
          fakeConstructor.delete();
        }

        doCtClass(ctEnum);

        final Factory factory = ctEnum.getFactory();

        // not in getMethods(), but hacked into getAllMethods()
        // public static final CtEnum valueOf(String arg0);
        final CtMethod<T> valueOfMethod = ctEnum.getMethod("valueOf", factory.Type().stringType());
        if (null == valueOfMethod.getBody()) {
          final CtTypeReference<?> enumClass = ctEnum.getSuperclass();
          final CtExecutableReference<?> superValueOfMethod = factory.Executable().createReference(
              enumClass, enumClass, "valueOf", factory.Type().get(Class.class).getReference(),
              factory.Type().stringType());

          final CtParameterReference<?> param0 = valueOfMethod.getParameters().get(0)
              .getReference();
          final CtTypeAccess<?> target = factory.Code().createTypeAccess(enumClass);
          final CtVariableAccess<?> arg0 = factory.Code().createVariableRead(param0, false);
          final CtFieldAccess<Class<T>> thisClass = factory.Code()
              .createClassAccess(ctEnum.getReference());
          final CtInvocation<?> inv = factory.Code()
              .createInvocation(target, superValueOfMethod, thisClass, arg0)
              .addTypeCast(ctEnum.getReference());
          final CtReturn<?> ctReturn = factory.Code().createCtReturn(inv);

          valueOfMethod.setBody(factory.Code().createCtBlock(ctReturn));
        }
        jiMethods.add(resolveExecutable(valueOfMethod));

        // public static final CtEnum[] values();
        final CtMethod<T> valuesMethod = ctEnum.getMethod("values");
        if (null == valuesMethod.getBody()) {
          final CtBlock<?> block = factory.Core().createBlock();
          final CtArrayTypeReference<T[]> enumArrayType = factory.Type()
              .createArrayReference(ctEnum);
          final CtFieldReference<T[]> valuesFieldReference = factory.Core().createFieldReference()
              .setDeclaringType(ctEnum.getReference()).setType(enumArrayType.clone())
              .setSimpleName("ENUM$VALUES");

          final CtTypeAccess<?> enumTarget = factory.Code().createTypeAccess(ctEnum.getReference());
          final CtFieldRead<T[]> valuesFieldRead = factory.Core().createFieldRead()
              .setTarget(enumTarget).<CtFieldRead<T[]>>setType(enumArrayType.clone())
              .setVariable(valuesFieldReference);

          final CtLocalVariable<T[]> l0 = factory.Code().createLocalVariable(enumArrayType.clone(),
              "l0", valuesFieldRead);
          block.addStatement(l0);

          final CtFieldReference<Integer> valuesFieldLength = factory.Core().createFieldReference()
              .setDeclaringType(enumArrayType.clone())
              .setType(factory.Type().integerPrimitiveType()).setSimpleName("length");

          final CtFieldRead<Integer> valuesFieldLengthRead = factory.Core().createFieldRead()
              .setTarget(factory.Code().createVariableRead(l0.getReference(), false))
              .<CtFieldRead<Integer>>setType(factory.Type().integerPrimitiveType())
              .setVariable(valuesFieldLength);

          final CtLocalVariable<Integer> l1 = factory.Code().createLocalVariable(
              factory.Type().integerPrimitiveType(), "l1", valuesFieldLengthRead);
          block.addStatement(l1);

          final CtNewArray<T[]> newArray = factory.Core().createNewArray()
              .<CtNewArray<T[]>>setType(enumArrayType.clone())
              .addDimensionExpression(factory.Code().createVariableRead(l1.getReference(), false));
          final CtLocalVariable<T[]> l2 = factory.Code().createLocalVariable(enumArrayType.clone(),
              "l2", newArray);
          block.addStatement(l2);
          final CtTypeReference<?> ctSystemClass = factory.Type().createReference(FQN_SYSTEM);
          final CtTypeAccess<?> target = factory.Code().createTypeAccess(ctSystemClass);
          final CtExecutableReference<?> ctSystemArraycopy = factory.Executable().createReference(
              ctSystemClass, true, factory.Type().voidPrimitiveType(), "arraycopy",
              factory.Type().createReference(FQN_OBJECT), factory.Type().integerPrimitiveType(),
              factory.Type().createReference(FQN_OBJECT), factory.Type().integerPrimitiveType(),
              factory.Type().integerPrimitiveType());

          final CtInvocation<?> inv = factory.Code().createInvocation(target, ctSystemArraycopy,
              factory.Code().createVariableRead(l0.getReference(), false),
              factory.Code().createLiteral(0),
              factory.Code().createVariableRead(l2.getReference(), false),
              factory.Code().createLiteral(0),
              factory.Code().createVariableRead(l1.getReference(), false));
          block.addStatement(inv);
          block.addStatement(
              factory.createCtReturn(factory.Code().createVariableRead(l2.getReference(), false)));
          valuesMethod.setBody(block);
        }
        jiMethods.add(resolveExecutable(valuesMethod));

        // CtEnum <init>(String arg0, int arg1);
        final CtConstructor<T> initMethod = factory.Core().createConstructor();
        ctEnum.addConstructor(initMethod); // CAVE: modifies the class
        initMethod.setType(ctEnum.getReference());
        final List<CtParameter<?>> parameters = List.of(
            factory.Core().createParameter().<CtParameter<?>>setType(factory.Type().stringType())
                .setSimpleName("arg0"),
            factory.Core().createParameter()
                .<CtParameter<?>>setType(factory.Type().integerPrimitiveType())
                .setSimpleName("arg1"));
        initMethod.setModifiers(EnumSet.of(ModifierKind.PRIVATE));
        initMethod.setParameters(parameters);
        initMethod.setImplicit(true);

        final CtTypeReference<?> ctSuper = ctEnum.getSuperclass();

        final CtExecutableReference<?> ctSuperConstructor = factory.Executable().createReference(
            ctSuper, ctSuper, CtExecutableReference.CONSTRUCTOR_NAME, factory.Type().stringType(),
            factory.Type().integerPrimitiveType());

        final CtVariableAccess<?> arg0 = factory.Code()
            .createVariableRead(parameters.get(0).getReference(), false);
        final CtVariableAccess<?> arg1 = factory.Code()
            .createVariableRead(parameters.get(1).getReference(), false);
        final CtInvocation<?> inv = factory.Code().createInvocation(null, ctSuperConstructor, arg0,
            arg1);
        initMethod.setBody(factory.Code().createCtBlock(inv));
        jiMethods.add(resolveExecutable(initMethod));
      }

      @Override
      public <T> void visitCtInterface(final CtInterface<T> ctInterface) {
//        super.visitCtInterface(ctInterface);
        ctInterface.getMethods().forEach(x -> jiMethods.add(resolveExecutable(x)));
      }

      @Override
      public void visitCtRecord(final CtRecord ctRecord) {
//        super.visitCtRecord(ctRecord);
        doCtClass(ctRecord);
      }

      private <T> void doCtClass(final CtClass<T> ctClass) {
        ctClass.getAnonymousExecutables().forEach(x -> {
          jiMethods.add(resolveExecutable(x));
          addClInit.set(true);
        });
        ctClass.getConstructors().stream().forEach(x -> jiMethods.add(resolveExecutable(x)));
        ctClass.getMethods().forEach(x -> jiMethods.add(resolveExecutable(x)));
        ctClass.getFields().forEach(f -> {
          if (null != f.getDefaultExpression() && (!f.getType().isPrimitive() || !f.isFinal())) {
            addClInit.set(true);
          }
        });
        // uses assert -> $assertionDisabled
        // Outer$1
      }
    });

    if (addClInit.get()) {
      // void <clinit>();
      final Factory factory = ctType.getFactory();
      final CtMethod<?> clInitMethod = factory.Core().createMethod();
      clInitMethod.setModifiers(EnumSet.of(ModifierKind.STATIC));
      clInitMethod.setType(factory.Type().voidPrimitiveType());
      clInitMethod.setSimpleName("<clinit>");
      clInitMethod.setImplicit(true);
      jiMethods.add(resolveExecutable(clInitMethod));
    }
    return jiMethods;
  }

  @Override
  public @NonNull Set<ClassModifier> resolveModifiers() {
    // TODO: synthesize internal modifiers.
    // TODO: remove redundant modifiers
    final EnumSet<ClassModifier> jiModifiers = EnumSet.noneOf(ClassModifier.class);
    ctType.getModifiers().forEach(m -> jiModifiers.add(mapClassModifier(m)));
    return jiModifiers;
  }

  @Override
  public @NonNull Optional<JavaClassType> resolveOuterClass() {
    return Optional.ofNullable(toSootClassType(ctType.getDeclaringType()));
  }

  @Override
  public @NonNull Position resolvePosition() {
    return SpoonConverter.toPosition(ctType);
  }

  @Override
  public @NonNull Optional<JavaClassType> resolveSuperclass() {
    return Optional.ofNullable(ctType.getSuperclass()).map(this::toSootClassType)
        .or(() -> Optional.of(identifierFactory.getClassType(FQN_OBJECT)));
  }

  @Override
  protected Iterable<AnnotationUsage> resolveAnnotations() {
    return mapAnnotations(ctType);
  }

  private List<AnnotationUsage> mapAnnotations(final CtElement ctElement) {
    if (ctElement.getAnnotations().isEmpty()) {
      return List.of();
    }
    final List<AnnotationUsage> annotations = new ArrayList<>();
    for (final CtAnnotation<? extends Annotation> ctAnnotation : ctElement.getAnnotations()) {
      final JavaClassType jiAnnotationType = toSootClassType(ctAnnotation.getAnnotationType());
      final Map<String, Object> jiAnnotationValues = new LinkedHashMap<>();
      ctAnnotation.getValues().forEach((k, v) -> jiAnnotationValues.put(k, v.toString()));
      annotations.add(new AnnotationUsage(jiAnnotationType, jiAnnotationValues));
    }
    return annotations;
  }

  private ClassModifier mapClassModifier(final ModifierKind ctModifier) {
    return switch (ctModifier) {
    case ABSTRACT -> ClassModifier.ABSTRACT;
    case PRIVATE -> ClassModifier.PRIVATE;
    case PROTECTED -> ClassModifier.PROTECTED;
    case PUBLIC -> ClassModifier.PUBLIC;
    case STATIC -> ClassModifier.STATIC;
    case FINAL -> ClassModifier.FINAL;
    default -> throw new IllegalArgumentException("" + ctModifier);
    };
  }

  private OptionalInt nestingLevel(final CtType<?> innerType) {
    if (innerType.isStatic() || innerType.isInterface() || null == innerType.getDeclaringType()) {
      return OptionalInt.empty();
    }
    final OptionalInt parentNesting = nestingLevel(innerType.getDeclaringType());
    return OptionalInt.of(1 + parentNesting.orElse(0));
  }

  private SpoonModelExecutable resolveExecutable(final CtExecutable<?> ctExecutable) {
    LOG.trace("resolveExecutable//{} -> {}", ctExecutable.getSimpleName(), ctExecutable.getType());

    final CtTypeReference<?> ctType;
    final String methodname = ctExecutable.getSimpleName();

    final List<Type> params = ctExecutable.getParameters().stream().map(CtParameter::getType)
        .map(this::toSootType).collect(Collectors.toList());

    if (ctExecutable instanceof CtConstructor<?>) {
      ctType = ctExecutable.getFactory().Type().voidPrimitiveType();
    } else {
      ctType = ctExecutable.getType();
    }

    final Type type = toSootType(ctType);

    final MethodSignature methodSignature = identifierFactory.getMethodSignature(classSignature,
        JimpleUtils.unescape(methodname), type, params);

    final EnumSet<MethodModifier> modifier = EnumSet.noneOf(MethodModifier.class);
    if (ctExecutable instanceof final CtModifiable ctModifiable) {
      ctModifiable.getModifiers().forEach(m -> modifier.add(SpoonModelUtil.mapMethodModifier(m)));
    }

    final List<ClassType> exceptions = null == ctExecutable.getThrownTypes() ? List.of()
        : ctExecutable.getThrownTypes().stream().map(this::toSootClassType)
            .map(ClassType.class::cast).toList();

    final Position methodPosition = SpoonConverter.toPosition(ctExecutable);

    final LazySpoonExecutableBodySource source = LazySpoonExecutableBodySource.of(view,
        analysisInputLocation.getBodyInterceptors(), methodSignature, ctExecutable);

    final List<AnnotationUsage> annotations = mapAnnotations(ctExecutable);
    return new SpoonModelExecutable(source, methodSignature, modifier, exceptions, annotations,
        methodPosition);
  }

  private JavaSootField resolveField(final CtType<?> ctDeclaringType, final CtField<?> ctField) {
    final EnumSet<FieldModifier> modifier = EnumSet.noneOf(FieldModifier.class);
    ctField.getModifiers().forEach(m -> modifier.add(SpoonModelUtil.mapFieldModifier(m)));
    if (ctField.isImplicit()) {
      modifier.add(FieldModifier.SYNTHETIC);
    } else if (ctDeclaringType.isEnum()) {
      modifier.add(FieldModifier.ENUM);
    }
    final List<AnnotationUsage> annotations = mapAnnotations(ctField);

    final JavaClassType jiType = toSootClassType(ctDeclaringType);
    final Type jiFieldType = toSootType(ctField.getType());
    final FieldSignature jiFieldSignature = identifierFactory
        .getFieldSignature(ctField.getSimpleName(), jiType, jiFieldType);
    return new JavaSootField(jiFieldSignature, modifier, annotations,
        NoPositionInformation.getInstance());
  }
}
