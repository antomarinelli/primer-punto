package primerpunto.excepciones;

/** El participante ya tiene una inscripción activa en el torneo (RF09). */
public class InscripcionDuplicadaException extends PrimerPuntoException {

    public InscripcionDuplicadaException(String participante) {
        super(participante + " ya está inscripto en este torneo.");
    }
}
