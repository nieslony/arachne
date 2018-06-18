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
