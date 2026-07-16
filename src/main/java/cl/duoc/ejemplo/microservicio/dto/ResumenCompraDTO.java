package cl.duoc.ejemplo.microservicio.dto;

public class ResumenCompraDTO {
    private Long inscripcionId;
    private String resumen;

    public ResumenCompraDTO() {
    }

    public ResumenCompraDTO(Long inscripcionId, String resumen) {
        this.inscripcionId = inscripcionId;
        this.resumen = resumen;
    }

    public Long getInscripcionId() {
        return inscripcionId;
    }

    public void setInscripcionId(Long inscripcionId) {
        this.inscripcionId = inscripcionId;
    }

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }
}
