/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package at.nieslony.arachne.settings;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author claas
 */
public interface SettingsRepository extends JpaRepository<SettingsModel, Long> {

    Optional<SettingsModel> findBySetting(String setting);

    Optional<SettingsModel> deleteBySetting(String setting);
}
