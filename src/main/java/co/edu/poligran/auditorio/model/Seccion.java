package co.edu.poligran.auditorio.model;

public enum Seccion {
    B1(50, "B1"),
    B2(50, "B2"),
    B3(100, "B3 (Completo)");

    private final int capacidad;
    private final String label;

    Seccion(int c, String l) { this.capacidad = c; this.label = l; }

    public int getCapacidad() { return capacidad; }
    public String getLabel()   { return label; }
}
