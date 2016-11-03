package eu.toolchain.condo;

import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@AutoService(Processor.class)
public class CondoProcessor extends AbstractProcessor {
  public static final String AUTO_CONDO = AutoCondo.class.getCanonicalName();
  public static final String IMPLEMENTATION_NAME_FORMAT = "{0}_Condo";
  public static final String METADATA_NAME_FORMAT = "{0}Metadata";

  public static final String GENERATED_PACKAGE = Generated.class.getPackage().getName();
  public static final String GENERATED = Generated.class.getSimpleName();
  public static final String OVERRIDE_PACKAGE = Override.class.getPackage().getName();
  public static final String OVERRIDE = Override.class.getSimpleName();

  public static final String CONDO_PROCESSOR = CondoProcessor.class.getCanonicalName();

  public static final Joiner PARAMETER_JOINER = Joiner.on(", ");

  public static final Converter<String, String> METADATA_TYPE_CONVERTER =
      CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL);

  private Filer filer;
  private Elements elements;
  private Types types;
  private Messager messager;

  @Override
  public void init(final ProcessingEnvironment env) {
    filer = env.getFiler();
    elements = env.getElementUtils();
    types = env.getTypeUtils();
    messager = env.getMessager();
  }

  @Override
  public boolean process(
      final Set<? extends TypeElement> annotations, final RoundEnvironment env
  ) {
    final List<JavaFile> files = new ArrayList<>();

    final Set<? extends Element> elements =
        env.getElementsAnnotatedWith(this.elements.getTypeElement(AUTO_CONDO));

    for (final Element element : elements) {
      if (!(element instanceof TypeElement)) {
        messager.printMessage(Diagnostic.Kind.ERROR,
            String.format("Must be interface: %s", element));
        continue;
      }

      final TypeElement typeElement = (TypeElement) element;

      if (typeElement.getKind() != ElementKind.INTERFACE) {
        messager.printMessage(Diagnostic.Kind.ERROR,
            String.format("Must be interface: %s", element));
        continue;
      }

      files.add(processImpl(typeElement));
      files.add(processMetadata(typeElement));
    }

    for (final JavaFile file : files) {
      try {
        file.writeTo(filer);
      } catch (final IOException e) {
        messager.printMessage(Diagnostic.Kind.ERROR,
            String.format("Could not write file: %s: %s", file, e.getMessage()));
      }
    }

    return true;
  }

  private JavaFile processImpl(final TypeElement typeElement) {
    final TypeElement completableFutureType =
        elements.getTypeElement(CompletableFuture.class.getCanonicalName());

    final String packageName = elements.getPackageOf(typeElement).getQualifiedName().toString();
    final String className =
        MessageFormat.format(IMPLEMENTATION_NAME_FORMAT, typeElement.getSimpleName());

    final TypeName interfaceType = TypeName.get(typeElement.asType());

    final ClassName condo = ClassName.get(Condo.class);

    final AnnotationSpec overrideAnnotation =
        AnnotationSpec.builder(ClassName.get(OVERRIDE_PACKAGE, OVERRIDE)).build();

    final ClassName metadata = ClassName.get(packageName,
        MessageFormat.format(METADATA_NAME_FORMAT, typeElement.getSimpleName()));

    final AnnotationSpec generatedAnnotation = AnnotationSpec
        .builder(ClassName.get(GENERATED_PACKAGE, GENERATED))
        .addMember("value", "$S", CONDO_PROCESSOR)
        .build();

    final FieldSpec condoField = FieldSpec
        .builder(ParameterizedTypeName.get(condo, metadata), "condo", Modifier.PRIVATE,
            Modifier.FINAL)
        .build();

    final FieldSpec delegateField =
        FieldSpec.builder(interfaceType, "delegate", Modifier.PRIVATE, Modifier.FINAL).build();

    final TypeSpec.Builder typeSpec = TypeSpec.classBuilder(className);

    typeSpec.addAnnotation(generatedAnnotation);
    typeSpec.addSuperinterface(interfaceType);
    typeSpec.addField(condoField);
    typeSpec.addField(delegateField);
    typeSpec.addMethod(processImplConstructor(condoField, delegateField));

    for (final Element element : typeElement.getEnclosedElements()) {
      if (element.getKind() != ElementKind.METHOD) {
        continue;
      }

      final ExecutableElement executableElement = (ExecutableElement) element;

      final String methodName = executableElement.getSimpleName().toString();
      final MethodSpec.Builder methodSpec = MethodSpec.methodBuilder(methodName);
      final TypeMirror returnType = executableElement.getReturnType();

      methodSpec.addAnnotation(overrideAnnotation);
      methodSpec.addModifiers(Modifier.PUBLIC);
      methodSpec.returns(TypeName.get(returnType));

      final List<ParameterSpec> metadataParameters = new ArrayList<>();
      final List<ParameterSpec> delegateParameters = new ArrayList<>();
      final List<String> metadataFormatParts = new ArrayList<>();
      final List<String> delegateFormatParts = new ArrayList<>();

      for (final VariableElement parameter : executableElement.getParameters()) {
        final ParameterSpec parameterSpec = ParameterSpec
            .builder(TypeName.get(parameter.asType()), parameter.getSimpleName().toString(),
                Modifier.FINAL)
            .build();

        final AutoCondo.Skip skip = parameter.getAnnotation(AutoCondo.Skip.class);

        if (skip == null) {
          metadataParameters.add(parameterSpec);
          metadataFormatParts.add("$N");
        }

        methodSpec.addParameter(parameterSpec);
        delegateParameters.add(parameterSpec);
        delegateFormatParts.add("$N");
      }

      final String metadataFormat = PARAMETER_JOINER.join(metadataFormatParts);
      final String delegateFormat = PARAMETER_JOINER.join(delegateFormatParts);

      final Stream.Builder<Object> arguments = Stream.builder();
      arguments.add(condoField);
      arguments.add(metadata);
      arguments.add(METADATA_TYPE_CONVERTER.convert(methodName));
      metadataParameters.forEach(arguments::add);

      final Stream.Builder<Object> delegateOnly = Stream.builder();

      delegateOnly.add(delegateField);
      delegateOnly.add(methodName);
      delegateParameters.forEach(delegateOnly::add);

      if (TypeKind.VOID == returnType.getKind()) {
        final Object[] args =
            Stream.concat(arguments.build(), delegateOnly.build()).toArray(Object[]::new);

        methodSpec.addStatement(
            String.format("$N.schedule(new $T.$L(%s), () -> { $N.$L(%s); return null; })",
                metadataFormat, delegateFormat), args);
      } else if (TypeKind.DECLARED == returnType.getKind() &&
          ((DeclaredType) returnType).asElement().equals(completableFutureType)) {
        final Object[] args =
            Stream.concat(arguments.build(), delegateOnly.build()).toArray(Object[]::new);

        methodSpec.addStatement(
            String.format("return $N.scheduleAsync(new $T.$L(%s), () -> $N.$L(%s))", metadataFormat,
                delegateFormat), args);
      } else {
        final Object[] args = delegateOnly.build().toArray(Object[]::new);

        methodSpec.addStatement(String.format("return $N.$L(%s)", delegateFormat), args);
      }

      typeSpec.addMethod(methodSpec.build());
    }

    return JavaFile
        .builder(packageName, typeSpec.build())
        .skipJavaLangImports(true)
        .indent("  ")
        .build();
  }

  private MethodSpec processImplConstructor(
      final FieldSpec condoField, final FieldSpec delegateField
  ) {
    final ParameterSpec condoParameter =
        ParameterSpec.builder(condoField.type, "condo", Modifier.FINAL).build();

    final ParameterSpec delegateParameter =
        ParameterSpec.builder(delegateField.type, "delegate", Modifier.FINAL).build();

    final MethodSpec.Builder builder = MethodSpec.constructorBuilder();

    builder.addModifiers(Modifier.PUBLIC);
    builder.addParameter(condoParameter);
    builder.addParameter(delegateParameter);

    builder.addStatement("this.$N = $N", condoField, condoParameter);
    builder.addStatement("this.$N = $N", delegateField, delegateParameter);

    return builder.build();
  }

  private JavaFile processMetadata(final TypeElement typeElement) {
    final String packageName = elements.getPackageOf(typeElement).getQualifiedName().toString();
    final String className =
        MessageFormat.format(METADATA_NAME_FORMAT, typeElement.getSimpleName());

    final ClassName metadataType = ClassName.get(packageName, className);

    final AnnotationSpec generatedAnnotation = AnnotationSpec
        .builder(ClassName.get(GENERATED_PACKAGE, GENERATED))
        .addMember("value", "$S", CONDO_PROCESSOR)
        .build();

    final TypeSpec.Builder typeSpec = TypeSpec.interfaceBuilder(metadataType);

    typeSpec.addAnnotation(generatedAnnotation);

    for (final Element element : typeElement.getEnclosedElements()) {
      if (element.getKind() != ElementKind.METHOD) {
        continue;
      }

      final ExecutableElement executableElement = (ExecutableElement) element;

      final String methodName = executableElement.getSimpleName().toString();

      final TypeSpec.Builder childTypeSpec =
          TypeSpec.classBuilder(METADATA_TYPE_CONVERTER.convert(methodName));

      childTypeSpec.addModifiers(Modifier.PUBLIC);
      childTypeSpec.addModifiers(Modifier.STATIC);

      childTypeSpec.addSuperinterface(metadataType);

      final List<FieldSpec> fields = new ArrayList<>();

      /* setup fields */
      for (final VariableElement parameter : executableElement.getParameters()) {
        final FieldSpec fieldSpec = FieldSpec
            .builder(TypeName.get(parameter.asType()), parameter.getSimpleName().toString(),
                Modifier.PRIVATE, Modifier.FINAL)
            .build();

        final AutoCondo.Skip skip = parameter.getAnnotation(AutoCondo.Skip.class);

        if (skip == null) {
          childTypeSpec.addField(fieldSpec);
          fields.add(fieldSpec);
        }
      }

      /* generate constructor */
      final MethodSpec.Builder constructorSpec = MethodSpec.constructorBuilder();

      constructorSpec.addModifiers(Modifier.PUBLIC);

      for (final FieldSpec fieldSpec : fields) {
        final ParameterSpec parameterSpec =
            ParameterSpec.builder(fieldSpec.type, fieldSpec.name, Modifier.FINAL).build();

        constructorSpec.addParameter(parameterSpec);
        constructorSpec.addStatement("this.$N = $N", fieldSpec, parameterSpec);
      }

      childTypeSpec.addMethod(constructorSpec.build());

      /* generate accessors */
      for (final FieldSpec fieldSpec : fields) {
        final MethodSpec.Builder methodSpec = MethodSpec.methodBuilder(fieldSpec.name);

        methodSpec.addModifiers(Modifier.PUBLIC);
        methodSpec.returns(fieldSpec.type);
        methodSpec.addStatement("return $N", fieldSpec);

        childTypeSpec.addMethod(methodSpec.build());
      }

      typeSpec.addType(childTypeSpec.build());
    }

    return JavaFile
        .builder(packageName, typeSpec.build())
        .skipJavaLangImports(true)
        .indent("  ")
        .build();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(AUTO_CONDO);
  }
}
