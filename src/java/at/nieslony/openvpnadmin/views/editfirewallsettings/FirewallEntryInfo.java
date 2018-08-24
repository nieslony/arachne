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
package at.nieslony.openvpnadmin.views.editfirewallsettings;

import at.nieslony.openvpnadmin.beans.firewallzone.Entry;
import at.nieslony.openvpnadmin.beans.firewallzone.EntryCreteria;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Claas Nieslony
 */
public class FirewallEntryInfo implements Serializable {

    private Entry entry;
    private boolean isWhoExpanded = false;

    FirewallEntryInfo(Entry e) {
        entry = e;
    }

    public boolean getIsWhoExpanded() {
        return isWhoExpanded;
    }

    public void setIsWhoExpanded(boolean ie) {
        isWhoExpanded = ie;
    }

    private String getAsString(List<? extends EntryCreteria> ec) {
        if (ec.isEmpty()) {
            return "";
        }
        if (ec.size() > 1) {
            return String.format("%s…", ec.get(0).getAsString());
        }
        return ec.get(0).getAsString();
    }

    private String getAsStringExpanded(List<? extends EntryCreteria> ec) {
        List<String> whos = new LinkedList<>();
        ec.forEach((e) -> whos.add(e.getAsString()));
        return String.join("</br>", whos);
    }

    public String getWhoStr() {
        if (isWhoExpanded) {
            return getAsStringExpanded(entry.getWhos());
        } else {
            return getAsString(entry.getWhos());
        }
    }

    public String getWhereStr() {
        return getAsString(entry.getWheres());
    }

    public String getWhatStr() {
        return getAsString(entry.getWhats());
    }

    public Entry getEntry() {
        return entry;
    }

}
