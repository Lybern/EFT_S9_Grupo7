package cl.duoc.ejemplo.microservicio.dto;

import java.util.List;

public class InscripcionRequest {
    private String nombreEstudiante;
    private List<Long> cursoIds;

    public String getNombreEstudiante() {
        return nombreEstudiante;
    }

    public void setNombreEstudiante(String nombreEstudiante) {
        this.nombreEstudiante = nombreEstudiante;
    }

    public List<Long> getCursoIds() {
        return cursoIds;
    }

    public void setCursoIds(List<Long> cursoIds) {
        this.cursoIds = cursoIds;
    }
}