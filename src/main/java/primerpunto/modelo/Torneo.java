package primerpunto.modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Torneo de tenis del club, con sus datos generales y las inscripciones que recibe.
 *
 * El torneo solo guarda sus datos y responde sobre ellos (por ejemplo, si venció
 * el plazo de inscripción). Crearlo, inscribir o finalizarlo lo hacen los controladores.
 */
public class Torneo {

    private final int idTorneo;
    private final String nombre;
    private final Modalidad modalidad;
    private final double costo;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
    private final LocalDate plazoInscripcion;
    private final String lugar;
    private final int duracionPartidoMinutos;
    private final FormatoResultado formatoResultado;
    private EstadoTorneo estado;
    private final List<Inscripcion> inscripciones = new ArrayList<>();
    private final List<Formato> formatos = new ArrayList<>();

    /** Torneo con partidos de 1 h 30 min al mejor de 3 sets (los valores más habituales). */
    public Torneo(int idTorneo, String nombre, Modalidad modalidad, double costo, LocalDate fechaInicio,
                  LocalDate fechaFin, LocalDate plazoInscripcion, String lugar) {
        this(idTorneo, nombre, modalidad, costo, fechaInicio, fechaFin, plazoInscripcion, lugar, 90,
                FormatoResultado.MEJOR_DE_TRES);
    }

    /**
     * @param duracionPartidoMinutos  tiempo promedio de un partido, que se reserva en la cancha
     * @param formatoResultado        cómo se juegan y se cargan los resultados (sets, super tie-break...)
     */
    public Torneo(int idTorneo, String nombre, Modalidad modalidad, double costo, LocalDate fechaInicio,
                  LocalDate fechaFin, LocalDate plazoInscripcion, String lugar, int duracionPartidoMinutos,
                  FormatoResultado formatoResultado) {
        if (duracionPartidoMinutos < 30 || duracionPartidoMinutos > 300) {
            throw new IllegalArgumentException("La duración promedio de un partido debe estar entre 30 minutos y 5 horas.");
        }
        if (formatoResultado == null) {
            throw new IllegalArgumentException("Elegí el formato de resultado de los partidos.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El torneo debe tener un nombre.");
        }
        if (modalidad == null || fechaInicio == null || fechaFin == null) {
            throw new IllegalArgumentException("La modalidad y las fechas de inicio y fin son obligatorias.");
        }
        if (costo < 0) {
            throw new IllegalArgumentException("El costo de inscripción no puede ser negativo.");
        }
        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio.");
        }
        if (plazoInscripcion != null && plazoInscripcion.isAfter(fechaInicio)) {
            throw new IllegalArgumentException("El plazo de inscripción debe cerrar antes del inicio del torneo.");
        }
        this.idTorneo = idTorneo;
        this.nombre = nombre.trim();
        this.modalidad = modalidad;
        this.costo = costo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.plazoInscripcion = plazoInscripcion;
        this.lugar = lugar;
        this.duracionPartidoMinutos = duracionPartidoMinutos;
        this.formatoResultado = formatoResultado;
        this.estado = EstadoTorneo.INSCRIPCION_ABIERTA;
    }

    public int getDuracionPartidoMinutos() {
        return duracionPartidoMinutos;
    }

    public FormatoResultado getFormatoResultado() {
        return formatoResultado;
    }

    public int getIdTorneo() {
        return idTorneo;
    }

    public String getNombre() {
        return nombre;
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public double getCosto() {
        return costo;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public LocalDate getPlazoInscripcion() {
        return plazoInscripcion;
    }

    public String getLugar() {
        return lugar;
    }

    public EstadoTorneo getEstado() {
        return estado;
    }

    public void setEstado(EstadoTorneo estado) {
        this.estado = estado;
    }

    /** Devuelve las inscripciones en modo solo lectura: se agregan únicamente con agregarInscripcion(). */
    public List<Inscripcion> getInscripciones() {
        return Collections.unmodifiableList(inscripciones);
    }

    /** Guarda una inscripción ya validada y creada por GestionParticipantes. */
    public void agregarInscripcion(Inscripcion inscripcion) {
        inscripciones.add(inscripcion);
    }

    /** Formatos de competencia del torneo: un cuadro, una o más zonas, o zonas y luego un cuadro. */
    public List<Formato> getFormatos() {
        return Collections.unmodifiableList(formatos);
    }

    /** Guarda un formato ya generado por GestionCuadrosZonas. */
    public void agregarFormato(Formato formato) {
        formatos.add(formato);
    }

    /** Descarta los formatos generados, para volver a generarlos (lo decide GestionCuadrosZonas). */
    public void quitarFormatos() {
        formatos.clear();
    }

    /** Todos los partidos del torneo, de todos sus formatos. */
    public List<Partido> getPartidos() {
        List<Partido> partidos = new ArrayList<>();
        for (Formato formato : formatos) {
            partidos.addAll(formato.getPartidos());
        }
        return partidos;
    }

    /** Participantes habilitados para jugar: inscripción activa y pago registrado (RF08). */
    public List<Participante> getParticipantesHabilitados() {
        List<Participante> habilitados = new ArrayList<>();
        for (Inscripcion inscripcion : inscripciones) {
            if (inscripcion.estaHabilitado()) {
                habilitados.add(inscripcion.getParticipante());
            }
        }
        return habilitados;
    }

    /** Inscripciones que no fueron dadas de baja. */
    public List<Inscripcion> getInscripcionesActivas() {
        List<Inscripcion> activas = new ArrayList<>();
        for (Inscripcion inscripcion : inscripciones) {
            if (inscripcion.estaActiva()) {
                activas.add(inscripcion);
            }
        }
        return activas;
    }

    /** Indica si el participante ya tiene una inscripción activa en este torneo (RF09). */
    public boolean existeInscripcion(Participante participante) {
        for (Inscripcion inscripcion : inscripciones) {
            if (inscripcion.estaActiva() && inscripcion.getParticipante().equals(participante)) {
                return true;
            }
        }
        return false;
    }

    /** Verifica el plazo contra la fecha actual del sistema. */
    public boolean verificarPlazoVencido() {
        return verificarPlazoVencido(LocalDate.now());
    }

    /**
     * Verifica si el plazo de inscripción venció en la fecha indicada (RF10 y RF12).
     * El día límite todavía está dentro del plazo. Si el torneo no tiene plazo
     * configurado, se considera no vencido (corrección del defecto DEF02).
     */
    public boolean verificarPlazoVencido(LocalDate fecha) {
        if (plazoInscripcion == null) {
            return false;
        }
        return fecha.isAfter(plazoInscripcion);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
