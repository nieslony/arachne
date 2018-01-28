/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.ArachneStatics;
import java.io.Serializable;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

/**
 *
 * @author claas
 */
@ManagedBean
@ApplicationScoped
public class AboutArachne
        extends ArachneStatics
        implements Serializable
{

    /**
     * Creates a new instance of AboutArachne
     */
    public AboutArachne() {
    }
}
