/*
 * Copyright (C) 2026 claas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package at.nieslony.arachne.onetimeview;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@Getter
@Setter
@Entity
@ToString
@Table(name = "one-time-views")
@Slf4j
public class OneTimeViewModel implements Serializable {

    @Id
    private String id;

    @NotNull
    private String view;

    @NotNull
    private String username;

    @NotNull
    private LocalDateTime validUntil;

    private LocalDateTime visited = null;

    @JsonIgnore
    public String getValidUntilString() {
        try {
            return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(validUntil);
        } catch (DateTimeException ex) {
            log.warn("Invalid date/time: %s: %s".formatted(
                    validUntil.toString(),
                    ex.getMessage()
            ));
            return "???";
        }
    }

    @JsonIgnore
    public String getVisitedString() {
        if (visited == null) {
            return "never";
        }
        try {
            return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(visited);
        } catch (DateTimeException ex) {
            log.warn("Invalid date/time: %s: %s".formatted(
                    validUntil.toString(),
                    ex.getMessage()
            ));
            return "???";
        }
    }
}
