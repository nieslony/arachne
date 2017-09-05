/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.utils.pki;

import java.io.StringWriter;
import org.bouncycastle.asn1.x500.X500Name;

/**
 *
 * @author claas
 */
public class CaHelper {
    public static X500Name getSubjectDN(String title,
            String commonName,
            String organizationalUnit,
            String organization,
            String city,
            String state,
            String country) {
                StringWriter sw = new StringWriter();
        if (title != null && !title.isEmpty())
            sw.append("T=" + title + ",");
        sw.append("CN=" + commonName);
        if (organizationalUnit != null && !organizationalUnit.isEmpty())
            sw.append(", OU=" + organizationalUnit);
        if (organization != null && !organization.isEmpty())
            sw.append(", O=" + organization);
        if (city != null && !city.isEmpty())
            sw.append(", L=" + city);
        if (state != null && !state.isEmpty())
            sw.append(", ST=" + state);
        if (country != null && !country.isEmpty())
            sw.append(", C=" + country);

        return new X500Name(sw.toString());
    }

    public static String getKeyAlgo(String s) {
        String[] components = s.split("with");

        if (components != null && components.length == 2)
            return components[1];

        return "unknown";
    }
}
