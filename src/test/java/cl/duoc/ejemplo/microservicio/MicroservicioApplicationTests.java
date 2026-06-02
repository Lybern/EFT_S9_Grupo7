package cl.duoc.ejemplo.microservicio;

import cl.duoc.ejemplo.microservicio.repositories.CursoRepository;
import cl.duoc.ejemplo.microservicio.repositories.InscripcionRepository;
import cl.duoc.ejemplo.microservicio.service.AwsS3Service;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@EnableAutoConfiguration(exclude = {
		DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class
})
class MicroservicioApplicationTests {

	@MockitoBean
	private InscripcionRepository inscripcionRepository;

	@MockitoBean
	private CursoRepository cursoRepository;

	@MockitoBean
	private AwsS3Service awsS3Service;

	// Test push
	@Test
	void contextLoads() {
	}

}
