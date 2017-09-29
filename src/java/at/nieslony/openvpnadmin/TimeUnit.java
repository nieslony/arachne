/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

/**
 *
 * @author claas
 */
public enum TimeUnit {
    SEC("secs",    1000),
    MIN("mins",    1000 * 60),
    HOUR("hours",  1000 * 60 * 60),
    DAY("days",    1000 * 60 * 60 * 24),
    WEEK("weeks",  1000 * 60 * 60 * 7),
    MONTH("month", 1000 * 60 * 60 * 30),
    YEAR("years",  1000 * 60 * 60 * 365);

    final private String timeUnitName;
    long value;

    private TimeUnit(String name, long value) {
        timeUnitName = name;
        this.value = value;
    }

    public String getUnitName() {
        return timeUnitName;
    }

    public long getValue() {
        return value;
    }
}
