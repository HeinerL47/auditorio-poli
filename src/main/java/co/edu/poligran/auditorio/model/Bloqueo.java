package co.edu.poligran.auditorio.model;

import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "bloqueos")
public class Bloqueo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    private Seccion seccion;

    @Enumerated(EnumType.STRING)
    private DayOfWeek diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;

    private LocalDateTime inicio;
    private LocalDateTime fin;

    private boolean recurrente;

    public Bloqueo() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Seccion getSeccion() { return seccion; }
    public void setSeccion(Seccion seccion) { this.seccion = seccion; }
    public DayOfWeek getDiaSemana() { return diaSemana; }
    public void setDiaSemana(DayOfWeek d) { this.diaSemana = d; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime h) { this.horaInicio = h; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime h) { this.horaFin = h; }
    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime i) { this.inicio = i; }
    public LocalDateTime getFin() { return fin; }
    public void setFin(LocalDateTime f) { this.fin = f; }
    public boolean isRecurrente() { return recurrente; }
    public void setRecurrente(boolean r) { this.recurrente = r; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Bloqueo b = new Bloqueo();
        public Builder motivo(String v) { b.motivo = v; return this; }
        public Builder seccion(Seccion v) { b.seccion = v; return this; }
        public Builder diaSemana(DayOfWeek v) { b.diaSemana = v; return this; }
        public Builder horaInicio(LocalTime v) { b.horaInicio = v; return this; }
        public Builder horaFin(LocalTime v) { b.horaFin = v; return this; }
        public Builder inicio(LocalDateTime v) { b.inicio = v; return this; }
        public Builder fin(LocalDateTime v) { b.fin = v; return this; }
        public Builder recurrente(boolean v) { b.recurrente = v; return this; }
        public Bloqueo build() { return b; }
    }
}
