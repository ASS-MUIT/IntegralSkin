package dit.us.derma.task_performer.services;

import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.json.JSONWriter;
import org.dcm4che3.util.UIDUtils;

public class UpsDicomWebClient {

    private final String baseUrl; // ej: "http://pacs.local:8080/dcm4chee-arc/aets/DCM4CHEE/rs"
    private final HttpClient httpClient;

    public UpsDicomWebClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
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
        String jsonPayload = toDicomJson(attrs);

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

    private String toDicomJson(Attributes attrs) {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = Json.createGenerator(sw)) {
            JSONWriter jsonWriter = new JSONWriter(gen);
            jsonWriter.write(attrs);
        }
        return sw.toString();
    }
}
