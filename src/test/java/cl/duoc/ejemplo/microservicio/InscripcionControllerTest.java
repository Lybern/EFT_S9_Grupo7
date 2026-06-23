package cl.duoc.ejemplo.microservicio;

import cl.duoc.ejemplo.microservicio.controllers.InscripcionController;
import cl.duoc.ejemplo.microservicio.entities.Curso;
import cl.duoc.ejemplo.microservicio.entities.Inscripcion;
import cl.duoc.ejemplo.microservicio.repositories.CursoRepository;
import cl.duoc.ejemplo.microservicio.repositories.InscripcionRepository;
import cl.duoc.ejemplo.microservicio.service.AwsS3Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InscripcionController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class
})
class InscripcionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InscripcionRepository inscripcionRepository;

    @MockitoBean
    private CursoRepository cursoRepository;

    @MockitoBean
    private AwsS3Service awsS3Service;

    @Test
    void testGenerarResumenFisico_Success() throws Exception {
        // Arrange
        Long inscripcionId = 1L;
        Curso curso = new Curso();
        curso.setId(101L);
        curso.setNombre("Cloud Native");
        curso.setCosto(new BigDecimal("150000"));

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(inscripcionId);
        inscripcion.setNombreEstudiante("Juan Perez");
        inscripcion.setCursos(List.of(curso));
        inscripcion.setTotalAPagar(new BigDecimal("150000"));

        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.of(inscripcion));

        // Act & Assert
        mockMvc.perform(get("/inscripciones/{id}/resumen/generar", inscripcionId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"resumen_inscripcion_1.txt\""))
                .andExpect(content().string(
                        "=== RESUMEN DE INSCRIPCIÓN ===\n" +
                        "ID Inscripción: 1\n" +
                        "Estudiante: Juan Perez\n" +
                        "Cursos Inscritos:\n" +
                        " - Cloud Native (Costo: $150000)\n" +
                        "Total a Pagar: $150000\n" +
                        "==============================\n"
                ));
    }

    @Test
    void testGenerarResumenFisico_NotFound() throws Exception {
        // Arrange
        Long inscripcionId = 99L;
        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/inscripciones/{id}/resumen/generar", inscripcionId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSubirResumenS3_Success() throws Exception {
        // Arrange
        Long inscripcionId = 1L;
        String bucket = "test-bucket";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resumen.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "contenido del resumen".getBytes(StandardCharsets.UTF_8)
        );

        doNothing().when(awsS3Service).upload(anyString(), anyString(), any(MockMultipartFile.class));

        // Act & Assert
        mockMvc.perform(multipart("/inscripciones/{id}/resumen", inscripcionId)
                        .file(file)
                        .param("bucket", bucket)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        verify(awsS3Service, times(1)).upload(bucket, inscripcionId + "/" + file.getOriginalFilename(), file);
    }

    @Test
    void testModificarResumenS3_Success() throws Exception {
        // Arrange
        Long inscripcionId = 1L;
        String bucket = "test-bucket";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resumen_modificado.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "contenido modificado".getBytes(StandardCharsets.UTF_8)
        );

        // Act & Assert
        mockMvc.perform(multipart(HttpMethod.PUT, "/inscripciones/{id}/resumen", inscripcionId)
                        .file(file)
                        .param("bucket", bucket))
                .andExpect(status().isOk());

        verify(awsS3Service, times(1)).upload(bucket, inscripcionId + "/" + file.getOriginalFilename(), file);
    }

    @Test
    void testDescargarResumenS3_Success() throws Exception {
        // Arrange
        Long inscripcionId = 1L;
        String bucket = "test-bucket";
        String filename = "resumen.txt";
        String key = inscripcionId + "/" + filename;
        byte[] fileBytes = "contenido del resumen".getBytes(StandardCharsets.UTF_8);

        when(awsS3Service.downloadAsBytes(bucket, key)).thenReturn(fileBytes);

        // Act & Assert
        mockMvc.perform(get("/inscripciones/{id}/resumen", inscripcionId)
                        .param("bucket", bucket)
                        .param("filename", filename))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("Content-Disposition", "attachment; filename=" + filename))
                .andExpect(content().bytes(fileBytes));

        verify(awsS3Service, times(1)).downloadAsBytes(bucket, key);
    }

    @Test
    void testBorrarResumenS3_Success() throws Exception {
        // Arrange
        Long inscripcionId = 1L;
        String bucket = "test-bucket";
        String filename = "resumen.txt";
        String key = inscripcionId + "/" + filename;

        doNothing().when(awsS3Service).deleteObject(bucket, key);

        // Act & Assert
        mockMvc.perform(delete("/inscripciones/{id}/resumen", inscripcionId)
                        .param("bucket", bucket)
                        .param("filename", filename))
                .andExpect(status().isNoContent());

        verify(awsS3Service, times(1)).deleteObject(bucket, key);
    }
}