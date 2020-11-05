package com.example.springboot;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.IOTools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Extract classes from BOOT-INF/classes and add to CP so that Java compiler can see them.
 * See also https://docs.spring.io/spring-boot/docs/1.2.7.RELEASE/reference/html/howto-build.html#howto-extract-specific-libraries-when-an-executable-jar-runs
 */
public class ExtractSpringBootClasses {
    private static final String PREFIX = "BOOT-INF/classes";
    private static final String SUFFIX = "!/" + PREFIX + "!/";

    public static void addToClassPath(Class clazz) {
        addToClassPath(clazz, IOTools.createTempDirectory(PREFIX.replaceAll("/", "_")).toFile());
    }

    public static void addToClassPath(Class clazz, File baseDir) {
        Jvm.addToClassPath(clazz);
        final ClassLoader cl = clazz.getClassLoader();
        if (!(cl instanceof URLClassLoader))
            return;

        try {
            final URLClassLoader ucl = (URLClassLoader) cl;
            for (URL url : ucl.getURLs()) {
                if (url.toString().endsWith(SUFFIX)) {
                    String file = url.getPath().substring(0, url.getPath().length() - SUFFIX.length());
                    final URL url1 = new URL(file);
                    final JarFile jf = new JarFile(url1.getFile());
                    for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements(); ) {
                        JarEntry je = e.nextElement();
                        if (je.getName().startsWith(PREFIX)) {
                            File fl = new File(baseDir, je.getName());
                            if (!je.isDirectory()) {
                                fl.getParentFile().mkdirs();
                                copy(jf, je, fl);
                            }
                        }
                    }

                    File cpBaseDir = new File(baseDir, PREFIX);
                    String classpath = System.getProperty(Jvm.JAVA_CLASS_PATH);
                    System.setProperty(Jvm.JAVA_CLASS_PATH, classpath + File.pathSeparator + cpBaseDir.getCanonicalPath());
                    Jvm.debug().on(ExtractSpringBootClasses.class, "Added " + cpBaseDir + " to classpath");
                    baseDir.deleteOnExit();
                    return;
                }
            }
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    private static void copy(JarFile jf, JarEntry from, File to) throws IOException {
        try (InputStream is = jf.getInputStream(from);
             FileOutputStream fo = new FileOutputStream(to)) {
            while (is.available() > 0) {
                fo.write(is.read());
            }
        }
    }
}
