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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
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

    public SettingsModel(String setting, String content) {
        this.setting = setting;
        this.content = content;
    }

    public SettingsModel(String setting, int content) {
        this.setting = setting;
        this.content = String.valueOf(content);
    }

    public SettingsModel(String setting, List<String> content) {
        this.setting = setting;
        this.content = joinList(content);
    }

    public SettingsModel(String setting, boolean content) {
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

    static String encodeString(String s) {
        return s.replaceAll("\\\\", "\\\\\\\\").replaceAll(",", "\\\\,");
    }

    static String decodeString(String s) {
        return s.replaceAll("\\\\,", ",").replaceAll("\\\\\\\\", "\\\\");
    }

    static List<String> splitString(String s) {
        List<String> l = Arrays.asList(s.split(",,"));
        l.replaceAll(i -> decodeString(i));
        return l;
    }

    static String joinList(List<String> l) {
        List<String> lc = new LinkedList<>(l);
        lc.replaceAll(i -> encodeString(i));

        return String.join(",,", lc);
    }
}
