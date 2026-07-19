/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn.management.commands;

import at.nieslony.arachne.openvpn.management.ManagementException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@Slf4j
public class Version extends MultiLineCommand<Version.VersionInfo> {

    public record VersionInfo(
            String programName,
            String versionStr,
            int versionMajor,
            int versionMinor,
            int versionPatch,
            String plattform,
            Set<String> flags,
            int managementVersion
            ) {

    }

    public Version(BlockingQueue<Command> queue) {
        super(queue, "version");
    }

    @Override
    public void processResult() throws ManagementException {
        if (lines.size() != 2) {
            throw new ManagementException("Cannot parse output (too many lines): " + lines.toString());
        }
        log.debug("Parsing: " + lines.get(0));
        String[] split = lines.get(0).split(" ");
        String progName = split[2];
        String versionStr = split[3];
        log.debug("VersionStr: " + versionStr);
        String[] versionFields = versionStr.split("\\.");
        int versionMajor = Integer.parseInt(versionFields[0]);
        int versionMinor = Integer.parseInt(versionFields[1]);
        int versionPatch = Integer.parseInt(versionFields[2]);
        String plattform = split[4];

        Set<String> flags = new HashSet<>();
        Pattern fieldPattern = Pattern.compile("\\[([^\\]]*)\\]");
        Matcher fieldMatcher = fieldPattern.matcher(lines.get(0));
        while (fieldMatcher.find()) {
            flags.add(fieldMatcher.group(1));
        }

        int managementVersion = Integer.parseInt(lines.get(1).split(": ")[1]);
        VersionInfo versionInfo = new VersionInfo(
                progName,
                versionStr,
                versionMajor, versionMinor, versionPatch,
                plattform,
                flags,
                managementVersion
        );
        value.complete(versionInfo);
    }
}
