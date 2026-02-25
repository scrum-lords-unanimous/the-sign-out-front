package FluentUIJavaFxKit;

import java.net.URL;
import java.net.URLClassLoader;

class ChildFirstClassLoader extends URLClassLoader {

    ChildFirstClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c != null) return c;

            if (shouldDelegateToParent(name)) return super.loadClass(name, resolve);

            try {
                c = findClass(name);
                if (resolve) resolveClass(c);
                return c;
            } catch (ClassNotFoundException e) {
                return super.loadClass(name, resolve);
            }
        }
    }

    private static boolean shouldDelegateToParent(String name) {
        return name.startsWith("java.")
                || name.startsWith("jdk.")
                || name.startsWith("javax.")
                || name.startsWith("javafx.")
                || name.startsWith("com.sun.")
                || name.startsWith("sun.")
                || name.startsWith("com.fasterxml.")
                || name.startsWith("org.fxmisc.");
    }
}
