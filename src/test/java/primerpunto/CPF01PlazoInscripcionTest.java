package primerpunto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import primerpunto.excepciones.PlazoVencidoException;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.EstadoInscripcion;
import primerpunto.modelo.Inscripcion;
import primerpunto.modelo.Jugador;
import primerpunto.modelo.Modalidad;
import primerpunto.modelo.Torneo;
import primerpunto.negocio.GestionParticipantes;
import primerpunto.negocio.GestionTorneos;

/**
 * Caso de prueba CPF01 (caja negra, CU05): partición de equivalencia y análisis de
 * frontera sobre el plazo de inscripción (RF10). plazoInscripcion = 30/09/2026.
 */
@DisplayName("CPF01 - Inscribir participante: análisis de frontera del plazo de inscripción")
class CPF01PlazoInscripcionTest {

    private GestionParticipantes gestionParticipantes;
    private Torneo torneo;
    private Jugador sofia;

    /** Datos de prueba: «Torneo Apertura 2026» con dos inscriptos previos (Antonella y Lucas). */
    @BeforeEach
    void prepararDatos() throws PrimerPuntoException {
        GestionTorneos gestionTorneos = new GestionTorneos();
        gestionParticipantes = new GestionParticipantes();
        torneo = gestionTorneos.crearTorneo("Torneo Apertura 2026", Modalidad.SINGLE, 8500,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 9, 30), "Club Los Cuervos");
        Jugador antonella = gestionParticipantes.registrarJugador("Antonella", "Marinelli", "46512132", "", "antonella@gmail.com");
        Jugador lucas = gestionParticipantes.registrarJugador("Lucas", "Gomez", "38765432", "", "lucas@gmail.com");
        gestionParticipantes.registrarJugador("Marcos", "Diaz", "39876543", "", "marcos@gmail.com");
        sofia = gestionParticipantes.registrarJugador("Sofia", "Perez", "41234567", "", "sofia@gmail.com");
        gestionParticipantes.inscribirParticipante(torneo, antonella, true, LocalDate.of(2026, 9, 20));
        gestionParticipantes.inscribirParticipante(torneo, lucas, true, LocalDate.of(2026, 9, 19));
    }

    @Test
    @DisplayName("29/09/2026 (límite inferior) - inscripción aceptada, el torneo pasa de 2 a 3 inscriptos")
    void limiteInferiorAceptado() throws PrimerPuntoException {
        Inscripcion inscripcion = gestionParticipantes.inscribirParticipante(torneo, sofia, true, LocalDate.of(2026, 9, 29));
        assertEquals(EstadoInscripcion.CONFIRMADA, inscripcion.getEstadoInscripcion());
        assertEquals(LocalDate.of(2026, 9, 29), inscripcion.getFecha());
        assertEquals(3, torneo.getInscripcionesActivas().size());
    }

    @Test
    @DisplayName("30/09/2026 (frontera) - inscripción aceptada, el torneo pasa de 2 a 3 inscriptos")
    void fronteraAceptada() throws PrimerPuntoException {
        Inscripcion inscripcion = gestionParticipantes.inscribirParticipante(torneo, sofia, true, LocalDate.of(2026, 9, 30));
        assertEquals(EstadoInscripcion.CONFIRMADA, inscripcion.getEstadoInscripcion());
        assertEquals(3, torneo.getInscripcionesActivas().size());
    }

    @Test
    @DisplayName("01/10/2026 (límite superior) - «El plazo de inscripción del torneo ya finalizó.», sigue con 2 inscriptos")
    void limiteSuperiorRechazado() {
        PlazoVencidoException error = assertThrows(PlazoVencidoException.class,
                () -> gestionParticipantes.inscribirParticipante(torneo, sofia, true, LocalDate.of(2026, 10, 1)));
        assertEquals("El plazo de inscripción del torneo ya finalizó.", error.getMessage());
        assertEquals(2, torneo.getInscripcionesActivas().size());
    }
}
