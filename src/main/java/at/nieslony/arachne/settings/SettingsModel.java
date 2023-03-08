/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.settings;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
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
