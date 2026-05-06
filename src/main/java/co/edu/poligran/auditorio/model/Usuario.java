package co.edu.poligran.auditorio.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(columnNames = "correo"),
        @UniqueConstraint(columnNames = "documento")
})
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String documento;

    @Column(nullable = false, unique = true)
    private String correo;

    private String telefono;
    private String organizacion;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    private TipoSolicitante tipoSolicitante;

    private boolean activo = true;

    public Usuario() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getOrganizacion() { return organizacion; }
    public void setOrganizacion(String organizacion) { this.organizacion = organizacion; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public TipoSolicitante getTipoSolicitante() { return tipoSolicitante; }
    public void setTipoSolicitante(TipoSolicitante t) { this.tipoSolicitante = t; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public boolean esExterno() { return tipoSolicitante == TipoSolicitante.EXTERNO; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Usuario u = new Usuario();
        public Builder nombre(String v) { u.nombre = v; return this; }
        public Builder documento(String v) { u.documento = v; return this; }
        public Builder correo(String v) { u.correo = v; return this; }
        public Builder telefono(String v) { u.telefono = v; return this; }
        public Builder organizacion(String v) { u.organizacion = v; return this; }
        public Builder password(String v) { u.password = v; return this; }
        public Builder rol(Rol v) { u.rol = v; return this; }
        public Builder tipoSolicitante(TipoSolicitante v) { u.tipoSolicitante = v; return this; }
        public Builder activo(boolean v) { u.activo = v; return this; }
        public Usuario build() { return u; }
    }
}
