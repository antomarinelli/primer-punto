package primerpunto.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Formato de competencia de un torneo: un Cuadro de eliminación directa o una Zona
 * de todos contra todos. Guarda los partidos; cada subclase arma sus cruces y
 * decide qué pasa cuando termina un partido.
 */
public abstract class Formato {

    private static int contadorPartidos = 1;

    private final int idFormato;
    private final List<Partido> partidos = new ArrayList<>();

    public Formato(int idFormato) {
        this.idFormato = idFormato;
    }

    public int getIdFormato() {
        return idFormato;
    }

    public List<Partido> getPartidos() {
        return Collections.unmodifiableList(partidos);
    }

    public void agregarPartido(Partido partido) {
        partidos.add(partido);
    }

    /**
     * Indica si se puede corregir el resultado de un partido ya finalizado.
     * Por defecto siempre se puede; el cuadro lo restringe.
     */
    public boolean admiteCambioDeResultado(Partido partido) {
        return true;
    }

    /** Numeración única de los partidos de todo el sistema. */
    protected static int siguienteIdPartido() {
        return contadorPartidos++;
    }

    /** Nombre del formato para mostrar en pantalla, por ejemplo "Cuadro de 8" o "Zona 2". */
    public abstract String getDescripcion();

    /** Arma los cruces iniciales a partir de los participantes habilitados. */
    public abstract void armarCruces(List<Participante> participantes);

    /** Acción que realiza el formato cuando se carga el resultado de uno de sus partidos. */
    public abstract void procesarResultado(Partido partido);

    /** Un formato está completo cuando ya no quedan partidos por jugar. */
    public boolean estaCompleto() {
        for (Partido partido : partidos) {
            if (!partido.estaFinalizado()) {
                return false;
            }
        }
        return !partidos.isEmpty();
    }

    @Override
    public String toString() {
        return getDescripcion();
    }
}
