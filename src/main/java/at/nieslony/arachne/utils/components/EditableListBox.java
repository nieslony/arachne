/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils.components;

import com.vaadin.flow.component.textfield.TextField;

/**
 *
 * @author claas
 */
public class EditableListBox extends GenericEditableListBox<String, TextField> {

    public EditableListBox(String label) {
        super(label, new TextField());
    }
}
