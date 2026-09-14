# IHE-AIW-I Task Performer

Envuelve la solución IA del proyecto IntegralSkin en un TaskPerformer conforme a IHE-AIW-I

En esta versión inicial el proyecto incluye el paquete de aplicación MONAI y la aplicación Spring Boot que actúa como TaskPerformer.

# Estándares considerados

* [IHE-AIW-I, AI workflow for imaging](https://www.ihe.net/uploadedFiles/Documents/Radiology/IHE_RAD_Suppl_AIW-I.pdf).

El objetivo es envolver los algoritmos de IA del proyecto IntegralSkin y ofrecerlos como un servicio implementando el actor TaskPerformer de IHE AIW-I

* [Unified Procedure Step Service (UPS-RS) de Dicom]
(https://dicom.nema.org/medical/dicom/2019a/output/chtml/part18/sect_6.9.html)

La obtención de workitems desde el taskManager se basa en el servicio UPS-RS de Dicom, según el modelo Triggered-Pull. Usa RAD_86 y RAD_87 para suscribirse y recibir notificaciones del TaskManager (la información de acceso al TaskManager estará en application.properties)

