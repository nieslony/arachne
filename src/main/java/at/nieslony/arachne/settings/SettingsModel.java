/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 *
 * @author claas
 */
@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "arachne_settings")
public class SettingsModel implements Serializable {

    public SettingsModel(String setting, byte[] content, String stringContent) {
        this.setting = setting;
        this.content = content;
        this.stringContent = stringContent;
    }

    public SettingsModel(String setting, byte[] content) {
        this.setting = setting;
        this.content = content;
        this.stringContent = null;
    }

    public SettingsModel(String setting, String stringContent) {
        this.setting = setting;
        this.content = null;
        this.stringContent = stringContent;
    }

    public SettingsModel() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @Column(nullable = false, unique = true)
    private String setting;

    @Column(columnDefinition = "BLOB")
    @Lob
    private byte[] content;

    @Column
    @Lob
    private String stringContent;

    public SettingsModel withContent(byte[] content) {
        this.content = content;
        return this;
    }

    public SettingsModel withStringContent(String stringContent) {
        this.stringContent = stringContent;
        return this;
    }
}
