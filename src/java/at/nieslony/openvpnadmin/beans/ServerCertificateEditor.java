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
