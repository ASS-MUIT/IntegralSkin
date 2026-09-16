package dit.us.derma.task_performer.services;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.util.UIDUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dit.us.derma.task_performer.model.UpsWorkItem;
import dit.us.derma.task_performer.utils.DicomJson;

@Service
public class UpsDicomWebClient {

    private final String baseUrl; // ej: "http://pacs.local:8080/dcm4chee-arc/aets/DCM4CHEE/rs"
    private final HttpClient httpClient;

    public UpsDicomWebClient(@Value("${taskmanager.dicomweb.url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Implementación encapsulada de la transacción IHE RAD-83 (Get UPS Workitem),
     * es decir, el "Retrieve Workitem" de UPS-RS: GET {baseUrl}/workitems/{uid}
     *
     * @param workitemUID SOP Instance UID de la tarea, tal y como llega en la
     *                    notificación RAD-87.
     * @return el workitem completo, listo para pasarlo al flujo de ejecución o
     *         para persistirlo.
     */
    public UpsWorkItem retrieveWorkitem(String workitemUID) throws IOException, InterruptedException {
        URI uri = URI.create(baseUrl + "/workitems/" + URLEncoder.encode(workitemUID, StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/dicom+json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Error en RAD-83 (Get Workitem): HTTP " + response.statusCode() + " - " + response.body());
        }

        Attributes attrs = DicomJson.toAttributes(response.body());

        // El Task Manager no está obligado a devolver el SOP Instance UID dentro del
        // dataset, así que lo fijamos con el UID solicitado si no viene.
        if (attrs.getString(Tag.SOPInstanceUID) == null) {
            attrs.setString(Tag.SOPInstanceUID, VR.UI, workitemUID);
        }

        return UpsWorkItem.from(attrs);
    }

    /**
     * Implementación encapsulada de la transacción IHE RAD-82 (Claim UPS Workitem)
     */
    public String claimWorkitem(String workitemUID) throws Exception {
        // 1. Generar Transaction UID obligatorio para RAD-82
        String transactionUID = UIDUtils.createUID();

        // 2. Construir el Dataset DICOM usando dcm4che
        Attributes attrs = new Attributes();
        attrs.setString(Tag.TransactionUID, VR.UI, transactionUID);
        attrs.setString(Tag.ProcedureStepState, VR.CS, "IN PROGRESS");

        // 3. Serializar a DICOM JSON usando dcm4che-json
        String jsonPayload = DicomJson.toJson(attrs);

        // 4. Armar la petición HTTP según la norma DICOMweb UPS-RS
        URI uri = URI.create(baseUrl + "/workitems/" + workitemUID + "/state?transaction=" + transactionUID);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/dicom+json")
                .header("Accept", "application/dicom+json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 204) {
            // Devuelve el Transaction UID asignado para usarlo en RAD-84 y RAD-85
            return transactionUID;
        } else {
            throw new RuntimeException("Error en RAD-82 (Claim): HTTP " + response.statusCode() + " - " + response.body());
        }
    }
}
