/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils.validators;

import at.nieslony.arachne.utils.net.DnsServiceName;
import at.nieslony.arachne.utils.net.NetUtils;
import at.nieslony.arachne.utils.net.SrvRecord;
import at.nieslony.arachne.utils.net.TransportProtocol;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import javax.naming.NamingException;

/**
 *
 * @author claas
 */
public class ServiceRecordValidator implements Validator<DnsServiceName> {

    private final HasValue<?, String> domainProvider;
    private final HasValue<?, TransportProtocol> protocolProvider;

    public ServiceRecordValidator(
            HasValue<?, String> domainProvider,
            HasValue<?, TransportProtocol> protocolProvider
    ) {
        this.domainProvider = domainProvider;
        this.protocolProvider = protocolProvider;
    }

    @Override
    public ValidationResult apply(DnsServiceName service, ValueContext vc) {
        String domain = domainProvider.getValue();
        TransportProtocol protocol = protocolProvider.getValue();

        if (service == null || service.name() == null || service.name().isEmpty()) {
            return ValidationResult.error("Service must not be empty");
        }
        if (DnsServiceName.getService(service.name()) == null) {
            return ValidationResult.error("Service name unknown");
        }
        if (protocol == null) {
            return ValidationResult.error("Please select a protocol");
        }
        if (domain == null || domain.isEmpty()) {
            return ValidationResult.error("Please provide a domain");
        }

        String queryString = "_%s._%s.%s"
                .formatted(
                        service.name(),
                        protocol.toString().toLowerCase(),
                        domain);

        try {
            List<SrvRecord> srvRecs = NetUtils.srvLookup(
                    service.name(),
                    protocol,
                    domain
            );
            for (SrvRecord srvRec : srvRecs) {
                InetAddress[] addrs = InetAddress.getAllByName(srvRec.getHostname());
                if (addrs.length == 0) {
                    return ValidationResult.error("No SRV records %s found".formatted(queryString));
                }
            }
        } catch (NamingException | UnknownHostException ex) {
            return ValidationResult.error(
                    "Cannot find service record %s: %s"
                            .formatted(
                                    queryString,
                                    ex.getMessage()
                            )
            );
        }

        return ValidationResult.ok();
    }

}
