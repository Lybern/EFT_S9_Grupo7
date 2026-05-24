package cl.duoc.ejemplo.microservicio.repositories;

import cl.duoc.ejemplo.microservicio.entities.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CursoRepository extends JpaRepository<Curso, Long> {
}