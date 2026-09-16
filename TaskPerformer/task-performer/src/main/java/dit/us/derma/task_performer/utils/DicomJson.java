package dit.us.derma.task_performer.utils;

import java.io.StringReader;
import java.io.StringWriter;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.json.JSONReader;
import org.dcm4che3.json.JSONWriter;

import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;

/**
 * Utilidades de conversión entre DICOM JSON (application/dicom+json) y
 * org.dcm4che3.data.Attributes.
 */
public final class DicomJson {

    private DicomJson() {
        // Clase de utilidad
    }

    /**
     * Convierte un String DICOM JSON a Attributes. Admite tanto un objeto JSON
     * suelto como un array con un único dataset (formato habitual de UPS-RS).
     */
    public static Attributes toAttributes(String json) {
        JsonParser parser = Json.createParser(new StringReader(json));
        JSONReader jsonReader = new JSONReader(parser);
        return jsonReader.readDataset(null);
    }

    /**
     * Serializa un dataset a DICOM JSON.
     */
    public static String toJson(Attributes attrs) {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = Json.createGenerator(sw)) {
            JSONWriter jsonWriter = new JSONWriter(gen);
            jsonWriter.write(attrs);
        }
        return sw.toString();
    }
}
