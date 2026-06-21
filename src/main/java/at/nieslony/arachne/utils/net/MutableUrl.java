/*
 * Copyright (C) 2026 claas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package at.nieslony.arachne.utils.net;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author claas
 */
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
public class MutableUrl implements Serializable {

    public record SchemaDescr(String name, int defaultPort) implements Serializable {

    }

    private String schema = "invalid";
    protected List<SchemaDescr> allowedSchemata = List.of();
    private String host = "example.com";
    private Integer port = 0;

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?<schema>[a-z]+)://(?<host>[a-z\\-.]+):(?<port>[0-9]+).*"
    );

    public MutableUrl(SchemaDescr[] allowedSchemata) {
        this.allowedSchemata = List.of(allowedSchemata);
        this.schema = allowedSchemata[0].name();
        this.port = allowedSchemata[0].defaultPort();
    }

    public MutableUrl(MutableUrl another) {
        this.allowedSchemata = another.allowedSchemata;
        this.schema = another.getSchema();
        this.host = another.getHost();
        this.port = another.getPort();
    }

    final public SchemaDescr findSchema(String name) {
        return allowedSchemata.stream()
                .filter(sch -> sch.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    public MutableUrl(SchemaDescr[] allowedSchemata, String url) throws UrlParseException {
        this(allowedSchemata);
        if (url == null || url.equals("")) {
            schema = "invalid";
            host = "invalid";
            port = 0;
        } else {
            try {
                Matcher m = URL_PATTERN.matcher(url);
                m.matches();
                schema = m.group("schema");
                if (findSchema(schema) == null) {
                    throw new UrlParseException(
                            url,
                            "Schema %s not allowed.".formatted(schema)
                    );
                }
                host = m.group("host");
                port = Integer.valueOf(m.group("port"));
            } catch (NumberFormatException ex) {
                throw new UrlParseException(url, ex.getMessage());
            }
        }
    }

    public MutableUrl(SchemaDescr[] allowedSchemata, String schema, String host, Integer port) throws UrlParseException {
        this(allowedSchemata);
        this.schema = schema;
        if (findSchema(schema) == null) {
            throw new UrlParseException(
                    "Schema %s not allowed. Allowed: %s".
                            formatted(schema,
                                    Arrays.toString(allowedSchemata)
                            )
            );
        }
        this.host = host;
        this.port = port;
    }

    public void setSchema(String schema) throws UrlParseException {
        if (findSchema(schema) == null) {
            throw new UrlParseException(
                    "Schema %s not allowed. Allowed: %s".
                            formatted(schema, allowedSchemata.toString())
            );
        }
        this.schema = schema;
    }

    @Override
    public String toString() {
        return "%s://%s:%d".formatted(schema, host, port);
    }
}
