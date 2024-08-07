/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.firewall;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.HasValidator;

/**
 *
 * @author claas
 */
public class EditFirewallWho extends AbstractCompositeField<VerticalLayout, EditFirewallWho, FirewallWho>
        implements HasValidator<FirewallWho> {

    public EditFirewallWho() {
        super(new FirewallWho());
    }

    @Override
    protected void setPresentationValue(FirewallWho value) {

    }
}
