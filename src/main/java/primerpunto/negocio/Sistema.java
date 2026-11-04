package primerpunto.negocio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import primerpunto.datos.CanchaDAO;
import primerpunto.datos.FormatoDAO;
import primerpunto.datos.IAccesoDatos;
import primerpunto.datos.InscripcionDAO;
import primerpunto.datos.ParticipanteDAO;
import primerpunto.datos.SinPersistencia;
import primerpunto.datos.TorneoDAO;
import primerpunto.datos.UsuarioDAO;
import primerpunto.excepciones.PersistenciaException;
import primerpunto.modelo.Cancha;
import primerpunto.modelo.Formato;
import primerpunto.modelo.Jugador;
import primerpunto.modelo.Pareja;
import primerpunto.modelo.Participante;
import primerpunto.modelo.Torneo;

/**
 * Punto de acceso a la capa de negocio: reúne los cuatro controladores del
 * sistema para que la interfaz pueda usarlos. Cada ventana trabaja con el
 * controlador de su módulo.
 *
 * Al iniciar, carga los datos guardados en la base en el orden en que dependen
 * unos de otros: canchas, jugadores, parejas, torneos con sus inscripciones, y por
 * último los formatos con sus partidos y resultados.
 */
public class Sistema {

    private final IAccesoDatos acceso;
    private final CanchaDAO canchaDAO;
    private final ParticipanteDAO participanteDAO;
    private final TorneoDAO torneoDAO;
    private final InscripcionDAO inscripcionDAO;
    private final FormatoDAO formatoDAO;
    private final UsuarioDAO usuarioDAO;

    private final GestionTorneos gestionTorneos;
    private final GestionParticipantes gestionParticipantes;
    private final GestionCuadrosZonas gestionCuadrosZonas;
    private final GestionPartidosCanchas gestionPartidosCanchas;
    private final GestionUsuarios gestionUsuarios;

    /** Sin base de datos (lo usan las pruebas automatizadas). */
    public Sistema() {
        this(new SinPersistencia());
    }

    public Sistema(IAccesoDatos acceso) {
        this.acceso = acceso;
        canchaDAO = new CanchaDAO(acceso);
        participanteDAO = new ParticipanteDAO(acceso);
        torneoDAO = new TorneoDAO(acceso);
        inscripcionDAO = new InscripcionDAO(acceso);
        formatoDAO = new FormatoDAO(acceso);
        usuarioDAO = new UsuarioDAO(acceso);

        gestionTorneos = new GestionTorneos(torneoDAO);
        gestionParticipantes = new GestionParticipantes(participanteDAO, inscripcionDAO);
        gestionCuadrosZonas = new GestionCuadrosZonas(gestionTorneos, formatoDAO);
        gestionPartidosCanchas = new GestionPartidosCanchas(gestionTorneos, canchaDAO, formatoDAO);
        gestionUsuarios = new GestionUsuarios(usuarioDAO);
    }

    /** Lee de la base todo lo guardado y lo entrega a cada controlador. */
    public void cargarDatos() throws PersistenciaException {
        List<Cancha> canchas = canchaDAO.cargar();
        Map<Integer, Cancha> canchasPorId = new HashMap<>();
        for (Cancha cancha : canchas) {
            canchasPorId.put(cancha.getIdCancha(), cancha);
        }

        List<Jugador> jugadores = participanteDAO.cargarJugadores();
        Map<Integer, Jugador> jugadoresPorId = new HashMap<>();
        Map<Integer, Participante> participantesPorId = new HashMap<>();
        for (Jugador jugador : jugadores) {
            jugadoresPorId.put(jugador.getIdParticipante(), jugador);
            participantesPorId.put(jugador.getIdParticipante(), jugador);
        }
        List<Pareja> parejas = participanteDAO.cargarParejas(jugadoresPorId);
        for (Pareja pareja : parejas) {
            participantesPorId.put(pareja.getIdParticipante(), pareja);
        }

        List<Torneo> torneos = torneoDAO.cargar();
        Map<Integer, Torneo> torneosPorId = new HashMap<>();
        for (Torneo torneo : torneos) {
            torneosPorId.put(torneo.getIdTorneo(), torneo);
        }
        inscripcionDAO.cargar(torneosPorId, participantesPorId);
        formatoDAO.cargar(torneosPorId, participantesPorId, canchasPorId);

        gestionPartidosCanchas.restaurar(canchas, canchaDAO.maximoId(), formatoDAO.maximoIdResultado());
        gestionParticipantes.restaurar(jugadores, parejas, torneos, participanteDAO.maximoId(),
                inscripcionDAO.maximoId());
        gestionTorneos.restaurar(torneos, torneoDAO.maximoId());
        gestionCuadrosZonas.restaurar(formatoDAO.maximoIdFormato());
        Formato.continuarNumeracionPartidos(formatoDAO.maximoIdPartido());
        gestionUsuarios.restaurar(usuarioDAO.cargar(), usuarioDAO.maximoId());
    }

    /** La base no tiene datos todavía (primera vez que se usa el programa). */
    public boolean estaVacio() {
        return gestionTorneos.listarTorneos(null).isEmpty() && gestionParticipantes.listarJugadores().isEmpty()
                && gestionPartidosCanchas.listarCanchas().isEmpty();
    }

    /** Cierra la conexión con la base al salir del programa. */
    public void cerrar() {
        acceso.desconectar();
    }

    public GestionTorneos getGestionTorneos() {
        return gestionTorneos;
    }

    public GestionParticipantes getGestionParticipantes() {
        return gestionParticipantes;
    }

    public GestionCuadrosZonas getGestionCuadrosZonas() {
        return gestionCuadrosZonas;
    }

    public GestionPartidosCanchas getGestionPartidosCanchas() {
        return gestionPartidosCanchas;
    }

    public GestionUsuarios getGestionUsuarios() {
        return gestionUsuarios;
    }
}
