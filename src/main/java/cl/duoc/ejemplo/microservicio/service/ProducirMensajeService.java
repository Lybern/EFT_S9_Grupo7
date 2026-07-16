package cl.duoc.ejemplo.microservicio.service;

public interface ProducirMensajeService {

	void enviarMensaje(String mensaje);

	public void enviarObjeto(Object objeto);
}
