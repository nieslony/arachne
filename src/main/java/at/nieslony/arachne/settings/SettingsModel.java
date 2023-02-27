/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.settings;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 *
 * @author claas
 */
@Data
@Entity
@Accessors(chain = true)
@Table(name = "arachne_settings")
public class SettingsModel implements Serializable {

    public SettingsModel(String setting, String content) {
        this.setting = setting;
        this.content = content;
    }

    public SettingsModel(String setting, int content) {
        this.setting = setting;
        this.content = String.valueOf(content);
    }

    public SettingsModel() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @Column(nullable = false, unique = true)
    private String setting;

    @Column
    @Lob
    private String content;

    public int getIntContent() {
        return Integer.getInteger(content);
    }

    public SettingsModel setContent(int content) {
        this.content = String.valueOf(content);
        return this;
    }

    public SettingsModel setContent(String content) {
        this.content = content;
        return this;
    }
}
