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

package at.nieslony.openvpnadmin.tasks;

import java.io.Serializable;

/**
 *
 * @author claas
 */
public class AvailableTask implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 7701185371837929819L;
	
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
