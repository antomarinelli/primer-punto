package primerpunto.negocio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import primerpunto.datos.ExportadorCSV;
import primerpunto.datos.InscripcionDAO;
import primerpunto.datos.ParticipanteDAO;
import primerpunto.datos.SinPersistencia;
import primerpunto.excepciones.ArchivoException;
import primerpunto.excepciones.DatoInvalidoException;
import primerpunto.excepciones.InscripcionDuplicadaException;
import primerpunto.excepciones.PersistenciaException;
import primerpunto.excepciones.PlazoVencidoException;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.EstadoInscripcion;
import primerpunto.modelo.EstadoTorneo;
import primerpunto.modelo.Inscripcion;
import primerpunto.modelo.Jugador;
import primerpunto.modelo.Pareja;
import primerpunto.modelo.Participante;
import primerpunto.modelo.Torneo;

/**
 * Controlador del módulo Participantes: registra jugadores y parejas, inscribe
 * participantes en los torneos, registra los pagos y procesa las bajas.
 */
public class GestionParticipantes {

    private final ParticipanteDAO participanteDAO;
    private final InscripcionDAO inscripcionDAO;
    private final ExportadorCSV exportador = new ExportadorCSV();
    private final List<Jugador> jugadores = new ArrayList<>();
    private final List<Pareja> parejas = new ArrayList<>();
    /** Vacantes liberadas por bajas aceptadas fuera de plazo, por número de torneo (CU05, flujo alternativo 1). */
    private final Map<Integer, Integer> vacantesPorBaja = new HashMap<>();
    private int proximoIdParticipante = 1;
    private int proximoIdInscripcion = 1;

    /** Sin base de datos (lo usan las pruebas automatizadas). */
    public GestionParticipantes() {
        this(new ParticipanteDAO(new SinPersistencia()), new InscripcionDAO(new SinPersistencia()));
    }

    public GestionParticipantes(ParticipanteDAO participanteDAO, InscripcionDAO inscripcionDAO) {
        this.participanteDAO = participanteDAO;
        this.inscripcionDAO = inscripcionDAO;
    }

    /**
     * Recibe los jugadores, las parejas y los torneos (con sus inscripciones) leídos de la
     * base. Las vacantes por baja se recalculan: cada baja fuera de plazo tiene motivo, y
     * cada inscripción posterior al plazo ocupó una de esas vacantes.
     */
    void restaurar(List<Jugador> jugadoresGuardados, List<Pareja> parejasGuardadas, List<Torneo> torneos,
                   int ultimoIdParticipante, int ultimoIdInscripcion) {
        jugadores.clear();
        jugadores.addAll(jugadoresGuardados);
        parejas.clear();
        parejas.addAll(parejasGuardadas);
        proximoIdParticipante = ultimoIdParticipante + 1;
        proximoIdInscripcion = ultimoIdInscripcion + 1;
        vacantesPorBaja.clear();
        for (Torneo torneo : torneos) {
            int vacantes = 0;
            for (Inscripcion inscripcion : torneo.getInscripciones()) {
                if (!inscripcion.estaActiva() && inscripcion.getMotivoBaja() != null
                        && !inscripcion.getMotivoBaja().isBlank()) {
                    vacantes++;
                }
                if (torneo.verificarPlazoVencido(inscripcion.getFecha())) {
                    vacantes--;
                }
            }
            if (vacantes > 0) {
                vacantesPorBaja.put(torneo.getIdTorneo(), vacantes);
            }
        }
    }

    // ------------------------------------------------------------ jugadores y parejas

    /** Registra un jugador nuevo (CU01). No se admiten dos jugadores con el mismo DNI. */
    public Jugador registrarJugador(String nombre, String apellido, String dni, String celular, String email)
            throws PrimerPuntoException {
        for (Jugador existente : jugadores) {
            if (existente.getDni().equals(dni)) {
                throw new DatoInvalidoException("Ya existe un jugador registrado con el DNI " + dni + ": "
                        + existente.obtenerNombreCompleto() + ".");
            }
        }
        Jugador jugador;
        try {
            jugador = new Jugador(proximoIdParticipante, nombre, apellido, dni, celular, email);
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException(e.getMessage(), e);
        }
        participanteDAO.insertarJugador(jugador);
        jugadores.add(jugador);
        proximoIdParticipante++;
        return jugador;
    }

