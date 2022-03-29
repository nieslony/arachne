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

import jakarta.faces.context.FacesContext;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 * @author claas
 */
public class BeanInjector {
    public static void injectStaticBeans(FacesContext ctx, Class c)
        throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException
    {
        for (Field field : c.getDeclaredFields()) {
            if (field.isAnnotationPresent(StaticMemberBean.class)) {
                String fieldName = field.getName();
                String setterName = "set" +
                        fieldName.substring(0, 1).toUpperCase() +
                        fieldName.substring(1);
                Method setter = null;
                setter = c.getMethod(setterName, field.getType());
                if (setter != null) {
                    setter.invoke(c, (Object) findBean(ctx, fieldName));
                }
            }
        }
    }

    private static <T> T findBean(FacesContext ctx, String beanName) {
        return (T) ctx.getApplication().evaluateExpressionGet(ctx, "#{" + beanName + "}", Object.class);
    }

}
