package org.azertio.core.util;

/**
 * Utility for loading classes that may reside in dynamically-added modules
 * (e.g. JARs declared with {@code with} in azertio.yaml and loaded into the module layer at runtime).
 */
public final class ClassFinder {

    private ClassFinder() {}

    /**
     * Finds a class by name, first via the standard {@link Class#forName(String)}, then by
     * iterating all modules in the layer of the given {@code anchor} class.
     * This allows finding classes from JARs that were added to the module layer at runtime.
     *
     * @param className the fully qualified class name to find
     * @param anchor    a class whose module layer is used as the search scope
     * @return the class, or {@code null} if not found
     */
    public static Class<?> find(String className, Class<?> anchor) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {}

        ModuleLayer layer = anchor.getModule().getLayer();
        if (layer == null) return null;
        for (Module module : layer.modules()) {
            ClassLoader loader = module.getClassLoader();
            if (loader == null) continue;
            try {
                return loader.loadClass(className);
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

}