package primerpunto.excepciones;

/** Se intentó inscribir a un participante después del cierre del plazo de inscripción (RF10). */
public class PlazoVencidoException extends PrimerPuntoException {

    public PlazoVencidoException() {
        super("El plazo de inscripción del torneo ya finalizó.");
    }
}
