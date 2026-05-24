package cl.duoc.ejemplo.microservicio.controllers;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.ejemplo.microservicio.dto.InscripcionRequest;
import cl.duoc.ejemplo.microservicio.entities.Curso;
import cl.duoc.ejemplo.microservicio.entities.Inscripcion;
import cl.duoc.ejemplo.microservicio.repositories.CursoRepository;
import cl.duoc.ejemplo.microservicio.repositories.InscripcionRepository;

@RestController
@RequestMapping("/inscripciones")
public class InscripcionController {

    private final InscripcionRepository inscripcionRepository;
    private final CursoRepository cursoRepository;

    public InscripcionController(InscripcionRepository inscripcionRepository, CursoRepository cursoRepository) {
        this.inscripcionRepository = inscripcionRepository;
        this.cursoRepository = cursoRepository;
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
}