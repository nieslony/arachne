/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn.vpnsite;

import at.nieslony.arachne.utils.net.NetMask;
import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.HasValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * @author claas
 */
public class EditRemoteNetworks extends AbstractCompositeField<VerticalLayout, EditRemoteNetworks, RemoteNetwork>
        implements HasValidator<RemoteNetwork> {

    private final Binder<RemoteNetwork> binder;

    public EditRemoteNetworks() {
        super(new RemoteNetwork());

        binder = new Binder<>();

        TextField network = new TextField("Network");
        network.setValueChangeMode(ValueChangeMode.EAGER);
        binder.bind(
                network,
                RemoteNetwork::getAddress,
                RemoteNetwork::setAddress
        );

        Select<NetMask> networkMask = new Select<>();
        networkMask.setItems(
                IntStream
                        .range(1, 32)
                        .boxed()
                        .map(i -> new NetMask(i))
                        .collect(Collectors.toList())
        );
        networkMask.setLabel("Subnet Mask");
        binder.forField(networkMask)
                .bind(
                        (source) -> {
                            int mask = source.getMask();
                            return new NetMask(mask);
                        },
                        (s, v) -> {
                            s.setMask(v.getBits());
                        }
                );

        HorizontalLayout netwokLayout = new HorizontalLayout();
        netwokLayout.add(network, networkMask);

        TextField name = new TextField("Name");

        getContent().add(
                netwokLayout,
                name
        );
    }

    @Override
    protected void setPresentationValue(RemoteNetwork remoteNetwork) {
        binder.setBean(remoteNetwork);
        binder.validate();
    }

}
