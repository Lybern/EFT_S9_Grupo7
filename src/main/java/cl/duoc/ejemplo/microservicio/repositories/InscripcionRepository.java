package cl.duoc.ejemplo.microservicio.repositories;

import cl.duoc.ejemplo.microservicio.entities.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
}
