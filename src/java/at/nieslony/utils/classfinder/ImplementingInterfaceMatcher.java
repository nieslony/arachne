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
public class ImplementingInterfaceMatcher
    implements ClassMatcher
{
    Class matchingInterface;

    public ImplementingInterfaceMatcher(Class interfaceC)
            throws ClassNotFoundException
    {
        matchingInterface = interfaceC;
    }

    @Override
    public boolean classMatches(Class c) {
        for (Class i : c.getInterfaces()) {
            if (i.getName().equals(matchingInterface.getName())) {
                System.out.println(i.getName() + " implements " + c.getName());
                return true;
            }
        }

        return false;
    }
}
