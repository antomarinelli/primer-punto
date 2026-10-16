package primerpunto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import primerpunto.excepciones.DatoInvalidoException;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.EstadoInscripcion;
import primerpunto.modelo.Inscripcion;
import primerpunto.modelo.Jugador;
import primerpunto.modelo.Modalidad;
import primerpunto.modelo.Torneo;
import primerpunto.negocio.GestionParticipantes;
import primerpunto.negocio.GestionTorneos;

/**
 * Caso de prueba CPS03 (pruebas funcionales, CU09): caminos de la baja de una
 * inscripción (RF15, RF16, RF17 y RF18). plazoInscripcion = 30/09/2026.
 */
@DisplayName("CPS03 - Dar de baja una inscripción: dentro y fuera de plazo")
class CPS03DarDeBajaTest {

    private static final LocalDate FUERA_DE_PLAZO = LocalDate.of(2026, 10, 3);

    private GestionParticipantes gestionParticipantes;
    private Torneo torneo;
    private Inscripcion inscripcionAntonella;  // id 1, pago abonado
    private Inscripcion inscripcionLucas;      // id 2, pago abonado
    private Inscripcion inscripcionSofia;      // id 3, pago pendiente

    @BeforeEach
    void prepararDatos() throws PrimerPuntoException {
        GestionTorneos gestionTorneos = new GestionTorneos();
        gestionParticipantes = new GestionParticipantes();
        torneo = gestionTorneos.crearTorneo("Torneo Apertura 2026", Modalidad.SINGLE, 8500,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 9, 30), "Club Los Cuervos");
        Jugador antonella = gestionParticipantes.registrarJugador("Antonella", "Marinelli", "46512132", "", "antonella@gmail.com");
        Jugador lucas = gestionParticipantes.registrarJugador("Lucas", "Gomez", "38765432", "", "lucas@gmail.com");
        Jugador sofia = gestionParticipantes.registrarJugador("Sofia", "Perez", "41234567", "", "sofia@gmail.com");
        inscripcionAntonella = gestionParticipantes.inscribirParticipante(torneo, antonella, true, LocalDate.of(2026, 9, 20));
        inscripcionLucas = gestionParticipantes.inscribirParticipante(torneo, lucas, true, LocalDate.of(2026, 9, 19));
        inscripcionSofia = gestionParticipantes.inscribirParticipante(torneo, sofia, false, LocalDate.of(2026, 9, 23));
    }

    @Test
    @DisplayName("E1: baja dentro de plazo (25/09/2026) - estado 'Baja', el torneo queda con 2 inscriptos activos")
    void bajaDentroDePlazo() throws DatoInvalidoException {
        boolean dadoDeBaja = gestionParticipantes.procesarBaja(torneo, inscripcionLucas, null, false,
                LocalDate.of(2026, 9, 25));
        assertTrue(dadoDeBaja);
        assertEquals(EstadoInscripcion.BAJA, inscripcionLucas.getEstadoInscripcion());
        assertEquals(2, torneo.getInscripcionesActivas().size());
    }

    @Test
    @DisplayName("E2: fuera de plazo con motivo aceptado - estado 'Baja', motivo registrado, sin deuda")
    void bajaFueraDePlazoMotivoAceptado() throws DatoInvalidoException {
        boolean dadoDeBaja = gestionParticipantes.procesarBaja(torneo, inscripcionAntonella,
                "Lesión de muñeca con certificado médico", true, FUERA_DE_PLAZO);
        assertTrue(dadoDeBaja);
        assertEquals(EstadoInscripcion.BAJA, inscripcionAntonella.getEstadoInscripcion());
        assertEquals("Lesión de muñeca con certificado médico", inscripcionAntonella.getMotivoBaja());
        assertFalse(inscripcionAntonella.tieneDeuda());
        assertEquals(1, gestionParticipantes.vacantesDisponibles(torneo), "Se libera una vacante (CU05, FA1)");
    }

    @Test
    @DisplayName("E3: fuera de plazo con motivo rechazado - la inscripción no cambia y el pago sigue adeudado")
    void bajaFueraDePlazoMotivoRechazado() throws DatoInvalidoException {
        boolean dadoDeBaja = gestionParticipantes.procesarBaja(torneo, inscripcionSofia, "Viaje personal", false,
                FUERA_DE_PLAZO);
        assertFalse(dadoDeBaja, "La solicitud de baja no fue aceptada");
        assertEquals(EstadoInscripcion.PENDIENTE, inscripcionSofia.getEstadoInscripcion());
        assertFalse(inscripcionSofia.isEstadoPago());
        assertTrue(inscripcionSofia.tieneDeuda());
        assertEquals(3, torneo.getInscripcionesActivas().size());
    }

    @Test
    @DisplayName("Fuera de plazo sin motivo - se exige indicar el motivo (RF16)")
    void bajaFueraDePlazoSinMotivo() {
        DatoInvalidoException error = assertThrows(DatoInvalidoException.class,
                () -> gestionParticipantes.procesarBaja(torneo, inscripcionLucas, " ", true, FUERA_DE_PLAZO));
        assertEquals("La baja está fuera de plazo: es obligatorio indicar el motivo.", error.getMessage());
    }
}
