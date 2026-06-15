package cl.duoc.ejemplo.microservicio.controllers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import cl.duoc.ejemplo.microservicio.dto.InscripcionRequest;
import cl.duoc.ejemplo.microservicio.entities.Curso;
import cl.duoc.ejemplo.microservicio.entities.Inscripcion;
import cl.duoc.ejemplo.microservicio.repositories.CursoRepository;
import cl.duoc.ejemplo.microservicio.repositories.InscripcionRepository;
import cl.duoc.ejemplo.microservicio.service.AwsS3Service;

@RestController
@RequestMapping("/inscripciones")
public class InscripcionController {

    private final InscripcionRepository inscripcionRepository;
    private final CursoRepository cursoRepository;
    private final AwsS3Service awsS3Service;

    public InscripcionController(InscripcionRepository inscripcionRepository, CursoRepository cursoRepository, AwsS3Service awsS3Service) {
        this.inscripcionRepository = inscripcionRepository;
        this.cursoRepository = cursoRepository;
        this.awsS3Service = awsS3Service;
    }

    @PostMapping
    public ResponseEntity<Inscripcion> inscribirEstudiante(@RequestBody InscripcionRequest request) {
        if (request.getCursoIds() == null || request.getCursoIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Curso> cursosSeleccionados = cursoRepository.findAllById(request.getCursoIds());

        BigDecimal totalAPagar = cursosSeleccionados.stream()
                .map(Curso::getCosto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setNombreEstudiante(request.getNombreEstudiante());
        inscripcion.setCursos(cursosSeleccionados);
        inscripcion.setTotalAPagar(totalAPagar);

        return ResponseEntity.ok(inscripcionRepository.save(inscripcion));
    }

    // 1. Generar archivo físico (resumen de la inscripción)
    @GetMapping("/{id}/resumen/generar")
    public ResponseEntity<byte[]> generarResumenFisico(@PathVariable Long id) {
        Optional<Inscripcion> inscripcionOpt = inscripcionRepository.findById(id);
        if (inscripcionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Inscripcion inscripcion = inscripcionOpt.get();
        StringBuilder resumen = new StringBuilder();
        resumen.append("=== RESUMEN DE INSCRIPCIÓN ===\n");
        resumen.append("ID Inscripción: ").append(inscripcion.getId()).append("\n");
        resumen.append("Estudiante: ").append(inscripcion.getNombreEstudiante()).append("\n");
        resumen.append("Cursos Inscritos:\n");
        for (Curso curso : inscripcion.getCursos()) {
            resumen.append(" - ").append(curso.getNombre()).append(" (Costo: $").append(curso.getCosto()).append(")\n");
        }
        resumen.append("Total a Pagar: $").append(inscripcion.getTotalAPagar()).append("\n");
        resumen.append("==============================\n");

        byte[] archivoBytes = resumen.toString().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "resumen_inscripcion_" + id + ".txt");
        headers.setContentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8));

        return new ResponseEntity<>(archivoBytes, headers, HttpStatus.OK);
    }

    // 2. Subir resumen generado a bucket S3 en una carpeta con el id de la inscripción
    @PostMapping("/{id}/resumen")
    public ResponseEntity<Void> subirResumenS3(
            @PathVariable Long id, 
            @RequestParam("bucket") String bucket, 
            @RequestParam("file") MultipartFile file) {
        try {
            String key = id + "/" + file.getOriginalFilename();
            awsS3Service.upload(bucket, key, file);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 3. Modificar archivo del resumen en S3 (sobrescribir)
    @PutMapping("/{id}/resumen")
    public ResponseEntity<Void> modificarResumenS3(
            @PathVariable Long id, 
            @RequestParam("bucket") String bucket, 
            @RequestParam("file") MultipartFile file) {
        try {
            // Al subir un archivo a una ruta existente en S3, este se sobrescribe automáticamente
            String key = id + "/" + file.getOriginalFilename();
            awsS3Service.upload(bucket, key, file);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 4. Descargar archivo de resumen desde S3
    @GetMapping("/{id}/resumen")
    public ResponseEntity<byte[]> descargarResumenS3(
            @PathVariable Long id, 
            @RequestParam("bucket") String bucket, 
            @RequestParam("filename") String filename) {
        try {
            String key = id + "/" + filename;
            byte[] fileBytes = awsS3Service.downloadAsBytes(bucket, key);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fileBytes);
        } catch (NoSuchKeyException e) {
            try {
                java.io.InputStream in = new java.net.URI("https://http.cat/404").toURL().openStream();
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(in.readAllBytes());
            } catch (Exception ex) {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 5. Borrar archivo del resumen en S3
    @DeleteMapping("/{id}/resumen")
    public ResponseEntity<?> borrarResumenS3(
            @PathVariable Long id, 
            @RequestParam("bucket") String bucket, 
            @RequestParam("filename") String filename) {
        try {
            String key = id + "/" + filename;
            awsS3Service.deleteObject(bucket, key);
            return ResponseEntity.noContent().build();
        } catch (NoSuchKeyException e) {
            try {
                java.io.InputStream in = new java.net.URI("https://http.cat/404").toURL().openStream();
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(in.readAllBytes());
            } catch (Exception ex) {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}