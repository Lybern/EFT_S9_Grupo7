# EjemploMicroservicio1

Este es el microservicio de gestión de inscripciones (Cloud Native). Conecta a una base de datos Oracle y se integra con AWS S3 para generar y almacenar resúmenes de inscripción.

---

## Endpoints Disponibles (Inscripciones y Resúmenes S3)

### POST `/inscripciones`
Crea una nueva inscripción para un estudiante calculando el costo total según los cursos seleccionados.

**Cuerpo de ejemplo (JSON):**
```json
{
  "nombreEstudiante": "Juan Perez",
  "cursoIds": [1, 2]
}
```

**Ejemplo con `curl`:**
```bash
curl -X POST 'http://localhost:8080/inscripciones' \
  -H 'Content-Type: application/json' \
  -d '{"nombreEstudiante": "Juan Perez", "cursoIds": [1, 2]}'
```

---

### GET `/inscripciones`
Lista todas las inscripciones registradas en la base de datos.

**Ejemplo con `curl`:**
```bash
curl -X GET 'http://localhost:8080/inscripciones'
```

---

### GET `/inscripciones/{id}`
Obtiene el detalle de una inscripción específica según su ID.

**Ejemplo con `curl`:**
```bash
curl -X GET 'http://localhost:8080/inscripciones/1'
```

---

### GET `/inscripciones/{id}/resumen/generar`
Genera dinámicamente un archivo físico de texto (resumen) con los detalles de la inscripción, listo para descargar.

**Ejemplo con `curl`:**
```bash
curl -O -J -X GET 'http://localhost:8080/inscripciones/1/resumen/generar'
```

---

### POST `/inscripciones/{id}/resumen?bucket={bucketName}`
Sube un archivo de resumen generado a una carpeta lógica (identificada por el ID de la inscripción) dentro de un bucket de AWS S3.

**Ejemplo con `curl`:**
```bash
curl -X POST "http://localhost:8080/inscripciones/1/resumen?bucket=mi-bucket-s3" \
  -F "file=@resumen_inscripcion_1.txt"
```

---

### PUT `/inscripciones/{id}/resumen?bucket={bucketName}`
Modifica (sobrescribe) un archivo de resumen existente en el bucket S3 para la inscripción dada.

**Ejemplo con `curl`:**
```bash
curl -X PUT "http://localhost:8080/inscripciones/1/resumen?bucket=mi-bucket-s3" \
  -F "file=@resumen_nuevo.txt"
```

---

### GET `/inscripciones/{id}/resumen?bucket={bucketName}&filename={fileName}`
Descarga el archivo de resumen previamente guardado en el bucket de S3.

**Ejemplo con `curl`:**
```bash
curl -O -J -X GET "http://localhost:8080/inscripciones/1/resumen?bucket=mi-bucket-s3&filename=resumen_inscripcion_1.txt"
```

---

### DELETE `/inscripciones/{id}/resumen?bucket={bucketName}&filename={fileName}`
Borra definitivamente el archivo de resumen alojado en AWS S3.

**Ejemplo con `curl`:**
```bash
curl -X DELETE "http://localhost:8080/inscripciones/1/resumen?bucket=mi-bucket-s3&filename=resumen_inscripcion_1.txt"
```

---

## Actualización (Semana 7): Integración con RabbitMQ y Seguridad

En la última iteración del proyecto, se incorporó un sistema de colas MQ y se mejoraron las prácticas de seguridad y despliegue:

*   **Integración RabbitMQ:** La generación del resumen de inscripción (`GET /{id}/resumen/generar`) ahora envía simultáneamente un mensaje a RabbitMQ. Un nuevo consumidor interno escucha la cola y guarda el resumen (como un comprobante de compra) en una **nueva tabla** (`RESUMEN_COMPRA`) creada automáticamente en Oracle Cloud.
*   **Nuevo Productor API:** Se habilitó el endpoint de prueba `POST /api/send` para publicar mensajes JSON estructurados directamente hacia la cola.
*   **Seguridad:** Se eliminaron las credenciales hardcodeadas. La conexión a la BD Oracle ahora es 100% inyectada por variables de entorno seguras.
*   **CI/CD Reforzado:** El pipeline hacia AWS EC2 fue actualizado para usar credenciales IAM estables y limpiar contenedores huérfanos antes de redesplegar el servicio.

---

## Notas
- Este microservicio interactúa con una base de datos **Oracle** (requiere inyectar variables de entorno como `ORACLE_TNS_NAME`, `ORACLE_DB_USER`, `ORACLE_DB_PASSWORD` y un Oracle Wallet válido bajo `/app/wallet` o entorno local).
- Utiliza **Spring Cloud AWS** para conectarse de manera fluida al servicio AWS S3.
- En los endpoints vinculados a S3, el uso del parámetro `bucket` en la URL otorga flexibilidad para elegir el bucket objetivo dinámicamente.
