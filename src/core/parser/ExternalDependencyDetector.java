package core.parser;

import java.util.*;

/**
 * Detects external dependencies (imports) from classes
 * Filters out internal project classes and groups by package
 */
// Have to show this info separately as per requirememnts.
public class ExternalDependencyDetector {
    public Map<String, Set<String>> detectExternalDependencies(List<ClassInfo> classes) {
        Map<String, Set<String>> packageDependencies = new LinkedHashMap<>();

        // Get all internal class names (project classes)
        Set<String> internalClasses = getInternalClassNames(classes);

        // Process each class
        for (ClassInfo classInfo : classes) {
            String className = classInfo.getName();

            for (String importStmt : classInfo.getImports()) {
                // Filter out internal classes
                if (isExternalDependency(importStmt, internalClasses)) {
                    String packageName = extractPackageName(importStmt);

                    // Add to map
                    packageDependencies.putIfAbsent(packageName, new LinkedHashSet<>());
                    packageDependencies.get(packageName).add(className);
                }
            }
        }

        return packageDependencies;
    }

    /**
     * Get all internal class names from the project
     */
    private Set<String> getInternalClassNames(List<ClassInfo> classes) {
        Set<String> internalClasses = new HashSet<>();
        for (ClassInfo classInfo : classes) {
            String fullName = classInfo.getPackageName().isEmpty()
                ? classInfo.getName()
                : classInfo.getPackageName() + "." + classInfo.getName();
            internalClasses.add(fullName);
            internalClasses.add(classInfo.getName());
        }
        return internalClasses;
    }

    /**
     * Check if an import is an external dependency (not from the project)
     */
    private boolean isExternalDependency(String importStmt, Set<String> internalClasses) {
        // Extract class name from import
        String className = extractClassName(importStmt);

        // Check if it's an internal class
        if (internalClasses.contains(className) || internalClasses.contains(importStmt)) {
            return false;
        }

        // Filter out common Java primitives and default packages
        if (importStmt.startsWith("java.lang.") && !importStmt.contains(".annotation")) {
            return false; // java.lang is auto-imported
        }

        return true;
    }

    /**
     * Extract package name from import statement
     * Example: "java.util.ArrayList" -> "java.util"
     */
    private String extractPackageName(String importStmt) {
        int lastDot = importStmt.lastIndexOf('.');
        if (lastDot > 0) {
            return importStmt.substring(0, lastDot);
        }
        return importStmt;
    }

    /**
     * Extract class name from import statement
     * Example: "java.util.ArrayList" -> "ArrayList"
     */
    private String extractClassName(String importStmt) {
        int lastDot = importStmt.lastIndexOf('.');
        if (lastDot > 0) {
            return importStmt.substring(lastDot + 1);
        }
        return importStmt;
    }
}
