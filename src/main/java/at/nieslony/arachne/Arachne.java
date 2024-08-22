/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package at.nieslony.arachne;

import com.vaadin.flow.component.page.AppShellConfigurator;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class Arachne implements AppShellConfigurator {

    private static final AtomicReference< ConfigurableApplicationContext> context
            = new AtomicReference<>();

    public static void main(String[] args) {
        context.set(SpringApplication.run(Arachne.class, args));
    }

    public static void restart() {
        ConfigurableApplicationContext ctx = context.get();
        ApplicationArguments args = ctx.getBean(ApplicationArguments.class);

        Thread thread = new Thread(() -> {
            ctx.close();
            ConfigurableApplicationContext ctx2 = SpringApplication.run(
                    Arachne.class,
                    args.getSourceArgs()
            );
            context.set(ctx2);
        });

        thread.setDaemon(false);
        thread.start();
    }
}
