package cl.duoc.ejemplo.microservicio.controllerss;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/microservicio")
public class MicroservicioController {

	@PostMapping
	public ResponseEntity<String> create(@RequestBody Map<String, String> body) {

		System.out.println("Se recibió llamada a endpoint POST");
		return ResponseEntity.ok("Integración OK - POST, recibido body: " + body);
	}

	@GetMapping("/{id}")
	public ResponseEntity<String> read(@PathVariable("id") String id) {

		System.out.println("Se recibió llamada a endpoint GET");
		return ResponseEntity.ok("Integración OK - GET, recibido path variable: " + id);
	}

	@PutMapping
	public ResponseEntity<String> update(@RequestParam("status") String status) {

		System.out.println("Se recibió llamada a endpoint PUT");
		return ResponseEntity.ok("Integración OK - PUT, recibido query param: " + status);
	}

	@DeleteMapping
	public ResponseEntity<String> delete(@RequestHeader("Authorization") String authHeader) {

		System.out.println("Se recibió llamada a endpoint DELETE");
		return ResponseEntity.ok("Integración OK - DELETE, recibido header: " + authHeader);
	}
}
