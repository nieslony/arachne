/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
