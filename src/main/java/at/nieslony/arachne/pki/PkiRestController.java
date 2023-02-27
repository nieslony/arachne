/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.pki;

import java.security.PrivateKey;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author claas
 */
@RestController
@RequestMapping("/api/pki")
public class PkiRestController {
    @Autowired
    CertificateRepository certificateRepository;

    @Autowired
    Pki pki;

    @Data
    public class UserCertInfo {
        UserCertInfo(CertificateModel model) {
            this.subject = model.getSubject();
            this.validFrom = model.getValidFrom();
            this.validTo = model.getValidTo();
            this.isRevoked = model.getIsRevoked();
            this.certType = model.getCertType();
        }

        private String subject;
        private Date validFrom;
        private Date validTo;
        private Boolean isRevoked;
        private CertificateModel.CertType certType;
    }

    @GetMapping("/user_certs")
    public Map<String, Object> findAllUserCerts() {
        Map<String, Object> result = new HashMap<>();

        pki.getRootCertAsBase64();

        List<UserCertInfo> certs = new LinkedList<>();
        for (CertificateModel model: certificateRepository.findAll()) {
            certs.add(new UserCertInfo(model));
            PrivateKey pk = model.getKeyModel().getPrivateKey();
        }

        result.put("data", certs);
        return result;
    }
}
