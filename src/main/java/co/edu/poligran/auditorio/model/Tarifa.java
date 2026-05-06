package co.edu.poligran.auditorio.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tarifas")
public class Tarifa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private Seccion seccion;

    @Column(nullable = false)
    private BigDecimal valorHora;

    public Tarifa() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Seccion getSeccion() { return seccion; }
    public void setSeccion(Seccion seccion) { this.seccion = seccion; }
    public BigDecimal getValorHora() { return valorHora; }
    public void setValorHora(BigDecimal valorHora) { this.valorHora = valorHora; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Tarifa t = new Tarifa();
        public Builder seccion(Seccion v) { t.seccion = v; return this; }
        public Builder valorHora(BigDecimal v) { t.valorHora = v; return this; }
        public Tarifa build() { return t; }
    }
}
