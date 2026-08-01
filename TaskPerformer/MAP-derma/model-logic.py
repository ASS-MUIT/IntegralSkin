#Misión: Vuestro código debe ir dentro de la función run_inference(dicom_objects).
#Entrada: Recibiréis una lista de objetos pydicom.dataset.Dataset. Podéis acceder a los píxeles con obj.pixel_array.
#Salida Obligatoria: Debéis devolver un diccionario con:

#    "finding": Un string o código con el diagnóstico (ej: "Pneumonia").
#    "probability": Un float de 0 a 1.
#    "heatmap": (Opcional para XAI) Un array de NumPy con las mismas dimensiones que la imagen original, representando la importancia de cada píxel en el diagnóstico.

#No os preocupéis por: Guardar archivos, metadatos del paciente, protocolos de red o formatos DICOM SR. El cascarón se encarga de todo eso para cumplir los estándares del hospital.