    /**
     * Devuelve la pareja formada por los dos jugadores (RF02). Si ya estaba
     * registrada se reutiliza, sin importar el orden en que se elijan los jugadores.
     */
    public Pareja asociarPareja(Jugador jugador1, Jugador jugador2) throws PrimerPuntoException {
        for (Pareja pareja : parejas) {
            if (pareja.contieneJugador(jugador1) && pareja.contieneJugador(jugador2)) {
                return pareja;
            }
        }
        Pareja pareja;
        try {
            pareja = new Pareja(proximoIdParticipante, jugador1, jugador2);
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException(e.getMessage(), e);
        }
        participanteDAO.insertarPareja(pareja);
        parejas.add(pareja);
        proximoIdParticipante++;
        return pareja;
    }

    /** Actualiza el celular y el email de un jugador; si no se puede guardar, quedan los anteriores. */
    public void modificarContacto(Jugador jugador, String celular, String email) throws PrimerPuntoException {
        String celularAnterior = jugador.getCelular();
        String emailAnterior = jugador.getEmail();
        try {
            jugador.setContacto(celular, email);
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException(e.getMessage(), e);
        }
        try {
            participanteDAO.actualizarContacto(jugador);
        } catch (PersistenciaException e) {
            jugador.setContacto(celularAnterior, emailAnterior);
            throw e;
        }
    }

    public List<Jugador> listarJugadores() {
        return new ArrayList<>(jugadores);
    }

    public List<Pareja> listarParejas() {
        return new ArrayList<>(parejas);
    }

    // ------------------------------------------------------------ inscripción

    /** Inscribe a un participante con la fecha actual del sistema (CU05). */
    public Inscripcion inscribirParticipante(Torneo torneo, Participante participante, boolean pagado)
            throws PrimerPuntoException {
        return inscribirParticipante(torneo, participante, pagado, LocalDate.now());
    }

    /**
     * Inscribe a un participante en un torneo (CU05), incluyendo el registro del pago (CU06).
     * Las validaciones siguen el orden del diagrama de secuencia: plazo, registro del
     * participante, modalidad y duplicados.
     */
    public Inscripcion inscribirParticipante(Torneo torneo, Participante participante, boolean pagado,
                                             LocalDate fecha) throws PrimerPuntoException {
        if (torneo == null || participante == null) {
            throw new DatoInvalidoException("Seleccioná el torneo y el participante a inscribir.");
        }
        if (torneo.getEstado() == EstadoTorneo.EN_CURSO || torneo.getEstado() == EstadoTorneo.FINALIZADO) {
            throw new DatoInvalidoException("El torneo ya comenzó: no admite nuevas inscripciones.");
        }

        // 1. Plazo de inscripción (RF10). Fuera de plazo solo se puede cubrir una vacante por baja (CU05, FA1).
        boolean usaVacante = false;
        if (torneo.verificarPlazoVencido(fecha)) {
            if (vacantesDisponibles(torneo) == 0) {
                throw new PlazoVencidoException();
            }
            usaVacante = true;
        }

        // 2. El participante tiene que estar registrado en el sistema
        if (!participante.estaRegistrado()) {
            throw new DatoInvalidoException("El participante no está registrado en el sistema.");
        }

        // 3. Jugador para Single, pareja para Dobles y Mixto
        if (torneo.getModalidad().requierePareja() && !(participante instanceof Pareja)) {
            throw new DatoInvalidoException("La modalidad " + torneo.getModalidad() + " requiere inscribir una pareja.");
        }
        if (!torneo.getModalidad().requierePareja() && !(participante instanceof Jugador)) {
            throw new DatoInvalidoException("La modalidad Single requiere inscribir a un jugador individual.");
        }

        // 4. Sin inscripciones duplicadas (RF09), tampoco de un jugador que ya juega en otra pareja
        if (torneo.existeInscripcion(participante)) {
            throw new InscripcionDuplicadaException(participante.obtenerNombreCompleto());
        }
        if (participante instanceof Pareja pareja) {
            for (Inscripcion inscripcion : torneo.getInscripcionesActivas()) {
                if (inscripcion.getParticipante() instanceof Pareja otra
                        && (otra.contieneJugador(pareja.getJugador1()) || otra.contieneJugador(pareja.getJugador2()))) {
                    throw new DatoInvalidoException("Uno de los jugadores ya está inscripto en este torneo con otra pareja.");
                }
            }
        }

        // 5. Se crea la inscripción con su estado de pago (CU06) y se guarda en el torneo
        Inscripcion inscripcion = new Inscripcion(proximoIdInscripcion, participante, fecha, pagado);
        inscripcionDAO.insertar(torneo, inscripcion);
        torneo.agregarInscripcion(inscripcion);
        proximoIdInscripcion++;
        if (usaVacante) {
            vacantesPorBaja.put(torneo.getIdTorneo(), vacantesDisponibles(torneo) - 1);
        }
        return inscripcion;
    }

