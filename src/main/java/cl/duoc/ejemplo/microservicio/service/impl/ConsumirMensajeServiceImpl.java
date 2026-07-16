package cl.duoc.ejemplo.microservicio.service.impl;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;

import cl.duoc.ejemplo.microservicio.config.RabbitMQConfig;
import cl.duoc.ejemplo.microservicio.dto.ResumenCompraDTO;
import cl.duoc.ejemplo.microservicio.entities.ResumenCompra;
import cl.duoc.ejemplo.microservicio.repositories.ResumenCompraRepository;
import cl.duoc.ejemplo.microservicio.service.ConsumirMensajeService;

@Service
public class ConsumirMensajeServiceImpl implements ConsumirMensajeService {

	private String ultimoMensaje;

	@Autowired
	private ResumenCompraRepository resumenCompraRepository;

	@Override
	public String obtenerUltimoMensaje() {

		if (ultimoMensaje != null) {
			return ultimoMensaje;
		}

		String mensaje = null;

		ConnectionFactory factory = new ConnectionFactory();

		factory.setHost("localhost");
		factory.setUsername("guest");
		factory.setPassword("guest");

		try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {

			GetResponse response = channel.basicGet("myQueue", true);

			if (response != null) {
				mensaje = new String(response.getBody(), "UTF-8");
				ultimoMensaje = mensaje;
				System.out.println("Mensaje recibido: " + mensaje);
			} else {
				System.out.println("No hay mensajes en la cola");
			}

		} catch (Exception e) {
			System.out.println("Error al consumir mensaje de RabbitMQ");
			e.printStackTrace();
		}

		return mensaje;
	}

	@Override
	public void recibirMensaje(Object objeto) {

		this.ultimoMensaje = objeto.toString();
		System.out.println("Mensaje recibido en myQueue: " + objeto);
	}

	@RabbitListener(queues = RabbitMQConfig.MAIN_QUEUE, ackMode = "MANUAL")
	@Override
	public void recibirMensajeConAckManual(Message mensaje, Channel canal) throws IOException {

		try {
			String contenido = new String(mensaje.getBody(), "UTF-8");

			// Deserializar el DTO enviado por el Productor
			ObjectMapper mapper = new ObjectMapper();
			ResumenCompraDTO dto = mapper.readValue(contenido, ResumenCompraDTO.class);
			
			this.ultimoMensaje = dto.getResumen();

			System.out.println("Mensaje recibido para inscripcion: " + dto.getInscripcionId());

			// Guardar en la nueva tabla de Oracle
			ResumenCompra entidad = new ResumenCompra();
			entidad.setInscripcionId(dto.getInscripcionId());
			entidad.setResumen(dto.getResumen());
			resumenCompraRepository.save(entidad);
			System.out.println("Resumen guardado exitosamente en BD Oracle.");

			canal.basicAck(mensaje.getMessageProperties().getDeliveryTag(), false);
			System.out.println("Acknowledge OK enviado");

		} catch (Exception e) {
			canal.basicNack(mensaje.getMessageProperties().getDeliveryTag(), false, false);
			System.out.println("Acknowledge NO OK enviado por error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
