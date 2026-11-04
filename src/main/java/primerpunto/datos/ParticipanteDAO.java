package primerpunto.datos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import primerpunto.excepciones.PersistenciaException;
import primerpunto.modelo.Jugador;
import primerpunto.modelo.Pareja;

/**
 * Tablas participante, jugador y pareja (CU01, RF02). Jugador y Pareja heredan de
 * Participante con una tabla por subclase: cada uno ocupa una fila en participante
 * y otra en su propia tabla. En las tres se usa el mismo número de identificador.
 */
public class ParticipanteDAO extends DAO {

    public ParticipanteDAO(IAccesoDatos acceso) {
        super(acceso);
    }

    public List<Jugador> cargarJugadores() throws PersistenciaException {
        List<Jugador> jugadores = new ArrayList<>();
        List<Object[]> filas = acceso.ejecutarConsulta(
                "SELECT id_participante, nombre, apellido, dni, celular, email FROM jugador ORDER BY id_participante");
        for (Object[] fila : filas) {
            jugadores.add(new Jugador(entero(fila[0]), texto(fila[1]), texto(fila[2]), texto(fila[3]),
                    texto(fila[4]), texto(fila[5])));
        }
        return jugadores;
    }

    /** Carga las parejas; los jugadores ya cargados se buscan por su identificador. */
    public List<Pareja> cargarParejas(Map<Integer, Jugador> jugadoresPorId) throws PersistenciaException {
        List<Pareja> parejas = new ArrayList<>();
        List<Object[]> filas = acceso.ejecutarConsulta(
                "SELECT id_participante, id_jugador1, id_jugador2 FROM pareja ORDER BY id_participante");
        for (Object[] fila : filas) {
            parejas.add(new Pareja(entero(fila[0]), jugadoresPorId.get(entero(fila[1])),
                    jugadoresPorId.get(entero(fila[2]))));
        }
        return parejas;
    }

    public void insertarJugador(Jugador jugador) throws PersistenciaException {
        enTransaccion(() -> {
            insertarParticipante(jugador.getIdParticipante(), jugador.getNombreRepresentativo());
            acceso.ejecutarActualizacion("INSERT INTO jugador (id_jugador, id_participante, nombre, apellido, dni, "
                            + "celular, email) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    jugador.getIdParticipante(), jugador.getIdParticipante(), jugador.getNombre(),
                    jugador.getApellido(), jugador.getDni(), jugador.getCelular(), jugador.getEmail());
        });
    }

    public void insertarPareja(Pareja pareja) throws PersistenciaException {
        enTransaccion(() -> {
            insertarParticipante(pareja.getIdParticipante(), pareja.getNombreRepresentativo());
            acceso.ejecutarActualizacion(
                    "INSERT INTO pareja (id_pareja, id_participante, id_jugador1, id_jugador2) VALUES (?, ?, ?, ?)",
                    pareja.getIdParticipante(), pareja.getIdParticipante(), pareja.getJugador1().getIdParticipante(),
                    pareja.getJugador2().getIdParticipante());
        });
    }

    public void actualizarContacto(Jugador jugador) throws PersistenciaException {
        acceso.ejecutarActualizacion("UPDATE jugador SET celular = ?, email = ? WHERE id_participante = ?",
                jugador.getCelular(), jugador.getEmail(), jugador.getIdParticipante());
    }

    public int maximoId() throws PersistenciaException {
        return maximoId("participante", "id_participante");
    }

    private void insertarParticipante(int id, String nombre) throws PersistenciaException {
        acceso.ejecutarActualizacion(
                "INSERT INTO participante (id_participante, nombre_representativo) VALUES (?, ?)", id, nombre);
    }
}
