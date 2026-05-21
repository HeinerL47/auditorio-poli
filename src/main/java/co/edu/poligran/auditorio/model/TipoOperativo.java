package co.edu.poligran.auditorio.model;

public enum TipoOperativo {
    ASISTENTE("Asistente (mismos permisos que Admin)"),
    TECNOLOGIA("Tecnología"),
    AUDIOVISUAL("Audiovisual"),
    INFRAESTRUCTURA("Infraestructura"),
    OPERACIONES("Operaciones");

    private final String label;

    TipoOperativo(String label) { this.label = label; }

    public String getLabel() { return label; }
}
