package dit.us.derma.task_performer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dit.us.derma.task_performer.utils.DicomJson;

class UpsWorkItemTest {

    private static final String WORKITEM_JSON = """
            [{
              "00080018": {"vr":"UI","Value":["1.2.3.4.5"]},
              "00100010": {"vr":"PN","Value":[{"Alphabetic":"DOE^JOHN"}]},
              "00100020": {"vr":"LO","Value":["PAT-001"]},
              "00741000": {"vr":"CS","Value":["SCHEDULED"]},
              "00741204": {"vr":"LO","Value":["Segmentacion piel"]},
              "00741202": {"vr":"LO","Value":["DERMA_AI"]},
              "00404005": {"vr":"DT","Value":["20260916120000"]},
              "00404041": {"vr":"CS","Value":["READY"]},
              "00404018": {"vr":"SQ","Value":[{
                 "00080100": {"vr":"SH","Value":["123456"]},
                 "00080102": {"vr":"SH","Value":["DCM"]},
                 "00080104": {"vr":"LO","Value":["AI Skin Analysis"]}
              }]},
              "00404021": {"vr":"SQ","Value":[{
                 "0040E020": {"vr":"CS","Value":["DICOM"]},
                 "0020000D": {"vr":"UI","Value":["1.2.3.4.5.6"]},
                 "0020000E": {"vr":"UI","Value":["1.2.3.4.5.6.7"]},
                 "00081199": {"vr":"SQ","Value":[{
                    "00081150": {"vr":"UI","Value":["1.2.840.10008.5.1.4.1.1.77.1.4"]},
                    "00081155": {"vr":"UI","Value":["1.2.3.4.5.6.7.8"]}
                 }]},
                 "0040E025": {"vr":"SQ","Value":[{
                    "00081190": {"vr":"UR","Value":["http://localhost:8080/dcm4chee-arc/aets/DCM4CHEE/rs/studies/1.2.3.4.5.6"]}
                 }]}
              }]}
            }]
            """;

    @Test
    void mapeaLosAtributosClaveDelWorkitem() {
        UpsWorkItem workItem = UpsWorkItem.from(DicomJson.toAttributes(WORKITEM_JSON));

        assertEquals("1.2.3.4.5", workItem.workitemUID());
        assertEquals("SCHEDULED", workItem.procedureStepState());
        assertTrue(workItem.isScheduled());
        assertEquals("Segmentacion piel", workItem.procedureStepLabel());
        assertEquals("DERMA_AI", workItem.worklistLabel());
        assertEquals("READY", workItem.inputReadinessState());
        assertEquals("20260916120000", workItem.scheduledStartDateTime());
        assertEquals("PAT-001", workItem.patient().patientID());
        assertEquals("DOE^JOHN", workItem.patient().patientName());
        assertEquals("AI Skin Analysis", workItem.scheduledWorkitemCode().codeMeaning());

        assertEquals(1, workItem.inputInformation().size());
        UpsWorkItem.InputInstances input = workItem.inputInformation().get(0);
        assertEquals("1.2.3.4.5.6", input.studyInstanceUID());
        assertEquals("1.2.3.4.5.6.7", input.seriesInstanceUID());
        assertEquals(java.util.List.of("1.2.3.4.5.6.7.8"), input.sopInstanceUIDs());
        assertTrue(input.retrieveUrl().endsWith("/studies/1.2.3.4.5.6"));

        assertTrue(workItem.toDicomJson().contains("1.2.3.4.5"));
        System.out.println("RESUMEN: " + workItem.summary());
    }

    @Test
    void usaElAffectedSopInstanceUidDeLaNotificacion() {
        String eventJson = """
                {
                  "00001000": {"vr":"UI","Value":["1.2.9.9.9"]},
                  "00001002": {"vr":"US","Value":[1]},
                  "00741000": {"vr":"CS","Value":["SCHEDULED"]}
                }
                """;

        UpsWorkItem workItem = UpsWorkItem.from(DicomJson.toAttributes(eventJson));

        assertEquals("1.2.9.9.9", workItem.workitemUID());
        assertTrue(workItem.isScheduled());
        assertTrue(workItem.inputInformation().isEmpty());
    }
}
