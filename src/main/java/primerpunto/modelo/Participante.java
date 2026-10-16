package primerpunto.modelo;

/**
 * Representa a quien se inscribe y compite en un torneo: un jugador (modalidad
 * Single) o una pareja (modalidades Dobles y Mixto).
 *
 * No se instancia directamente: siempre es un Jugador o una Pareja. Guarda lo que
 * ambos tienen en común y cada subclase arma su nombre completo.
 */
public abstract class Participante {

    private final int idParticipante;
    private final String nombreRepresentativo;

    public Participante(int idParticipante, String nombreRepresentativo) {
        if (nombreRepresentativo == null || nombreRepresentativo.isBlank()) {
            throw new IllegalArgumentException("El participante debe tener un nombre.");
        }
        this.idParticipante = idParticipante;
        this.nombreRepresentativo = nombreRepresentativo.trim();
    }

    public int getIdParticipante() {
        return idParticipante;
    }

    public String getNombreRepresentativo() {
        return nombreRepresentativo;
    }

    /** Un participante está registrado cuando el sistema ya le asignó un identificador. */
    public boolean estaRegistrado() {
        return idParticipante > 0;
    }

    /** Nombre para mostrar: "Nombre Apellido" para un jugador, "A. Apellido / B. Apellido" para una pareja. */
    public abstract String obtenerNombreCompleto();

    @Override
    public String toString() {
        return obtenerNombreCompleto();
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Participante p && p.idParticipante == idParticipante;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idParticipante);
    }
}
