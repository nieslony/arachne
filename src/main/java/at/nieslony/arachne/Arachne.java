/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package at.nieslony.arachne;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.theme.Theme;
import java.util.List;
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
@Theme(value = "arachne")
@SpringBootApplication
@Push(transport = Transport.LONG_POLLING, value = PushMode.AUTOMATIC)
public class Arachne implements AppShellConfigurator {

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
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

    @Override
    public void configurePage(AppShellSettings settings) {
        List
                .of(16, 32, 48, 64, 120, 144, 152, 180, 192, 512)
                .forEach((size) -> {
                    String fileName = "icons/arachne.png?size=%d".formatted(size);
                    String sizesString = "%dx%d".formatted(size, size);
                    settings.addFavIcon("icon", fileName, sizesString);
                });
    }
}
