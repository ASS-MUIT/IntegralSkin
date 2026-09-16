package dit.us.derma.task_performer.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class UpsEventChannelManager {

    private static final Logger log = LoggerFactory.getLogger(UpsEventChannelManager.class);
    // UID estándar DICOM para la Suscripción Global a la Lista de Trabajo UPS
    private static final String UPS_GLOBAL_SUBSCRIPTION_UID = "1.2.840.10008.5.1.4.34.5";
    @Value("${taskmanager.dicomweb.url}")
    private String taskManagerUrl;
    @Value("${taskperformer.aet}")
    private String myAeTitle;
    private final RestClient restClient;

    public UpsEventChannelManager(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * Se ejecuta 1 vez al arrancar la aplicación cuando Spring Boot está 100% listo.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (!subscribeToEventChannel()) {
            log.error("Error al intentar suscribirse al canal de eventos.");
        }
    }

    /**
     * Método dedicado a la suscripción inicial (RAD-86 / RAD-109).
     */
    private boolean subscribeToEventChannel() {
        log.info("Aquí iría la suscripción");
        String endpoint = String.format("%s/workitems/%s/subscribers/%s",
                taskManagerUrl, UPS_GLOBAL_SUBSCRIPTION_UID, myAeTitle);
        boolean result = false;
        log.info("Enviando suscripción RAD-86 a UPS-RS: {}", endpoint);
        try {
            var response = restClient.post()
                    .uri(endpoint)
                    .header("Accept", "application/dicom+json")
                    .contentType(MediaType.parseMediaType("application/dicom+json"))
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Suscripción a tareas completada con éxito en el Task Manager.");
                result = true;

            } else {
                log.warn("El Task Manager respondió con estado: {}", response.getStatusCode());

            }
        } catch (Exception e) {
            log.error("Error al intentar suscribirse al Task Manager: {}", e.getMessage(), e);
        }
        return result;
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
