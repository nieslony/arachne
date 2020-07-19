/*
 * Copyright (C) 2018 Claas Nieslony
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.nieslony.utils.classfinder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import org.jboss.vfs.VirtualFile;

/**
 *
 * @author claas
 */
public class ClassFinder {
    private ClassLoader classLoader;
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    public ClassFinder(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    private void getClassesFromFolder(Path folder, List<Class> classes, ClassMatcher matcher) {
        try {
            Files.walk(folder).filter(p -> p.getFileName().toString().endsWith(".class")).forEach(file -> {
                logger.info("All: " + file.toString());

            });
        }
        catch (IOException ex) {
            logger.warning(ex.getMessage());
        }
    }

    private void getClassesFromVfs(URL vfsFilePath, List<Class> classes, ClassMatcher matcher) {
        try {
            URLConnection conn = new URL(vfsFilePath.toString()).openConnection();
            VirtualFile vf = (VirtualFile)conn.getContent();
            for (VirtualFile f: vf.getChildrenRecursively()) {
                String fn = f.getPathNameRelativeTo(vf);
                if (fn.endsWith(".class")) {
                    String className = fn.replaceAll("/", ".")
                            .substring(0, fn.length() - ".class".length());
                    //logger.info(String.format("Loading class %s", className));
                    try {
                        Class cl = Class.forName(className);
                        if (matcher.classMatches(cl)) {
                            logger.info(String.format("Class %s matches", className));
                            classes.add(cl);
                        }
                    }
                    catch (ClassNotFoundException | NoClassDefFoundError | IllegalAccessError ex) {
                        logger.warning(String.format("Cannot load class %s: %s", className, ex.getMessage()));
                    }
                }
            }
        }
        catch (IOException ex) {
            logger.warning(ex.getMessage());
        }
    }

    private void getClassesFromJar(Path jarFilePath, List<Class> classes, ClassMatcher matcher) {
        String fn = "";
        String className = "";
        try {
            fn = jarFilePath.toString();
            if (fn.endsWith("!"))
                fn = fn.substring(0, fn.length()-1);
            if (fn.startsWith("file:"))
                fn = fn.substring("file:".length());
            JarFile jarFile = new JarFile(fn);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryFN = entry.getName();
                if (entryFN.endsWith(".class")) {
                    className = entryFN.replaceAll("/", ".");
                    className = className.substring(0, entryFN.length() - ".class".length());

                    //logger.info(String.format("Loading class %s", className));
                    try {
                        Class cl = Class.forName(className);
                        if (matcher.classMatches(cl)) {
                            logger.info(String.format("Class %s matches", className));
                            classes.add(cl);
                        }
                    }
                    catch (ExceptionInInitializerError | ClassNotFoundException | NoClassDefFoundError | IllegalAccessError ex) {
                        logger.warning(String.format("Cannot load class %s: %s", className, ex.getMessage()));
                    }
                }
            }
        }
        catch (IOException ex) {
            logger.warning(String.format("Error reading JAR file \"%s\": %s",
                    fn, ex.toString()));
        }
        catch (Exception ex) {
            logger.warning(String.format("Cannot load class %s: %s", className, ex.getMessage()));
        }
    }

    public List<Class> getMatchingClasses(ClassMatcher matcher)
        throws IOException, URISyntaxException, ClassNotFoundException
    {
        List<Class> classes = new LinkedList<>();
        Enumeration<URL> classPathElements = classLoader.getResources("/");

        while (classPathElements.hasMoreElements()) {
            URI uri = classPathElements.nextElement().toURI();
            logger.info(String.format("Looking for classes in %s", uri.toString()));

            /*if (uri.getScheme().equals("jar")) {
                Map<String, String> env = new HashMap<>();
                try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
                    getPath("", fs.getPath("/"), fs, classes, matcher);
                }
            }
            else */
            if (uri.getScheme().equals("vfs")) {
                getClassesFromVfs(uri.toURL(), classes, matcher);

            }
            else if (uri.getScheme().equals("jar")) {
                String fn = uri.getSchemeSpecificPart();
                Path p = Paths.get(fn);
                getClassesFromJar(p, classes, matcher);

            }
            else if (uri.getScheme().equals("file")) {
                String fn = uri.getSchemeSpecificPart();
                Path p = Paths.get(fn);
                getClassesFromFolder(p, classes, matcher);

                //FileSystem fs = FileSystems.getFileSystem(new URI("file:/"));
                //getPath("", p, fs, classes, matcher);
            }
            else {
                logger.warning(String.format("Unknown schema: %s", uri.getScheme()));
            }
        }

        return classes;
    }

    public List<Class> getAllClasses()
        throws IOException, URISyntaxException, ClassNotFoundException
    {
        return getMatchingClasses((Class c) -> true);
    }

    public List<Class> getAllClassesImplementing(Class interfaceC)
        throws IOException, URISyntaxException, ClassNotFoundException
    {
        logger.info(String.format("Getting all classes implementing %s", interfaceC.getName()));
        List<Class> classes = getMatchingClasses(new ImplementingInterfaceMatcher(interfaceC));
        logger.info(String.format("Found %d classes", classes.size()));
        return classes;
    }
}
