package dit.us.derma.task_performer.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class UpsEventChannelManager {

    private static final Logger log = LoggerFactory.getLogger(UpsEventChannelManager.class);

    /**
     * Se ejecuta 1 vez al arrancar la aplicación cuando Spring Boot está 100% listo.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        subscribeToEventChannel();
    }

    /**
     * Método dedicado a la suscripción inicial (RAD-86 / RAD-109).
     */
    private void subscribeToEventChannel() {
        log.info("Aquí iría la suscripción");
    }

    /**
     * Método programado que se ejecuta automáticamente cada 5 minutos (300.000 ms).
     */
    @Scheduled(fixedRate = 300000)
    public void scheduleChannelVerification() {
        verifyChannelHealth();
    }

    /**
     * Método dedicado a la verificación recurrente del canal.
     */
    private void verifyChannelHealth() {
        log.info("Aquí va la verificación del canal");
    }
}
