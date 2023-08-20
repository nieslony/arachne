/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.pki;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author claas
 */
public interface CertificateRepository extends JpaRepository<CertificateModel, Long> {

    List<CertificateModel> findBySubjectIgnoreCaseAndCertTypeOrderByValidToDesc(
            String subject,
            CertificateModel.CertType certType);

    List<CertificateModel> findBySubjectIgnoreCaseOrderByValidToDesc(
            String subject);

    List<CertificateModel> findByRevocationDateIsNotNullOrderByValidToDesc();
}
