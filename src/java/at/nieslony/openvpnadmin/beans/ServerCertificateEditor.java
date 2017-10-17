/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.TimeUnit;
import org.bouncycastle.asn1.x500.X500Name;

/**
 *
 * @author claas
 */
public interface ServerCertificateEditor {
    public void setTitle(String t);
    public void setCommonName(String cn);
    public void setOrganization(String o);
    public void setOrganizationalUnit(String ou);
    public void setCity(String l);
    public void setState(String s);
    public void setCountry(String c);

    public void setKeySize(Integer keySize);
    public void setSignatureAlgorithm(String algoName);

    public void setValidTime(Integer time);
    public void setValidTimeUnit(TimeUnit unit);

    public String getSignatureAlgorithm();
    public Integer getKeySize();
    public TimeUnit getValidTimeUnit();
    public Integer getValidTime();
    public X500Name getSubjectDn();
}
