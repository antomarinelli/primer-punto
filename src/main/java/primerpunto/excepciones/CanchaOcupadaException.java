package primerpunto.excepciones;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** La cancha ya tiene un partido asignado en ese horario (RF20). */
public class CanchaOcupadaException extends PrimerPuntoException {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CanchaOcupadaException(int numeroCancha, LocalDate fecha, LocalTime horario) {
        super("La cancha " + numeroCancha + " ya tiene un partido asignado el " + fecha.format(FECHA)
                + " a las " + horario + ".");
    }
}
