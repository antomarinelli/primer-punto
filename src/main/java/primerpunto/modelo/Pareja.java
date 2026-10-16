package primerpunto.modelo;

/**
 * Pareja formada por dos jugadores, para las modalidades Dobles y Mixto.
 * Se inscribe y compite como un único participante.
 */
public class Pareja extends Participante {

    private final Jugador jugador1;
    private final Jugador jugador2;

    public Pareja(int idParticipante, Jugador jugador1, Jugador jugador2) {
        super(idParticipante, armarNombre(jugador1, jugador2));
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
    }

    /** Valida la pareja y arma su nombre representativo, por ejemplo "Marinelli / Diaz". */
    private static String armarNombre(Jugador jugador1, Jugador jugador2) {
        if (jugador1 == null || jugador2 == null) {
            throw new IllegalArgumentException("La pareja debe estar formada por dos jugadores.");
        }
        if (jugador1.equals(jugador2)) {
            throw new IllegalArgumentException("Una pareja no puede estar formada por el mismo jugador dos veces.");
        }
        return jugador1.getApellido() + " / " + jugador2.getApellido();
    }

    public Jugador getJugador1() {
        return jugador1;
    }

    public Jugador getJugador2() {
        return jugador2;
    }

    public boolean contieneJugador(Jugador jugador) {
        return jugador1.equals(jugador) || jugador2.equals(jugador);
    }

    /** Muestra la inicial y el apellido de cada integrante: "A. Marinelli / M. Diaz". */
    @Override
    public String obtenerNombreCompleto() {
        return abreviar(jugador1) + " / " + abreviar(jugador2);
    }

    private static String abreviar(Jugador jugador) {
        return jugador.getNombre().charAt(0) + ". " + jugador.getApellido();
    }
}
