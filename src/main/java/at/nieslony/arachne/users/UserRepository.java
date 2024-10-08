/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package at.nieslony.arachne.users;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author claas
 */
public interface UserRepository extends JpaRepository<UserModel, Long> {

    UserModel findByUsername(String username);

    UserModel findByUsernameAndExternalProvider(
            String username,
            String externalProvider
    );
}
