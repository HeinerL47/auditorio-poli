package co.edu.poligran.auditorio.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "solicitante_id")
    private Usuario solicitante;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Seccion seccion;

    @Column(nullable = false)
    private String tipoEvento;

    @Column(length = 2000)
    private String especificaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado;

    private BigDecimal costo;

    @Column(length = 2000)
    private String observaciones;

    @Column(length = 2000)
    private String motivoRechazo;

    private LocalDateTime creadaEn;
    private LocalDateTime decididaEn;
    private String decididaPor;
    private boolean recordatorioEnviado;

    public Reserva() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getSolicitante() { return solicitante; }
    public void setSolicitante(Usuario solicitante) { this.solicitante = solicitante; }
    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }
    public LocalDateTime getFin() { return fin; }
    public void setFin(LocalDateTime fin) { this.fin = fin; }
    public Seccion getSeccion() { return seccion; }
    public void setSeccion(Seccion seccion) { this.seccion = seccion; }
    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }
    public String getEspecificaciones() { return especificaciones; }
    public void setEspecificaciones(String e) { this.especificaciones = e; }
    public EstadoReserva getEstado() { return estado; }
    public void setEstado(EstadoReserva estado) { this.estado = estado; }
    public BigDecimal getCosto() { return costo; }
    public void setCosto(BigDecimal costo) { this.costo = costo; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String o) { this.observaciones = o; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime c) { this.creadaEn = c; }
    public LocalDateTime getDecididaEn() { return decididaEn; }
    public void setDecididaEn(LocalDateTime d) { this.decididaEn = d; }
    public String getDecididaPor() { return decididaPor; }
    public void setDecididaPor(String d) { this.decididaPor = d; }
    public boolean isRecordatorioEnviado() { return recordatorioEnviado; }
    public void setRecordatorioEnviado(boolean r) { this.recordatorioEnviado = r; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Reserva r = new Reserva();
        public Builder solicitante(Usuario v) { r.solicitante = v; return this; }
        public Builder inicio(LocalDateTime v) { r.inicio = v; return this; }
        public Builder fin(LocalDateTime v) { r.fin = v; return this; }
        public Builder seccion(Seccion v) { r.seccion = v; return this; }
        public Builder tipoEvento(String v) { r.tipoEvento = v; return this; }
        public Builder especificaciones(String v) { r.especificaciones = v; return this; }
        public Builder estado(EstadoReserva v) { r.estado = v; return this; }
        public Builder costo(BigDecimal v) { r.costo = v; return this; }
        public Builder creadaEn(LocalDateTime v) { r.creadaEn = v; return this; }
        public Reserva build() { return r; }
    }
}
