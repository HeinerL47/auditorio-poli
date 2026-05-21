package co.edu.poligran.auditorio.model;

public enum Rol {
    SOLICITANTE("Solicitante"),
    ADMIN_AUDITORIO("Administrador"),
    OPERATIVO("Operativo");

    private final String label;

    Rol(String label) { this.label = label; }

    public String getLabel() { return label; }
}
