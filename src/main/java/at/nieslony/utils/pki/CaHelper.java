/*
 * Copyright (C) 2018 Claas Nieslony
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

package at.nieslony.utils.pki;

import java.io.IOException;
import java.io.StringWriter;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.DSAKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;

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
        String[] components = s.split("WITH");

        if (components != null && components.length == 2)
            return components[1];

        return "unknown";
    }

    private static String getX500NamePart(ASN1ObjectIdentifier partName, X500Name subject) {
        RDN[] part = subject.getRDNs(partName);

        return part != null && part.length != 0 ?
                IETFUtils.valueToString(part[0].getFirst().getValue())
                : null;
    }

    public static String getTitle(X500Name subject) {
        return getX500NamePart(BCStyle.T, subject);
    }

    public static String getCn(X500Name subject) {
        return getX500NamePart(BCStyle.CN, subject);
    }

    public static String getOrganization(X500Name subject) {
        return getX500NamePart(BCStyle.O, subject);
    }

    public static String getCity(X500Name subject) {
        return getX500NamePart(BCStyle.L, subject);
    }

    public static String getState(X500Name subject) {
        return getX500NamePart(BCStyle.ST, subject);
    }

    public static String getCountry(X500Name subject) {
        return getX500NamePart(BCStyle.C, subject);
    }

    public static int getKeySize(SubjectPublicKeyInfo keyInfo)
            throws IOException
    {
        int keySize;

        AsymmetricKeyParameter keyParam = PublicKeyFactory.createKey(keyInfo);
        if (keyParam instanceof RSAKeyParameters) {
            keySize = ((RSAKeyParameters)keyParam).getModulus().bitLength();
        }
        else if (keyParam instanceof DSAKeyParameters) {
            keySize = ((DSAKeyParameters)keyParam).getParameters().getP().bitLength();
        }
        else if (keyParam instanceof ECPublicKeyParameters) {
            keySize = ((ECPublicKeyParameters)keyParam).getParameters().getN().bitLength();
        }
        else
            keySize = 0;

        return keySize;
    }
}
