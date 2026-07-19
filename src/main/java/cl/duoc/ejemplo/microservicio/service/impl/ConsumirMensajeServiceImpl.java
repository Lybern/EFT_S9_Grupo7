package cl.duoc.ejemplo.microservicio.service.impl;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

	@Value("${spring.rabbitmq.host}")
	private String rabbitmqHost;

	@Autowired
	private ResumenCompraRepository resumenCompraRepository;

	@Override
	public String obtenerUltimoMensaje() {

		if (ultimoMensaje != null) {
			return ultimoMensaje;
		}

		String mensaje = null;

		ConnectionFactory factory = new ConnectionFactory();

		factory.setHost(rabbitmqHost);
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

	/*
	 * [1] EL ESCUCHADOR DE LA COLA:
	 * Usamos @RabbitListener para estar atentos a la cola MAIN_QUEUE.
	 * El ackMode="MANUAL" es CRÍTICO: RabbitMQ no borrará el mensaje hasta que le demos permiso explícito,
	 * garantizando que no se pierdan datos si el servidor de Oracle se cae a medio proceso.
	 */
	@RabbitListener(queues = RabbitMQConfig.MAIN_QUEUE, ackMode = "MANUAL")
	@Override
	public void recibirMensajeConAckManual(Message mensaje, Channel canal) throws IOException {

		try {
			// [2] EXTRACCIÓN Y TRADUCCIÓN DEL MENSAJE (JSON a Java Object):
			// RabbitMQ envía el mensaje como bytes, lo pasamos a String UTF-8.
			String contenido = new String(mensaje.getBody(), "UTF-8");

			// Deserializamos el texto JSON enviado por el Productor y lo convertimos a nuestro ResumenCompraDTO
			ObjectMapper mapper = new ObjectMapper();
			ResumenCompraDTO dto = mapper.readValue(contenido, ResumenCompraDTO.class);
			
			this.ultimoMensaje = dto.getResumen();
			System.out.println("Mensaje recibido para inscripcion: " + dto.getInscripcionId());

			/* 
			 * [3] PERSISTENCIA EN ORACLE CLOUD:
			 * Tomamos los datos limpios extraídos de la cola y los guardamos en la tabla de Oracle.
			 * Esto ocurre de forma asíncrona, liberando a la API de hacer este trabajo lento.
			 */
			ResumenCompra entidad = new ResumenCompra();
			entidad.setInscripcionId(dto.getInscripcionId());
			entidad.setResumen(dto.getResumen());
			resumenCompraRepository.save(entidad);
			System.out.println("Resumen guardado exitosamente en BD Oracle.");

			/*
			 * [4] ACUSE DE RECIBO (ACK MANUAL):
			 * Si la base de datos guardó el registro sin arrojar excepciones, 
			 * enviamos un 'basicAck' a RabbitMQ diciéndole: "Trabajo exitoso, ya puedes borrar el mensaje".
			 */
			canal.basicAck(mensaje.getMessageProperties().getDeliveryTag(), false);
			System.out.println("Acknowledge OK enviado");

		} catch (Exception e) {
			/*
			 * [5] ESCUDO CONTRA FALLOS (NACK MANUAL):
			 * Si Oracle falla (ej. base de datos caída), el código entra aquí.
			 * Enviamos un 'basicNack' para rechazar el mensaje. Así, RabbitMQ retiene el mensaje
			 * y no lo elimina, evitando la pérdida de información del estudiante.
			 */
			canal.basicNack(mensaje.getMessageProperties().getDeliveryTag(), false, false);
			System.out.println("Acknowledge NO OK enviado por error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
