/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils;

/**
 *
 * @author claas
 */
public class ShellQuote {

    public static String shellQuote(String s) {
        return "'" + s.replaceAll("'", "'\"'\"'") + "'";
    }

    public static String escapeChars(String s, String chars) {
        return s.replaceAll("([" + chars + "])", "\\\\$1");
    }
}
