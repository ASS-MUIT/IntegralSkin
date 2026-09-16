package dit.us.derma.task_performer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;

import dit.us.derma.task_performer.utils.DicomJson;

/**
 * Representación en memoria de un UPS Workitem (Unified Procedure Step).
 *
 * <p>
 * Contiene los atributos que necesita el Task Performer para decidir y ejecutar
 * la tarea, y además conserva el dataset DICOM completo ({@link #attributes()})
 * para no perder información al pasarlo a otro componente o al persistirlo
 * ({@link #toDicomJson()}).
 */
public record UpsWorkItem(
        String workitemUID,
        String transactionUID,
        String procedureStepState,
        String procedureStepLabel,
        String worklistLabel,
        String priority,
        String inputReadinessState,
        String scheduledStartDateTime,
        Patient patient,
        Code scheduledWorkitemCode,
        List<InputInstances> inputInformation,
        Attributes attributes) {

    /** Estado de un workitem que todavía no ha sido reclamado. */
    public static final String STATE_SCHEDULED = "SCHEDULED";

    /** Datos identificativos del paciente asociado a la tarea. */
    public record Patient(
            String patientID,
            String issuerOfPatientID,
            String patientName,
            String birthDate,
            String sex) {
    }

    /** Código DICOM (valor, esquema de codificación y significado). */
    public record Code(
            String codeValue,
            String codingSchemeDesignator,
            String codeMeaning) {
    }

    /**
     * Instancias de entrada de la tarea (Input Information Sequence): las
     * imágenes que el MAP de MONAI deberá recuperar y procesar.
     */
    public record InputInstances(
            String typeOfInstances,
            String studyInstanceUID,
            String seriesInstanceUID,
            List<String> sopInstanceUIDs,
            String retrieveUrl,
            String retrieveAeTitle) {

        public InputInstances {
            sopInstanceUIDs = sopInstanceUIDs == null
                    ? List.of()
                    : Collections.unmodifiableList(new ArrayList<>(sopInstanceUIDs));
        }
    }

    public UpsWorkItem {
        inputInformation = inputInformation == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(inputInformation));
    }

    /**
     * Construye el workitem a partir de un dataset DICOM, ya proceda de la
     * respuesta a RAD-83 (Get UPS Workitem) o de la propia notificación RAD-87.
     */
    public static UpsWorkItem from(Attributes attrs) {
        return new UpsWorkItem(
                extractWorkitemUID(attrs),
                attrs.getString(Tag.TransactionUID),
                attrs.getString(Tag.ProcedureStepState),
                attrs.getString(Tag.ProcedureStepLabel),
                attrs.getString(Tag.WorklistLabel),
                attrs.getString(Tag.ScheduledProcedureStepPriority),
                attrs.getString(Tag.InputReadinessState),
                attrs.getString(Tag.ScheduledProcedureStepStartDateTime),
                extractPatient(attrs),
                extractCode(attrs.getNestedDataset(Tag.ScheduledWorkitemCodeSequence)),
                extractInputInformation(attrs),
                attrs);
    }

    /**
     * El UID del workitem viaja como SOP Instance UID en la respuesta a RAD-83,
     * pero las notificaciones de evento (RAD-87) pueden traerlo como Affected o
     * Requested SOP Instance UID.
     */
    private static String extractWorkitemUID(Attributes attrs) {
        String uid = attrs.getString(Tag.SOPInstanceUID);
        if (uid == null) {
            uid = attrs.getString(Tag.AffectedSOPInstanceUID);
        }
        if (uid == null) {
            uid = attrs.getString(Tag.RequestedSOPInstanceUID);
        }
        return uid;
    }

    private static Patient extractPatient(Attributes attrs) {
        return new Patient(
                attrs.getString(Tag.PatientID),
                attrs.getString(Tag.IssuerOfPatientID),
                attrs.getString(Tag.PatientName),
                attrs.getString(Tag.PatientBirthDate),
                attrs.getString(Tag.PatientSex));
    }

    private static Code extractCode(Attributes codeItem) {
        if (codeItem == null) {
            return null;
        }
        return new Code(
                codeItem.getString(Tag.CodeValue),
                codeItem.getString(Tag.CodingSchemeDesignator),
                codeItem.getString(Tag.CodeMeaning));
    }

    private static List<InputInstances> extractInputInformation(Attributes attrs) {
        Sequence inputSequence = attrs.getSequence(Tag.InputInformationSequence);
        if (inputSequence == null || inputSequence.isEmpty()) {
            return List.of();
        }
        List<InputInstances> inputs = new ArrayList<>(inputSequence.size());
        for (Attributes item : inputSequence) {
            inputs.add(new InputInstances(
                    item.getString(Tag.TypeOfInstances),
                    item.getString(Tag.StudyInstanceUID),
                    item.getString(Tag.SeriesInstanceUID),
                    extractReferencedSopInstanceUIDs(item),
                    extractRetrieveUrl(item),
                    extractRetrieveAeTitle(item)));
        }
        return inputs;
    }

    private static List<String> extractReferencedSopInstanceUIDs(Attributes item) {
        Sequence referencedSops = item.getSequence(Tag.ReferencedSOPSequence);
        if (referencedSops == null || referencedSops.isEmpty()) {
            return List.of();
        }
        List<String> uids = new ArrayList<>(referencedSops.size());
        for (Attributes referencedSop : referencedSops) {
            String uid = referencedSop.getString(Tag.ReferencedSOPInstanceUID);
            if (uid != null) {
                uids.add(uid);
            }
        }
        return uids;
    }

    /**
     * La URL de recuperación puede venir directamente en el item o dentro de la
     * secuencia de recuperación DICOMweb (WADO-RS o WADO-URI).
     */
    private static String extractRetrieveUrl(Attributes item) {
        String url = item.getString(Tag.RetrieveURL);
        if (url == null) {
            url = nestedString(item, Tag.WADORSRetrievalSequence, Tag.RetrieveURL);
        }
        if (url == null) {
            url = nestedString(item, Tag.WADORetrievalSequence, Tag.RetrieveURL);
        }
        return url;
    }

    private static String extractRetrieveAeTitle(Attributes item) {
        String aet = item.getString(Tag.RetrieveAETitle);
        if (aet == null) {
            aet = nestedString(item, Tag.DICOMRetrievalSequence, Tag.RetrieveAETitle);
        }
        return aet;
    }

    private static String nestedString(Attributes item, int sequenceTag, int tag) {
        Attributes nested = item.getNestedDataset(sequenceTag);
        return nested == null ? null : nested.getString(tag);
    }

    /** Indica si la tarea sigue pendiente de ser reclamada (RAD-82). */
    public boolean isScheduled() {
        return STATE_SCHEDULED.equalsIgnoreCase(procedureStepState);
    }

    /** Serializa el workitem completo a DICOM JSON (útil para persistirlo). */
    public String toDicomJson() {
        return DicomJson.toJson(attributes);
    }

    /** Resumen legible para trazas, sin volcar el dataset completo. */
    public String summary() {
        return "UpsWorkItem[uid=" + workitemUID
                + ", estado=" + procedureStepState
                + ", etiqueta=" + procedureStepLabel
                + ", paciente=" + (patient == null ? null : patient.patientID())
                + ", entradas=" + inputInformation.size() + "]";
    }
}
