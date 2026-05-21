package co.edu.poligran.auditorio.model;

public enum TipoSolicitante {
    DOCENTE("Docente"),
    ADMINISTRATIVO("Administrativo"),
    EXTERNO("Agente Externo");

    private final String label;

    TipoSolicitante(String label) { this.label = label; }

    public String getLabel() { return label; }
}
