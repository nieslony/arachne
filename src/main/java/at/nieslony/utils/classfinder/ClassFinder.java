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
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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

    private void getPath(String dir, Path p, FileSystem fs,
            List<Class> classes, ClassMatcher matcher)
        throws IOException, ClassNotFoundException
    {
        DirectoryStream<Path> stream = Files.newDirectoryStream(p);
        for (Path file: stream) {
            String fn = file.getFileName().toString();

            if (fn.endsWith(".class")) {
                String className = dir + "/" + fn;
                className = className.replaceAll("/", ".").substring(1).replaceAll("\\.\\.", ".");
                className = className.substring(0, className.lastIndexOf(".class"));

                try {
                    Class c = Class.forName(className);
                    if (matcher.classMatches(c))
                        classes.add(c);
                    }
                catch (ClassNotFoundException ex) {
                    logger.warning(String.format("Cannot find class %s: %s", className, ex.toString()));
                }
                catch (Exception ex) {
                    logger.severe(String.format("Cannot load class %s: %s",
                            className, ex.getMessage()));
                }
            }

            if (fn.endsWith("/"))
                getPath(dir + fn, file, fs, classes, matcher);

            try {
                if (file.toFile().isDirectory())
                    getPath(dir + "/" + fn, file, fs, classes, matcher);
            }
            catch (UnsupportedOperationException ex) {
            }
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

            if (uri.getScheme().equals("jar")) {
                Map<String, String> env = new HashMap<>();
                try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
                    getPath("", fs.getPath("/"), fs, classes, matcher);
                }
            }
            else if (uri.getScheme().equals("file")) {
                String fn = uri.getSchemeSpecificPart();
                Path p = Paths.get(fn);
                FileSystem fs = FileSystems.getFileSystem(new URI("file:/"));
                getPath("", p, fs, classes, matcher);
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
        return getMatchingClasses(new ImplementingInterfaceMatcher(interfaceC));
    }
}
