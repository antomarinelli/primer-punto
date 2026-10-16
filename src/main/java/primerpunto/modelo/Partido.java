package primerpunto.modelo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Partido entre dos participantes, generado por un formato (cuadro o zona).
 * Se programa en una cancha, fecha y horario, y finaliza al cargarse su resultado.
 *
 * En un cuadro, los partidos de las rondas siguientes se crean desde el principio
 * con sus participantes "a definir": así se les puede reservar cancha y horario
 * antes de conocer a los ganadores de la ronda anterior.
 */
public class Partido {

    private final int idPartido;
    private Participante participante1;
    private Participante participante2;
    private final String instancia;   // por ejemplo "Semifinal" o "Zona 1"
    private final int ronda;          // número de ronda dentro del cuadro (en una zona, siempre 0)
    private final int posicion;       // lugar del partido dentro de su ronda
    private Cancha cancha;
    private LocalDate fecha;
    private LocalTime horario;
    private EstadoPartido estado;
    private Resultado resultado;

    public Partido(int idPartido, Participante participante1, Participante participante2,
                   String instancia, int ronda, int posicion) {
        if (participante1 != null && participante1.equals(participante2)) {
            throw new IllegalArgumentException("Un participante no puede jugar contra sí mismo.");
        }
        this.idPartido = idPartido;
        this.participante1 = participante1;
        this.participante2 = participante2;
        this.instancia = instancia;
        this.ronda = ronda;
        this.posicion = posicion;
        this.estado = EstadoPartido.PENDIENTE;
    }

    public int getIdPartido() {
        return idPartido;
    }

    public Participante getParticipante1() {
        return participante1;
    }

    public Participante getParticipante2() {
        return participante2;
    }

    public String getInstancia() {
        return instancia;
    }

    public int getRonda() {
        return ronda;
    }

    public int getPosicion() {
        return posicion;
    }

    public Cancha getCancha() {
        return cancha;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHorario() {
        return horario;
    }

    public EstadoPartido getEstado() {
        return estado;
    }

    public Resultado getResultado() {
        return resultado;
    }

    public Participante getGanador() {
        return resultado == null ? null : resultado.getGanador();
    }

    /** Un partido está definido cuando ya se conocen sus dos participantes. */
    public boolean estaDefinido() {
        return participante1 != null && participante2 != null;
    }

    public boolean estaProgramado() {
        return cancha != null;
    }

    public boolean estaFinalizado() {
        return estado == EstadoPartido.FINALIZADO;
    }

    public boolean participa(Participante participante) {
        return participante != null && (participante.equals(participante1) || participante.equals(participante2));
    }

    /** Lo usa el cuadro para completar el partido cuando avanza el ganador de la ronda anterior. */
    void setParticipante(int numero, Participante participante) {
        if (numero == 1) {
            participante1 = participante;
        } else {
            participante2 = participante;
        }
    }

    /** Asigna (o cambia) la cancha, la fecha y el horario del partido. */
    public void setHorarioYCancha(Cancha cancha, LocalDate fecha, LocalTime horario) {
        this.cancha = cancha;
        this.fecha = fecha;
        this.horario = horario;
        if (estado != EstadoPartido.FINALIZADO) {
            estado = EstadoPartido.PROGRAMADO;
        }
    }

    /** Libera la cancha y el horario asignados: el partido vuelve a quedar sin programar. */
    public void quitarProgramacion() {
        this.cancha = null;
        this.fecha = null;
        this.horario = null;
        if (estado != EstadoPartido.FINALIZADO) {
            estado = EstadoPartido.PENDIENTE;
        }
    }

    /** Guarda el resultado (o lo reemplaza, si se corrige uno ya cargado) y da el partido por finalizado. */
    public void setResultado(Resultado resultado) {
        this.resultado = resultado;
        this.estado = EstadoPartido.FINALIZADO;
    }

    public String getNombreParticipante1() {
        return participante1 == null ? "A definir" : participante1.obtenerNombreCompleto();
    }

    public String getNombreParticipante2() {
        return participante2 == null ? "A definir" : participante2.obtenerNombreCompleto();
    }

    @Override
    public String toString() {
        return getNombreParticipante1() + " vs. " + getNombreParticipante2();
    }
}
