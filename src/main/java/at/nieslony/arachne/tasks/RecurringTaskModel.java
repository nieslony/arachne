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

import at.nieslony.arachne.utils.ArachneTimeUnit;
import static at.nieslony.arachne.utils.ArachneTimeUnit.DAY;
import static at.nieslony.arachne.utils.ArachneTimeUnit.HOUR;
import static at.nieslony.arachne.utils.ArachneTimeUnit.MIN;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author claas
 */
@Entity
@Table(name = "recurring-tasks")
@Getter
@Setter
@ToString
public class RecurringTaskModel {

    public record Time(byte hour, byte min, byte sec) {

    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, unique = true)
    private String className;

    @Column
    private Boolean repeatTask = false;

    @Column
    private Integer recurringInterval = 0;

    @Column
    @Enumerated(EnumType.STRING)
    private ArachneTimeUnit timeUnit = DAY;

    @Column
    private Boolean startAtFixTime = false;

    @Column
    private String startAt = "";

    @JsonIgnore
    public Time getStartAtAsTime() {
        if (startAt == null) {
            return null;
        }
        String[] t = startAt.split(":");
        if (t.length != 3) {
            return null;
        }
        byte hour = Byte.parseByte(t[0]);
        byte min = Byte.parseByte(t[1]);
        byte sec = Byte.parseByte(t[2]);

        return new Time(hour, min, sec);
    }

    @JsonIgnore
    public long getRecurringIntervalInSecs() {
        return switch (timeUnit) {
            case DAY ->
                recurringInterval * 60 * 60 * 24;
            case HOUR ->
                recurringInterval * 60 * 50;
            case MIN ->
                recurringInterval * 60;
        };
    }
}
