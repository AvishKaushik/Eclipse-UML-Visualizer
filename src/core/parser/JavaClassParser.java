package core.parser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;


public class JavaClassParser {
    /**
     * Parse a compilation unit and extract all classes
     */
    public List<ClassInfo> parse(ICompilationUnit compilationUnit) throws JavaModelException {
        List<ClassInfo> classes = new ArrayList<>();
        
        IType[] types = compilationUnit.getAllTypes();
        for (IType type : types) {
            ClassInfo classInfo = parseType(type);
            classes.add(classInfo);
        }
        
        return classes;
    }
    
    /**
     * Parse a single type (class/interface/enum)
     */
    private ClassInfo parseType(IType type) throws JavaModelException {
        String typeName = type.getElementName();
        String typeKind = getTypeKind(type);
        
        ClassInfo classInfo = new ClassInfo(typeName, typeKind);
        
        // Parse fields
        for (IField field : type.getFields()) {
            FieldInfo fieldInfo = parseField(field);
            classInfo.addField(fieldInfo);
        }
        
        // Parse methods
        for (IMethod method : type.getMethods()) {
            if (!method.isConstructor()) {
                MethodInfo methodInfo = parseMethod(method);
                classInfo.addMethod(methodInfo);
            }
        }
        
        return classInfo;
    }
    
    /**
     * Parse a field
     */
    private FieldInfo parseField(IField field) throws JavaModelException {
        String name = field.getElementName();
        String type = Signature.toString(field.getTypeSignature());
        String visibility = getVisibility(field.getFlags());
        boolean isStatic = org.eclipse.jdt.core.Flags.isStatic(field.getFlags());
        
        return new FieldInfo(name, type, visibility, isStatic);
    }
    
    /**
     * Parse a method
     */
    private MethodInfo parseMethod(IMethod method) throws JavaModelException {
        String name = method.getElementName();
        String returnType = Signature.toString(method.getReturnType());
        String visibility = getVisibility(method.getFlags());
        boolean isStatic = org.eclipse.jdt.core.Flags.isStatic(method.getFlags());
        
        MethodInfo methodInfo = new MethodInfo(name, returnType, visibility, isStatic);
        
        // Parse parameters
        String[] paramTypes = method.getParameterTypes();
        String[] paramNames = method.getParameterNames();
        for (int i = 0; i < paramTypes.length; i++) {
            String paramType = Signature.toString(paramTypes[i]);
            String paramName = i < paramNames.length ? paramNames[i] : "arg" + i;
            methodInfo.addParameter(new ParameterInfo(paramName, paramType));
        }
        
        return methodInfo;
    }
    
    /**
     * Get type kind (class, interface, enum)
     */
    private String getTypeKind(IType type) throws JavaModelException {
        if (type.isEnum()) {
            return "enum";
        } else if (type.isInterface()) {
            return "interface";
        } else {
            return "class";
        }
    }
    
    /**
     * Get visibility modifier
     */
    private String getVisibility(int flags) {
        if (org.eclipse.jdt.core.Flags.isPublic(flags)) {
            return "public";
        } else if (org.eclipse.jdt.core.Flags.isPrivate(flags)) {
            return "private";
        } else if (org.eclipse.jdt.core.Flags.isProtected(flags)) {
            return "protected";
        } else {
            return "package";
        }
    }
}