    /** Registra el pago de una inscripción pendiente, habilitando al participante (CU06, RF07 y RF08). */
    public void registrarPago(Inscripcion inscripcion) throws PrimerPuntoException {
        if (!inscripcion.estaActiva()) {
            throw new DatoInvalidoException("No se puede registrar el pago de una inscripción dada de baja.");
        }
        if (inscripcion.isEstadoPago()) {
            throw new DatoInvalidoException("El pago de esta inscripción ya estaba registrado.");
        }
        inscripcion.setEstadoPago(true);
        try {
            inscripcionDAO.actualizar(inscripcion);
        } catch (PersistenciaException e) {
            inscripcion.setEstadoPago(false);
            throw e;
        }
    }

    /** Inscripciones de un torneo (CU08, RF14). */
    public List<Inscripcion> consultarInscripciones(Torneo torneo) {
        return new ArrayList<>(torneo.getInscripciones());
    }

    /** Exporta a un archivo CSV los inscriptos del torneo con su estado de pago, para el control de cobros. */
    public void exportarInscriptos(Torneo torneo, String rutaArchivo) throws ArchivoException {
        exportador.exportarInscriptos(torneo, rutaArchivo);
    }

    public int vacantesDisponibles(Torneo torneo) {
        return vacantesPorBaja.getOrDefault(torneo.getIdTorneo(), 0);
    }

    // ------------------------------------------------------------ bajas

    /** Procesa una baja con la fecha actual del sistema (CU09). */
    public boolean procesarBaja(Torneo torneo, Inscripcion inscripcion, String motivo, boolean motivoAceptado)
            throws PrimerPuntoException {
        return procesarBaja(torneo, inscripcion, motivo, motivoAceptado, LocalDate.now());
    }

    /**
     * Da de baja una inscripción (CU09).
     * - Dentro del plazo: la baja se realiza directamente (RF15).
     * - Fuera del plazo: se exige un motivo (RF16) que el organizador acepta o rechaza (RF17).
     *   Si lo acepta, la baja se realiza sin deuda y se libera una vacante; si lo rechaza,
     *   la inscripción se mantiene con su estado de pago (RF18).
     *
     * @return true si el participante quedó dado de baja; false si el motivo fue rechazado
     */
    public boolean procesarBaja(Torneo torneo, Inscripcion inscripcion, String motivo, boolean motivoAceptado,
                                LocalDate fecha) throws PrimerPuntoException {
        if (!inscripcion.estaActiva()) {
            throw new DatoInvalidoException("La inscripción ya estaba dada de baja.");
        }
        if (!torneo.verificarPlazoVencido(fecha)) {
            darDeBaja(inscripcion, motivo);
            return true;
        }
        if (motivo == null || motivo.isBlank()) {
            throw new DatoInvalidoException("La baja está fuera de plazo: es obligatorio indicar el motivo.");
        }
        if (!motivoAceptado) {
            return false;
        }
        darDeBaja(inscripcion, motivo.trim());
        vacantesPorBaja.put(torneo.getIdTorneo(), vacantesDisponibles(torneo) + 1);
        return true;
    }

    /** Marca la baja y la guarda; si no se puede guardar, la inscripción queda como estaba. */
    private void darDeBaja(Inscripcion inscripcion, String motivo) throws PersistenciaException {
        EstadoInscripcion estadoAnterior = inscripcion.getEstadoInscripcion();
        String motivoAnterior = inscripcion.getMotivoBaja();
        inscripcion.setEstadoInscripcion(EstadoInscripcion.BAJA);
        inscripcion.setMotivoBaja(motivo);
        try {
            inscripcionDAO.actualizar(inscripcion);
        } catch (PersistenciaException e) {
            inscripcion.setEstadoInscripcion(estadoAnterior);
            inscripcion.setMotivoBaja(motivoAnterior);
            throw e;
        }
    }
}
