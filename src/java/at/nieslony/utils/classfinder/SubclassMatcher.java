/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.utils.classfinder;

/**
 *
 * @author claas
 */
public class SubclassMatcher
    implements ClassMatcher
{
    Class matchingClass;

    public SubclassMatcher(String superName)
            throws ClassNotFoundException
    {
        matchingClass = Class.forName(superName);
    }

    @Override
    public boolean classMatches(Class c) {
        return c.getSuperclass().equals(matchingClass);
    }
}
