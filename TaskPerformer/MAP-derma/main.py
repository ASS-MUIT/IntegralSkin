import os
import json
import pydicom
from pydicom.dataset import Dataset, FileDataset
from datetime import datetime
import numpy as np
# Importamos la lógica del otro equipo
try:
    from model_logic import run_inference
except ImportError:
    # Función dummy por si aún no han entregado su parte
    def run_inference(images): 
        return {"finding": "Normal", "probability": 0.1, "heatmap": None}

# Rutas del contrato de artefactos
INPUT_DIR = os.environ.get("INPUT_DIR", "/var/monai/input")
OUTPUT_DIR = os.environ.get("OUTPUT_DIR", "/var/monai/output")

def create_metadata(status, msg=""):
    """Genera el metadata.json para el MONAI Workflow Manager"""
    with open(os.path.join(OUTPUT_DIR, "metadata.json"), "w") as f:
        json.dump({"inference_status": status, "message": msg}, f)

def main():
    try:
        # 1. LEER ENTRADAS: Buscamos archivos DICOM en la carpeta de entrada
        dicom_files = [pydicom.dcmread(os.path.join(INPUT_DIR, f)) 
                       for f in os.listdir(INPUT_DIR) if f.endswith('.dcm')]
        
        if not dicom_files:
            raise Exception("No se encontraron archivos DICOM de entrada.")

        # 2. EJECUTAR MODELO: Pasamos los datos al equipo de IA
        # Ellos deben devolver un diccionario con el hallazgo y el mapa de calor (XAI)
        results = run_inference(dicom_files)

        # 3. ESTANDARIZACIÓN (IHE AIR): Crear DICOM Structured Report (SR)
        # Copiamos metadatos del estudio original para mantener coherencia
        ref_ds = dicom_files
        sr_ds = create_basic_sr(ref_ds, results)
        
        # Guardar el SR en la carpeta de salida
        sr_path = os.path.join(OUTPUT_DIR, f"SR_{sr_ds.SOPInstanceUID}.dcm")
        sr_ds.save_as(sr_path)

        # 4. (Opcional) Guardar Mapa Paramétrico para XAI si existe
        if results.get("heatmap") is not None:
            # Aquí iría la lógica para convertir el heatmap en DICOM Parametric Map
            pass

        # 5. NOTIFICAR ÉXITO al Workflow Manager
        create_metadata("success")
        print("Inferencia completada y resultados estandarizados guardados.")

    except Exception as e:
        create_metadata("failed", str(e))
        print(f"Error en el servicio de IA: {e}")

def create_basic_sr(ref_ds, results):
    """Crea un cascarón de DICOM SR siguiendo IHE AIR (TID 1500)"""
    # Configuración básica del archivo
    file_meta = Dataset()
    file_meta.MediaStorageSOPClassUID = '1.2.840.10008.5.1.4.1.1.88.34' # Comprehensive 3D SR
    file_meta.MediaStorageSOPInstanceUID = pydicom.uid.generate_uid()
    file_meta.ImplementationClassUID = pydicom.uid.generate_uid()

    ds = FileDataset("SR_output.dcm", {}, file_meta=file_meta, preamble=b"\0" * 128)
    
    # Copiar metadatos del paciente y estudio (Requisito AIW-I)
    ds.PatientName = ref_ds.PatientName
    ds.PatientID = ref_ds.PatientID
    ds.StudyInstanceUID = ref_ds.StudyInstanceUID
    ds.SeriesInstanceUID = pydicom.uid.generate_uid()
    ds.SOPInstanceUID = file_meta.MediaStorageSOPInstanceUID
    ds.SOPClassUID = file_meta.MediaStorageSOPClassUID
    ds.Modality = "SR"
    ds.ContentDate = datetime.now().strftime('%Y%m%d')
    ds.ContentTime = datetime.now().strftime('%H%M%S')

    # Identificación del Algoritmo (Contributing Equipment Sequence) [9, 10]
    contrib_item = Dataset()
    contrib_item.Manufacturer = "MiOrganizacion_AI"
    contrib_item.ManufacturerModelName = "Modelo_XAI_v1"
    contrib_item.SoftwareVersions = "1.0.0"
    contrib_item.DeviceUID = pydicom.uid.generate_uid()
    ds.ContributingEquipmentSequence = [contrib_item]

    # Aquí se añadiría la secuencia de contenido (Content Sequence) con el TID 1500
    # El hallazgo cualitativo (results['finding']) se codifica aquí
    return ds

if __name__ == "__main__":
    main()