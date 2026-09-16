package dit.us.derma.task_performer.controllers;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dit.us.derma.task_performer.model.UpsWorkItem;
import dit.us.derma.task_performer.services.UpsDicomWebClient;
import dit.us.derma.task_performer.utils.DicomJson;

@RestController
@RequestMapping("/api/v1/ups")
public class WorkItemNotificationController {

    private static final Logger log = LoggerFactory.getLogger(WorkItemNotificationController.class);

    private final UpsDicomWebClient upsClient;

    public WorkItemNotificationController(UpsDicomWebClient upsClient) {
        this.upsClient = upsClient;
    }

    /**
     * Endpoint para recibir notificaciones de eventos (UPS Event Reports).
     *
     * Corresponde con SendUpsNotification RAD-87
     *
     * @param jsonPayload Cuerpo DICOM+JSON enviado por el Task Manager.
     */
    @PostMapping(value = "/events", consumes = { "application/dicom+json", "application/json" })
    public ResponseEntity<Void> receiveUpsNotification(@RequestBody String jsonPayload) {
        log.info("Notificación de evento recibida del Task Manager.");

        try {
            // Parsear el JSON recibido a un objeto Attributes de dcm4che
            Attributes eventAttributes = DicomJson.toAttributes(jsonPayload);

            // Construcción inicial del workitem con lo que trae la propia notificación
            UpsWorkItem workItem = UpsWorkItem.from(eventAttributes);
            String eventTypeID = eventAttributes.getString(Tag.EventTypeID);

            log.info("Evento procesado - Task UID: {}, Estado: {}, EventTypeID: {}",
                    workItem.workitemUID(), workItem.procedureStepState(), eventTypeID);

            if (workItem.workitemUID() == null) {
                log.warn("La notificación no incluye el UID del workitem; no es posible recuperarlo.");
                return ResponseEntity.ok().build();
            }

            // La notificación solo trae un subconjunto de atributos: se completa el
            // workitem recuperándolo del Task Manager (RAD-83).
            workItem = retrieveFullWorkItem(workItem);

            processWorkItem(workItem, eventTypeID);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error al procesar la notificación DICOM JSON: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Recupera el workitem completo mediante RAD-83. Si el Task Manager no
     * responde, se devuelve el workitem parcial construido con la notificación
     * para no perder el evento.
     */
    private UpsWorkItem retrieveFullWorkItem(UpsWorkItem notifiedWorkItem) {
        String workitemUID = notifiedWorkItem.workitemUID();
        try {
            UpsWorkItem workItem = upsClient.retrieveWorkitem(workitemUID);
            log.info("Workitem recuperado del Task Manager (RAD-83): {}", workItem.summary());
            return workItem;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Recuperación del workitem {} interrumpida (RAD-83).", workitemUID);
        } catch (Exception e) {
            log.warn("No se pudo recuperar el workitem {} (RAD-83): {}. Se usan los datos de la notificación.",
                    workitemUID, e.getMessage());
        }
        return notifiedWorkItem;
    }

    /**
     * Punto único de entrega del workitem ya construido: desde aquí se pasa al
     * resto del flujo (persistencia, reclamo RAD-82, ejecución del MAP de MONAI).
     */
    private void processWorkItem(UpsWorkItem workItem, String eventTypeID) {
        log.info("Workitem disponible para el flujo (EventTypeID {}): {}", eventTypeID, workItem.summary());

        if (workItem.isScheduled()) {
            log.info("El workitem {} está en estado SCHEDULED: candidato a ser reclamado.", workItem.workitemUID());
        }

        // TODO: persistir el workitem (workItem.toDicomJson() conserva el dataset
        // completo) y, si el estado es SCHEDULED, invocar el flujo de reclamo
        // (RAD-82) y la ejecución del MAP de MONAI.
    }
}
