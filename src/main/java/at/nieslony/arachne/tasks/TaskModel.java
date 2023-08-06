/*
 * Copyright (C) 2023 claas
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
package at.nieslony.arachne.tasks;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author claas
 */
@Entity
@Table(name = "tasks")
@Getter
@Setter
@ToString
public class TaskModel {

    public enum TimeUnit {
        MIN("min"),
        HOUR("hours"),
        DAYS("days");

        private String unit;

        private TimeUnit(String u) {
            unit = u;
        }

        @Override
        public String toString() {
            return unit;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column
    private Boolean enabled;

    @Column
    private TimeUnit delayUnit;

    @Column
    private Integer delay;

    @Column
    private TimeUnit intervalUnit;

    @Column
    private Integer interval;

    @Column(nullable = false)
    private String taskClassName;

    @Column
    private Date lastRun;
}
