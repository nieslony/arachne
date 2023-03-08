/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package at.nieslony.arachne;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 *
 * @author claas
 */
@EnableJpaRepositories("at.nieslony.arachne")
@EntityScan("at.nieslony.arachne")
@SpringBootApplication
public class Arachne {

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        System.setProperty("server.servlet.context-path", "/arachne");
        context = SpringApplication.run(Arachne.class, args);
    }

    public static void restart() {
        ApplicationArguments args = context.getBean(ApplicationArguments.class);

        Thread thread = new Thread(() -> {
            context.close();
            context = SpringApplication.run(
                    Arachne.class,
                    args.getSourceArgs()
            );
        });

        thread.setDaemon(false);
        thread.start();
    }
}
