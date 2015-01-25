package info.archinnov.achilles;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import info.archinnov.achilles.annotations.ClusteringColumn;
import info.archinnov.achilles.annotations.Column;
import info.archinnov.achilles.annotations.PartitionKey;
import info.archinnov.achilles.internal.metadata.parsing.EntityIntrospector;
import info.archinnov.achilles.internal.metadata.parsing.PropertyFilter;
import info.archinnov.achilles.internal.proxy.ProxyInterceptor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes("info.archinnov.achilles.annotations.Entity")
public class AchillesProcessor extends AbstractProcessor {

    public static final String NEW_LINE = "\n";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Messager messager = processingEnv.getMessager();
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                messager.printMessage(Diagnostic.Kind.NOTE,"********************* Found entity : " + element.getSimpleName());
                TypeElement proxyElt = (TypeElement) element;
                try {
                    generateProxy(proxyElt);
                } catch (Exception e) {
                    messager.printMessage(Diagnostic.Kind.ERROR,e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private void generateProxy(TypeElement proxyElt) throws Exception {

        String interceptorType = ProxyInterceptor.class.getName()+"<"+proxyElt.getQualifiedName()+">";

        PackageElement pkg = processingEnv.getElementUtils().getPackageOf(proxyElt);
        JavaFileObject file = processingEnv.getFiler().createSourceFile(proxyElt.getQualifiedName() + "_AchillesSubClass", proxyElt);
        Writer writer = file.openWriter();
        writer.append("package ").append(pkg.getQualifiedName()).append(";").append(NEW_LINE);
        writer.append("public class ").append(proxyElt.getSimpleName()).append("_AchillesSubClass extends ").append(proxyElt.getQualifiedName()).append(" {").append(NEW_LINE);
        writer.append(NEW_LINE);
        writer.append("\t").append("private ").append(interceptorType).append(" interceptor;").append(NEW_LINE);
        writer.append(NEW_LINE);
        writer.append("\t").append("public void setInterceptor(").append(interceptorType).append(" interceptor) {").append(NEW_LINE);
        writer.append("\t\t").append("this.interceptor = interceptor;").append(NEW_LINE);
        writer.append("\t").append("}").append(NEW_LINE);
        writer.append(NEW_LINE);

        for (Element memberElt : processingEnv.getElementUtils().getAllMembers(proxyElt)) {
            Set<Modifier> modifiers = memberElt.getModifiers();
            if (memberElt.getKind() == ElementKind.FIELD &&
                    !modifiers.contains(Modifier.STATIC)) {

                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, " memberElt type : " + memberElt.getClass().getCanonicalName());
                VariableElement fieldElt = (VariableElement) memberElt;

                final boolean isPartitionKey = fieldElt.getAnnotation(PartitionKey.class) != null;
                final boolean isClusteringColumn = fieldElt.getAnnotation(ClusteringColumn.class) != null;
                final boolean isSimpleColumn = fieldElt.getAnnotation(Column.class) != null;

                if (isPartitionKey || isClusteringColumn || isSimpleColumn) {

                    final String fieldName = fieldElt.getSimpleName().toString();

                    final TypeMirror typeMirror = fieldElt.asType();
                    String typeName = extractType(typeMirror, fieldName, proxyElt.getQualifiedName().toString());

                    final String getterName = (typeName.equals("boolean") ? "is":"get") + StringUtils.capitalize(fieldName);
                    final String setterName = "set" + StringUtils.capitalize(fieldName);

                    if ("info.archinnov.achilles.type.Counter".equals(typeName)) {
                        writer.append("\t").append("@Override").append(NEW_LINE);
                        writer.append("\t").append("public ").append(typeName).append(" ").append(getterName).append("() {").append(NEW_LINE);
                        writer.append("\t\t").append("//this.interceptor.interceptCounterGet(").append(fieldName).append(")").append(NEW_LINE);
                        writer.append("\t\t").append("return super.").append(getterName).append("();").append(NEW_LINE);
                        writer.append("\t").append("}").append(NEW_LINE);
                    }

                    writer.append(NEW_LINE);

                    writer.append("\t").append("@Override").append(NEW_LINE);
                    writer.append("\t").append("public void ").append(setterName).append("(").append(typeName).append(" ").append("param").append(") {").append(NEW_LINE);
                    writer.append("\t\t").append(" //this.interceptor.interceptSetter(").append(fieldName).append(", param)").append(NEW_LINE);
                    writer.append("\t").append("}").append(NEW_LINE);

                    writer.append(NEW_LINE);
//                    try {
//
//                    } catch (ClassCastException ex) {
//                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ex.getMessage()+ " for field "+memberElt.toString()+" of class "+proxyElt.getQualifiedName());
//                        throw ex;
//                    }

                }


            }
        }
        writer.append("}\n");
        writer.close();
    }

    private String extractType(TypeMirror typeMirror, String fieldName, String className) {
        final TypeKind kind = typeMirror.getKind();
        switch (kind) {
            case ARRAY:
                return extractType(((ArrayType) typeMirror).getComponentType(), fieldName, className)+"[]";
            case BOOLEAN:
            case BYTE:
            case CHAR:
            case DOUBLE:
            case FLOAT:
            case INT:
            case LONG:
            case SHORT:
                return typeMirror.toString();

            case DECLARED:
                final List<? extends TypeMirror> typeArguments = ((DeclaredType) typeMirror).getTypeArguments();
                final String rawType = ((TypeElement) ((DeclaredType) typeMirror).asElement()).getQualifiedName().toString();
                if (isEmpty(typeArguments)) {
                    return rawType;
                } else if(typeArguments.size() == 1) {
                    return rawType + "<" + extractType(typeArguments.get(0), fieldName, className)+">";
                } else if (typeArguments.size() == 2) {
                    return rawType + "<" + extractType(typeArguments.get(0), fieldName, className) + "," + extractType(typeArguments.get(1), fieldName, className) + ">";
                } else {
                    throw new IllegalStateException("Cannot resolve type of field " + fieldName+" on entity "+className);
                }
            default:
                throw new IllegalStateException("Cannot resolve type of field " + fieldName+" on entity "+className);
        }
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

}
