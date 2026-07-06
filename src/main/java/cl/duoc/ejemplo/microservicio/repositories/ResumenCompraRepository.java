package cl.duoc.ejemplo.microservicio.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.ejemplo.microservicio.entities.ResumenCompra;

@Repository
public interface ResumenCompraRepository extends JpaRepository<ResumenCompra, Long> {
}
