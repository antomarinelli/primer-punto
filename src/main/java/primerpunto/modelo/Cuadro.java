package primerpunto.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Cuadro de eliminación directa: quien pierde un partido queda eliminado y
 * quien gana avanza a la ronda siguiente, hasta definir al campeón.
 *
 * El cuadro se representa como una lista de rondas. Cada ronda es un arreglo de
 * "lugares": en la primera ronda hay tantos lugares como el tamaño del cuadro, y
 * en cada ronda siguiente, la mitad. Los partidos se forman con los lugares de a
 * pares (0 y 1, 2 y 3, ...), y el ganador ocupa su lugar en la ronda siguiente.
 *
 * Todos los partidos del cuadro se crean al armarlo: los de las rondas siguientes
 * quedan "a definir" y se completan a medida que avanzan los ganadores.
 */
public class Cuadro extends Formato {

    private final String tipoCuadro = "Eliminación directa";
    private int tamanio;
    private int cantidadByes;
    private final List<Participante[]> rondas = new ArrayList<>();

    public Cuadro(int idFormato) {
        super(idFormato);
    }

    public String getTipoCuadro() {
        return tipoCuadro;
    }

    public int getTamanio() {
        return tamanio;
    }

    public int getCantidadByes() {
        return cantidadByes;
    }

    /** Cantidad de rondas que tiene el cuadro (por ejemplo, 3 para un cuadro de 8). */
    public int getCantidadRondas() {
        return rondas.size() - 1;
    }

    /** Participantes ubicados en cada lugar de una ronda (null si el lugar todavía está vacío). */
    public Participante[] getLugaresRonda(int ronda) {
        return rondas.get(ronda).clone();
    }

    /** El campeón queda en la última "ronda", que tiene un único lugar. */
    public Participante getCampeon() {
        return rondas.isEmpty() ? null : rondas.get(rondas.size() - 1)[0];
    }

    @Override
    public String getDescripcion() {
        return "Cuadro de " + tamanio;
    }

    /**
     * Calcula los byes necesarios para completar el cuadro (RF19).
     * El tamaño del cuadro es la menor potencia de 2 mayor o igual a la cantidad
     * de participantes, y los byes son los lugares que sobran.
     * Por ejemplo: con 5 participantes el cuadro es de 8 y hay 3 byes.
     */
    public int calcularByes(int cantidadParticipantes) {
        int tamanioCuadro = 1;
        while (tamanioCuadro < cantidadParticipantes) {
            tamanioCuadro *= 2;
        }
        return tamanioCuadro - cantidadParticipantes;
    }

    /**
     * Arma el cuadro. Los primeros participantes de la lista reciben los byes
     * (pasan directo a la segunda ronda); el resto se enfrenta en la primera ronda.
     * Así ningún partido de primera ronda queda con dos byes.
     */
    @Override
    public void armarCruces(List<Participante> participantes) {
        if (participantes.size() < 2) {
            throw new IllegalArgumentException("No hay inscriptos suficientes para generar el formato.");
        }
        cantidadByes = calcularByes(participantes.size());
        tamanio = participantes.size() + cantidadByes;

        // 1. Se crean las rondas vacías: tamanio lugares, luego la mitad, ..., hasta 1 (el campeón)
        rondas.clear();
        for (int lugares = tamanio; lugares >= 1; lugares /= 2) {
            rondas.add(new Participante[lugares]);
        }

        // 2. Primera ronda: quien tiene bye queda solo en su par de lugares y avanza directo
        int siguiente = 0;
        for (int i = 0; i < tamanio / 2; i++) {
            Participante primero = participantes.get(siguiente++);
            rondas.get(0)[2 * i] = primero;
            if (i < cantidadByes) {
                rondas.get(1)[i] = primero;
            } else {
                rondas.get(0)[2 * i + 1] = participantes.get(siguiente++);
            }
        }

        // 3. Se crean todos los partidos del cuadro (los de rondas futuras, "a definir")
        for (int ronda = 0; ronda < getCantidadRondas(); ronda++) {
            Participante[] lugares = rondas.get(ronda);
            for (int posicion = 0; posicion < lugares.length / 2; posicion++) {
                boolean esBye = ronda == 0 && posicion < cantidadByes;
                if (!esBye) {
                    agregarPartido(new Partido(siguienteIdPartido(), lugares[2 * posicion], lugares[2 * posicion + 1],
                            nombreRonda(ronda), ronda, posicion));
                }
            }
        }
    }

    /**
     * Cuando termina un partido, el ganador avanza a la ronda siguiente (RF25).
     * Si se corrigió un resultado y cambió el ganador, se reemplaza en el partido
     * siguiente (que todavía no se jugó).
     */
    @Override
    public void procesarResultado(Partido partido) {
        int ronda = partido.getRonda() + 1;
        int lugar = partido.getPosicion();
        rondas.get(ronda)[lugar] = partido.getGanador();
        Partido siguiente = buscarPartidoSiguiente(partido);
        if (siguiente != null) {
            siguiente.setParticipante(lugar % 2 == 0 ? 1 : 2, partido.getGanador());
        }
    }

    /** En el cuadro, un resultado se puede corregir mientras el partido siguiente no se haya jugado. */
    @Override
    public boolean admiteCambioDeResultado(Partido partido) {
        Partido siguiente = buscarPartidoSiguiente(partido);
        return siguiente == null || !siguiente.estaFinalizado();
    }

    /** Partido de la ronda siguiente al que avanza el ganador (null si es la final). */
    public Partido buscarPartidoSiguiente(Partido partido) {
        return buscarPartido(partido.getRonda() + 1, partido.getPosicion() / 2);
    }

    /** Partidos de la ronda anterior cuyos ganadores juegan este partido. */
    public List<Partido> buscarPartidosPrevios(Partido partido) {
        List<Partido> previos = new ArrayList<>();
        if (partido.getRonda() > 0) {
            for (int i = 0; i < 2; i++) {
                Partido previo = buscarPartido(partido.getRonda() - 1, partido.getPosicion() * 2 + i);
                if (previo != null) {
                    previos.add(previo);
                }
            }
        }
        return previos;
    }

    public Partido buscarPartido(int ronda, int posicion) {
        for (Partido partido : getPartidos()) {
            if (partido.getRonda() == ronda && partido.getPosicion() == posicion) {
                return partido;
            }
        }
        return null;
    }

    /** Nombre de la ronda según cuántos participantes quedan en ella. */
    public String nombreRonda(int ronda) {
        int lugares = rondas.get(ronda).length;
        switch (lugares) {
            case 2:
                return "Final";
            case 4:
                return "Semifinal";
            case 8:
                return "Cuartos de final";
            case 16:
                return "Octavos de final";
            default:
                return "Ronda de " + lugares;
        }
    }

    /** El cuadro está completo cuando ya hay campeón. */
    @Override
    public boolean estaCompleto() {
        return getCampeon() != null;
    }
}
