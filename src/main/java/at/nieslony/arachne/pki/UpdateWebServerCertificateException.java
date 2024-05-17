/*
 * Copyright (C) 2024 claas
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
package at.nieslony.arachne.pki;

/**
 *
 * @author claas
 */
public class UpdateWebServerCertificateException extends PkiException {

    private final String filename;
    private final String rootMessage;

    public UpdateWebServerCertificateException(String filename, Exception rootCause) {
        super("Cannot write %s: %s".formatted(filename, rootCause.getMessage()));
        this.filename = filename;
        this.rootMessage = rootCause.getMessage();
    }

    public String getFilename() {
        return filename;
    }

    public String getRoorMessage() {
        return rootMessage;
    }
}
