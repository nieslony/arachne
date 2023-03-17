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
package at.nieslony.arachne.kerberos;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author claas
 */

/*
There are two versions of the file format used by the FILE keytab type. The first byte of the file always has the value 5, and the value of the second byte contains the version number (1 or 2). Version 1 of the file format uses native byte order for integer representations. Version 2 always uses big-endian byte order.

After the two-byte version indicator, the file contains a sequence of signed 32-bit record lengths followed by key records or holes. A positive record length indicates a valid key entry whose size is equal to or less than the record length. A negative length indicates a zero-filled hole whose size is the inverse of the length. A length of 0 indicates the end of the file.

entry ::=
    principal
    timestamp (32 bits)
    key version (8 bits)
    enctype (16 bits)
    key length (32 bits)
    key contents

principal ::=
    count of components (32 bits) (-- 16 --) [includes realm in version 1]
    realm (data)
    component1 (data)
    component2 (data)
    ...
    name type (32 bits) [omitted in version 1]

data ::=
    length (16 bits)
    value (length bytes)
 */
class KeytabFile {

    public class KeytabEntry {

        private String principal;
        private Date timestamp;
        private int enctype;
        private byte keyVersion;
        byte[] key;

        public String getPrincipal() {
            return this.principal;
        }

        public Date getTimeStamp() {
            return this.timestamp;
        }

        public int getEncType() {
            return this.enctype;
        }

        public byte[] getKey() {
            return key;
        }

        public byte getKeyVersion() {
            return keyVersion;
        }
    }

    List<KeytabEntry> entries = new LinkedList<>();

    int read16BitInt(InputStream is) throws IOException {
        byte first = (byte) is.read();
        byte last = (byte) is.read();
        return (first << 8) | last;
    }

    int read32BitInt(InputStream is) throws IOException {
        /*byte[] bytes = new byte[4];
        is.read(bytes, 0, 4);
        return (bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3];*/
        long[] data = new long[4];
        for (int i = 0; i < 4; i++) {
            data[i] = is.read();
        }
        long ret = (data[0] << 24) | (data[1] << 16) | (data[2] << 8) | data[3];
        return (int) ret;
    }

    public KeytabFile(String filename) throws IOException, KeytabException {
        try (FileInputStream ifs = new FileInputStream(filename)) {
            byte head = (byte) ifs.read();
            if (head != 5) {
                throw new KeytabException("Invalid file header");
            }
            byte byteOrder = (byte) ifs.read();
            if (byteOrder != 1 && byteOrder != 2) {
                throw new KeytabException("Invalid byte order: " + String.valueOf(byteOrder));
            }

            KeytabEntry entry;
            while ((entry = readEntry(ifs)) != null) {
                entries.add(entry);
            }

        }
    }

    KeytabEntry readEntry(InputStream is) throws IOException {
        KeytabEntry entry = new KeytabEntry();
        try {
            int recordLength = read32BitInt(is);
        } catch (IOException ex) {
            return null;
        }

        String principal = readPrincipal(is);
        if (principal == null) {
            return null;
        }
        entry.principal = principal;
        int timestamp = read32BitInt(is);
        entry.timestamp = new Date((long) timestamp * 1000);
        entry.keyVersion = (byte) is.read();
        entry.enctype = read16BitInt(is);
        int keyLength = read16BitInt(is);
        entry.key = new byte[keyLength];
        is.read(entry.key, 0, keyLength);
        read32BitInt(is);

        return entry;
    }

    String readPrincipal(InputStream is) throws IOException {
        int noComponents = read16BitInt(is);
        byte[] realmBytes = readData(is);
        if (realmBytes == null) {
            return null;
        }
        String realm = new String(realmBytes);
        String[] components = new String[noComponents];
        for (int i = 0; i < noComponents; i++) {
            components[i] = new String(readData(is));
        }
        int type = read32BitInt(is);

        String principal = "%s@%s"
                .formatted(
                        String.join("/", components),
                        realm
                );
        return principal;
    }

    byte[] readData(InputStream is) throws IOException {
        int length = read16BitInt(is);
        if (length < 0) {
            return null;
        }
        byte[] value = new byte[length];
        is.read(value, 0, length);
        return value;
    }

    public List<KeytabEntry> getEntries() {
        return entries;
    }

    public Set<String> getPrincipals() {
        Set<String> princs = new HashSet<>();
        for (KeytabEntry entry : entries) {
            princs.add(entry.principal);
        }

        return princs;
    }
}
