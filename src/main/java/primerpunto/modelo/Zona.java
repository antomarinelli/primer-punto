package primerpunto.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Zona de todos contra todos: cada participante juega una vez contra cada uno
 * de los demás, y las posiciones se definen con una tabla.
 */
public class Zona extends Formato {

    private final int numeroZona;
    private final List<Participante> participantes = new ArrayList<>();

    public Zona(int idFormato, int numeroZona) {
        super(idFormato);
        this.numeroZona = numeroZona;
    }

    public int getNumeroZona() {
        return numeroZona;
    }

    public List<Participante> getParticipantes() {
        return Collections.unmodifiableList(participantes);
    }

    @Override
    public String getDescripcion() {
        return "Zona " + numeroZona;
    }

    /**
     * Genera todos los cruces posibles entre los participantes de la zona:
     * con n participantes se juegan n * (n - 1) / 2 partidos.
     */
    @Override
    public void armarCruces(List<Participante> participantesZona) {
        if (participantesZona.size() < 2) {
            throw new IllegalArgumentException("Una zona necesita al menos dos participantes.");
        }
        participantes.clear();
        participantes.addAll(participantesZona);
        int posicion = 0;
        for (int i = 0; i < participantes.size(); i++) {
            for (int j = i + 1; j < participantes.size(); j++) {
                agregarPartido(new Partido(siguienteIdPartido(), participantes.get(i), participantes.get(j),
                        getDescripcion(), 0, posicion++));
            }
        }
    }

    /** En una zona no hay avance de ronda: el resultado se refleja en la tabla de posiciones. */
    @Override
    public void procesarResultado(Partido partido) {
        // La tabla se calcula a partir de los partidos finalizados (ver calcularTablaPosiciones)
    }

    /** Calcula la tabla de posiciones de la zona (RF26), ordenada de primero a último. */
    public List<FilaPosicion> calcularTablaPosiciones() {
        Map<Participante, FilaPosicion> filas = new LinkedHashMap<>();
        for (Participante participante : participantes) {
            filas.put(participante, new FilaPosicion(participante));
        }
        for (Partido partido : getPartidos()) {
            if (partido.estaFinalizado()) {
                int[] sets = partido.getResultado().getSets();
                Participante ganador = partido.getGanador();
                filas.get(partido.getParticipante1()).registrarPartido(
                        ganador.equals(partido.getParticipante1()), sets[0], sets[1]);
                filas.get(partido.getParticipante2()).registrarPartido(
                        ganador.equals(partido.getParticipante2()), sets[1], sets[0]);
            }
        }
        List<FilaPosicion> tabla = new ArrayList<>(filas.values());
        Collections.sort(tabla);
        return tabla;
    }
}
