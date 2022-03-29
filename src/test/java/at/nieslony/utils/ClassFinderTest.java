/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.utils;

import at.nieslony.utils.classfinder.ClassFinder;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author claas
 */
public class ClassFinderTest {
    @Test
    public void testTest() {
        ClassFinder classFinder = new ClassFinder((getClass().getClassLoader()));
        try {
            List<Class> classes;
            classes = classFinder.getAllClassesImplementing(Serializable.class);
            assertNotEquals(0, classes.size());
        }
        catch (IOException|URISyntaxException|ClassNotFoundException ex) {
        }
    }
}
