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

    public enum Status {
        WAITING("Waiting"),
        SCHEDULED("Scheduled"),
        RUNNING("Running"),
        SUCCESS("Success"),
        ERROR("Error");

        private String status;

        private Status(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return status;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private String taskClassName;

    @Column
    private Date scheduled;

    @Column
    private Date started;

    @Column
    private Date stopped;

    @Column(nullable = false)
    private Status status;

    @Column
    private String statusMsg;

    public Class<? extends Task> getTaskClass() throws ClassNotFoundException {
        return (Class<? extends Task>) Class.forName(taskClassName);
    }

    public void setStarted() {
        started = new Date();
    }

    public void setStopped() {
        stopped = new Date();
    }
}
