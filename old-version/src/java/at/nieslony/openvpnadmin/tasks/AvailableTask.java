/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.tasks;

import java.io.Serializable;

/**
 *
 * @author claas
 */
public class AvailableTask implements Serializable {

    private Class klass;
    private String name;
    private String description;

    public AvailableTask(AvailableTask at) {
        klass = at.klass;
        name = at.name;
        description = at.description;
    }

    public AvailableTask(Class klass) {
        this.klass = klass;
        if (klass.isAnnotationPresent(ScheduledTaskInfo.class)) {
            ScheduledTaskInfo info =
                    (ScheduledTaskInfo) klass.getAnnotation(ScheduledTaskInfo.class);
            name = info.name();
            description = info.description();
        }
    }

    public Class getKlass() {
        return klass;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

}
