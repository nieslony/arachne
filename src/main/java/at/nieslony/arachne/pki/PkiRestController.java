/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.pki;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.annotation.security.RolesAllowed;
import java.security.PrivateKey;
import java.security.cert.X509CRL;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author claas
 */
@RestController
public class PkiRestController {

    @Autowired
    CertificateRepository certificateRepository;

    @Autowired
    Pki pki;

    @Getter
    @Setter
    public class UserCertInfo {

        UserCertInfo(CertificateModel model) {
            this.subject = model.getSubject();
            this.validFrom = model.getValidFrom();
            this.validTo = model.getValidTo();
            this.isRevoked = model.getRevocationDate() == null;
            this.certType = model.getCertType();
        }

        private String subject;
        private Date validFrom;
        private Date validTo;
        private Boolean isRevoked;
        private CertificateModel.CertType certType;
    }

    @GetMapping("/api/pki/user_certs")
    @RolesAllowed(value = {"ADMIN"})
    public Map<String, Object> getAllUserCerts() {
        Map<String, Object> result = new HashMap<>();

        pki.getRootCertAsBase64();

        List<UserCertInfo> certs = new LinkedList<>();
        for (CertificateModel model : certificateRepository.findAll()) {
            certs.add(new UserCertInfo(model));
            PrivateKey pk = model.getKeyModel().getPrivateKey();
        }

        result.put("data", certs);
        return result;
    }

    @GetMapping("/crl.pem")
    @AnonymousAllowed
    public String getCrl() {
        X509CRL crl = pki.getCrl(() -> {
            return certificateRepository.findByRevocationDateIsNotNullOrderByValidToDesc();
        });

        return Pki.asBase64(crl);
    }
}
