/*
 * Copyright (C) 2024 claas
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
package at.nieslony.arachne.utils;

import java.util.function.Supplier;

/**
 *
 * @author claas
 */
public class LazyCreate<T> {

    private T value = null;
    private final Supplier<T> valueCreater;

    private LazyCreate() {
        valueCreater = null;
    }

    public LazyCreate(Supplier<T> valueCreater) {
        this.valueCreater = valueCreater;
    }

    public T get() {
        if (value == null) {
            value = valueCreater.get();
        }
        return value;
    }

}
