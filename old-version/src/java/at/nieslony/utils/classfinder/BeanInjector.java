/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.utils.classfinder;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.faces.context.FacesContext;

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
