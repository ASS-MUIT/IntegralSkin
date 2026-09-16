package dit.us.derma.task_performer.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@SpringJUnitConfig(UpsEventChannelManagerTest.TestConfig.class)
class UpsEventChannelManagerTest {

    @Configuration
    @EnableScheduling
    @Import(UpsEventChannelManager.class)
    static class TestConfig {
        // Carga únicamente la configuración de scheduling y nuestro servicio
    }

    @Autowired
    private UpsEventChannelManager eventChannelManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    void shouldLoadServiceAndLogMessagesOnStartup(CapturedOutput output) {
        // 1. Verificar que el bean se ha cargado en el contexto de Spring
        assertThat(eventChannelManager).isNotNull();

        // 2. Simular el evento de arranque de la aplicación (ApplicationReadyEvent)
        // Esto dispara manualmente el método anotado con @EventListener(ApplicationReadyEvent.class)
        eventPublisher.publishEvent(new ApplicationReadyEvent(
                org.mockito.Mockito.mock(org.springframework.boot.SpringApplication.class),
                new String[]{},
                org.mockito.Mockito.mock(org.springframework.context.ConfigurableApplicationContext.class),
                java.time.Duration.ZERO
        ));

        // 3. Verificar que los logs contienen los mensajes esperados
        assertThat(output.getOut()).contains("Aquí iría la suscripción");
        assertThat(output.getOut()).contains("Aquí va la verificación del canal");
    }
}
