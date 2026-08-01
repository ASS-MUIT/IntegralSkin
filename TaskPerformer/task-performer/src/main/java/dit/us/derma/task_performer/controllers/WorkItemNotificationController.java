package dit.us.derma.task_performer.controllers;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.json.JSONReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.json.Json;
import jakarta.json.stream.JsonParser;
import java.io.StringReader;

@RestController
@RequestMapping("/api/v1/ups")
public class WorkItemNotificationController {

    private static final Logger log = LoggerFactory.getLogger(WorkItemNotificationController.class);

    /**
     * Endpoint para recibir notificaciones de eventos (UPS Event Reports).
     * 
     * @param jsonPayload Cuerpo DICOM+JSON enviado por el Task Manager.
     */
    @PostMapping(value = "/events", consumes = { "application/dicom+json", "application/json" })
    public ResponseEntity<Void> receiveUpsEventNotification(@RequestBody String jsonPayload) {
        log.info("Notificación de evento recibida del Task Manager.");

        try {
            // Parsear el JSON recibido a un objeto Attributes de dcm4che
            Attributes eventAttributes = parseDicomJson(jsonPayload);

            // Extraer metadatos de la tarea usando las etiquetas nativas (Tag) de dcm4che
            String sopInstanceUID = eventAttributes.getString(Tag.SOPInstanceUID);
            String procedureStepState = eventAttributes.getString(Tag.ProcedureStepState);
            String eventTypeID = eventAttributes.getString(Tag.EventTypeID);

            log.info("Evento procesado - Task UID: {}, Estado: {}, EventTypeID: {}",
                    sopInstanceUID, procedureStepState, eventTypeID);

            // TODO: Si el estado es SCHEDULED, invocar el flujo de reclamo (RAD-82) y
            // ejecución del MAP de MONAI

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error al procesar la notificación DICOM JSON: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Utilidad para convertir un String DICOM JSON a org.dcm4che3.data.Attributes
     */
    private Attributes parseDicomJson(String json) {
        JsonParser parser = Json.createParser(new StringReader(json));
        JSONReader jsonReader = new JSONReader(parser);
        return jsonReader.readDataset(null);
    }
}
