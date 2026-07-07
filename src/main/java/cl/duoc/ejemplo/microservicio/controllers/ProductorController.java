package cl.duoc.ejemplo.microservicio.controllers;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProductorController {

    private final RabbitTemplate rabbitTemplate;

    public ProductorController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/send")
    public ResponseEntity<String> send(@RequestBody cl.duoc.ejemplo.microservicio.dto.ResumenCompraDTO mensaje) {
        // Envía el objeto directamente para que Jackson lo serialice como JSON correcto
        rabbitTemplate.convertAndSend("myExchange", "", mensaje);
        return ResponseEntity.ok("Resumen enviado exitosamente a RabbitMQ para la inscripción: " + mensaje.getInscripcionId());
    }
}
