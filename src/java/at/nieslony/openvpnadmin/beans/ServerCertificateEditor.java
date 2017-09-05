/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

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

    public void setValidTime(Integer time);
    public void setValidTimeUnit(String unit);
}
