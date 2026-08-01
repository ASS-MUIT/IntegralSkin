package dit.us.derma.task_performer.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Este servicio se encarga de realizar la transacción de suscripción (RAD-86)
 * enviando un POST al Global Subscription UID de UPS-RS
 * (1.2.840.10008.5.1.4.34.5).
 * Subscriber2TaskManager
 */
@Service
public class Subscriber2TaskManager {
    private static final Logger log = LoggerFactory.getLogger(Subscriber2TaskManager.class);

    // UID estándar DICOM para la Suscripción Global a la Lista de Trabajo UPS
    private static final String UPS_GLOBAL_SUBSCRIPTION_UID = "1.2.840.10008.5.1.4.34.5";

    private final RestClient restClient;

    @Value("${taskmanager.dicomweb.url:http://localhost:8080/dcm4chee-arc/aets/DCM4CHEE/rs}")
    private String taskManagerUrl;

    @Value("${taskperformer.aet:MY_DERMA_AI}")
    private String myAeTitle;

    public Subscriber2TaskManager(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * RAD-86: Suscribirse a la lista de trabajo global de UPS en el Task Manager.
     * 
     * @return true si la suscripción fue aceptada (HTTP 200/201).
     */
    public boolean subscribeToWorklist() {
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
}
