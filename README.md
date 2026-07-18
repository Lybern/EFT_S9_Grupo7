# Plataforma Cloud Native de Gestión de Cursos
**Evaluación Final Transversal (EFT) - Semana 9**

**Integrantes:** Carolina Delgado, Leonardo Bustamante  
**Asignatura:** Cloud Native II (CDY2204)  
**Docente:** Ignacio Pastenet  

---

## 🚀 Resumen del Proyecto

Este repositorio contiene el código fuente y las configuraciones de infraestructura de una plataforma de gestión de cursos en línea, construida estrictamente bajo los paradigmas de **Cloud Native**. 

La arquitectura implementa un microservicio (BFF) en **Java con Spring Boot**, orquestando almacenamiento físico en la nube, comunicación asíncrona mediante colas de mensajería, persistencia transaccional y un perímetro de seguridad robusto con identidad federada.

---

## 🏗️ Arquitectura Cloud Native

El ecosistema de la aplicación se compone de los siguientes servicios integrados:

1.  **Identity as a Service (IdaaS):** Azure AD B2C (Microsoft Entra ID) provee la autenticación federada, gestión de usuarios y emisión de Access/ID Tokens (JWT).
2.  **API Manager:** AWS API Gateway actúa como el único punto de entrada, interceptando el tráfico, validando criptográficamente los JWT contra Azure B2C y enrutando de forma segura hacia el backend.
3.  **Microservicio BFF:** Desarrollado en Java (Spring Boot) y empaquetado en contenedores Docker, desplegado en una instancia de Amazon EC2.
4.  **Almacenamiento (Storage):** AWS S3 se utiliza para persistir los archivos físicos (resúmenes de inscripción) estructurados por carpetas.
5.  **Servicio de Colas (Mensajería Asíncrona):** RabbitMQ, desplegado mediante Docker Compose en EC2, desacopla la generación de comprobantes para garantizar tolerancia a fallos.
6.  **Persistencia Transaccional:** Oracle Cloud Autonomous Database guarda las inscripciones y el historial de mensajes consumidos exitosamente por la cola (Acknowledge Manual).
7.  **Integración y Despliegue Continuo (CI/CD):** Pipeline de GitHub Actions que automatiza la construcción de la imagen de Docker y el redespliegue (Zero-downtime aproximado) en AWS EC2.

---

## 🔒 Rutas Securitizadas (Endpoints del API Gateway)

Las siguientes rutas están protegidas por el **Autorizador JWT** de AWS. Para invocarlas (vía Postman o Frontend), es obligatorio adjuntar el `Bearer Token` obtenido desde Azure AD B2C en la cabecera `Authorization`.

**URL de Invocación Base:**  
`https://f5zy111qg7.execute-api.us-east-1.amazonaws.com/PlataformaCursos`

### 1. Gestión Transaccional (Síncrona)
*   `POST /inscripciones` - Inscribir un estudiante y calcular monto total.
*   `GET /cursos` - Obtener el catálogo de cursos.
*   `POST /cursos` - Crear un nuevo curso en el catálogo.

### 2. Procesos Asíncronos (RabbitMQ)
*   `GET /inscripciones/{id}/resumen/generar` - Genera el resumen y dispara el mensaje al Productor de RabbitMQ de forma asíncrona.
*   `GET /api/mensaje/ultimo` - Consumidor: Lee el último comprobante procesado y guardado en la base de datos Oracle.

### 3. Almacenamiento Físico (AWS S3)
*   `POST /inscripciones/{id}/resumen/subir` - Carga un archivo físico al Bucket de S3.
*   `GET /inscripciones/{id}/resumen/descargar` - Descarga el documento desde S3.
*   `DELETE /inscripciones/{id}/resumen/eliminar` - Borra el documento en S3.

---

## ⚙️ Configuración y Despliegue Local

### Requisitos
*   Java 17+
*   Docker y Docker Compose
*   Billetera (Wallet) de Oracle Cloud descomprimida.

### Levantar el Servicio de Colas (RabbitMQ)
```bash
docker-compose up -d
```
*RabbitMQ estará disponible en el puerto `5672` (AMQP) y su consola de administración en `http://localhost:15672` (guest/guest).*

### Variables de Entorno Requeridas
Para ejecutar el microservicio de Java, se deben inyectar las siguientes variables:
*   `ORACLE_TNS_NAME`, `ORACLE_DB_USER`, `ORACLE_DB_PASSWORD`
*   `TNS_ADMIN_PATH` (Ruta absoluta hacia el Oracle Wallet)
*   `AZURE_CLIENT_SECRET` (Secreto del App Registration en Azure B2C)
*   `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` (Credenciales para S3).

---

## 🔄 Flujo CI/CD (GitHub Actions)

Cualquier `push` o `pull request` hacia la rama `main` dispara automáticamente el workflow definido en `.github/workflows/main.yml`. 

1.  **Build:** Compila el proyecto con Maven y construye la imagen Docker.
2.  **Push:** Sube la versión más reciente al repositorio de Docker Hub.
3.  **Deploy:** Se conecta por SSH a la instancia EC2, apaga el contenedor de Java antiguo (dejando RabbitMQ intacto gracias al Docker Compose), descarga la nueva imagen y la levanta exponiendo el puerto `8080`.
