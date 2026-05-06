package co.edu.poligran.auditorio.model;

public enum Seccion {
    B1(50), B2(50), B3(100), COMPLETO(200);

    private final int capacidad;
    Seccion(int c) { this.capacidad = c; }
    public int getCapacidad() { return capacidad; }
}
