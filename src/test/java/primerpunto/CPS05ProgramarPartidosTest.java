package primerpunto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import primerpunto.excepciones.CanchaOcupadaException;
import primerpunto.excepciones.DatoInvalidoException;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.Cancha;
import primerpunto.modelo.Cuadro;
import primerpunto.modelo.Jugador;
import primerpunto.modelo.Modalidad;
import primerpunto.modelo.Partido;
import primerpunto.modelo.Torneo;
import primerpunto.negocio.GestionCuadrosZonas;
import primerpunto.negocio.GestionParticipantes;
import primerpunto.negocio.GestionPartidosCanchas;
import primerpunto.negocio.GestionTorneos;

/**
 * Caso de prueba CPS05 (pruebas funcionales, CU11): la programación no asigna dos
 * partidos a la misma cancha en el mismo horario y respeta el horario de la cancha (RF20).
 */
@DisplayName("CPS05 - Programar partidos: canchas sin superposición de horarios")
class CPS05ProgramarPartidosTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 10);

    private GestionPartidosCanchas gestionPartidos;
    private Torneo torneo;
    private Cancha cancha1;
    private Cancha cancha2;
    private Partido partidoNuevo;

    /** Canchas 1 y 2 de 08:00 a 22:00, y un partido ya programado en la cancha 1 el 10/10/2026 a las 10:00. */
    @BeforeEach
    void prepararDatos() throws PrimerPuntoException {
        GestionTorneos gestionTorneos = new GestionTorneos();
        GestionParticipantes gestionParticipantes = new GestionParticipantes();
        GestionCuadrosZonas gestionCuadros = new GestionCuadrosZonas(gestionTorneos);
        gestionPartidos = new GestionPartidosCanchas(gestionTorneos);

        torneo = gestionTorneos.crearTorneo("Torneo Apertura 2026", Modalidad.SINGLE, 8500,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 9, 30), "Club Los Cuervos");
        String[][] jugadores = {
            {"Antonella", "Marinelli", "46512132"}, {"Marcos", "Diaz", "39876543"},
            {"Lucas", "Gomez", "38765432"}, {"Sofia", "Perez", "41234567"}
        };
        for (String[] datos : jugadores) {
            Jugador jugador = gestionParticipantes.registrarJugador(datos[0], datos[1], datos[2], "", "contacto@email.com");
            gestionParticipantes.inscribirParticipante(torneo, jugador, true, LocalDate.of(2026, 9, 20));
        }
        gestionTorneos.cerrarInscripcion(torneo);
        Cuadro cuadro = gestionCuadros.generarCuadro(torneo, List.of(), false);

        cancha1 = gestionPartidos.registrarCancha(1, LocalTime.of(8, 0), LocalTime.of(22, 0));
        cancha2 = gestionPartidos.registrarCancha(2, LocalTime.of(8, 0), LocalTime.of(22, 0));
        Partido partidoProgramado = cuadro.getPartidos().get(0);
        partidoNuevo = cuadro.getPartidos().get(1);
        gestionPartidos.programarPartido(torneo, partidoProgramado, cancha1, DIA, LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("E1: cancha 1, 10/10/2026, 10:00 - rechazado: «La cancha 1 ya tiene un partido asignado...»")
    void canchaOcupada() {
        CanchaOcupadaException error = assertThrows(CanchaOcupadaException.class,
                () -> gestionPartidos.programarPartido(torneo, partidoNuevo, cancha1, DIA, LocalTime.of(10, 0)));
        assertEquals("La cancha 1 ya tiene un partido asignado el 10/10/2026 a las 10:00.", error.getMessage());
        assertEquals(null, partidoNuevo.getCancha(), "No se registra el partido");
    }

    @Test
    @DisplayName("E2: cancha 2, 10/10/2026, 10:00 - aceptado")
    void otraCanchaMismoHorario() throws PrimerPuntoException {
        gestionPartidos.programarPartido(torneo, partidoNuevo, cancha2, DIA, LocalTime.of(10, 0));
        assertEquals(cancha2, partidoNuevo.getCancha());
        assertEquals(DIA, partidoNuevo.getFecha());
        assertEquals(LocalTime.of(10, 0), partidoNuevo.getHorario());
    }

    @Test
    @DisplayName("E3: cancha 1, 10/10/2026, 12:00 - aceptado")
    void mismaCanchaOtroHorario() throws PrimerPuntoException {
        gestionPartidos.programarPartido(torneo, partidoNuevo, cancha1, DIA, LocalTime.of(12, 0));
        assertEquals(cancha1, partidoNuevo.getCancha());
        assertEquals(LocalTime.of(12, 0), partidoNuevo.getHorario());
    }

    @Test
    @DisplayName("E4: cancha 1, 10/10/2026, 23:00 - rechazado: fuera del horario de la cancha")
    void fueraDelHorarioDeLaCancha() {
        DatoInvalidoException error = assertThrows(DatoInvalidoException.class,
                () -> gestionPartidos.programarPartido(torneo, partidoNuevo, cancha1, DIA, LocalTime.of(23, 0)));
        assertEquals("El horario seleccionado está fuera del horario disponible de la cancha.", error.getMessage());
    }

    @Test
    @DisplayName("La programación automática no superpone partidos en la misma cancha")
    void programacionAutomaticaSinSuperposicion() throws PrimerPuntoException {
        gestionPartidos.programarPartidos(torneo, torneo.getFechaInicio());
        Partido a = torneo.getPartidos().get(0);
        Partido b = torneo.getPartidos().get(1);
        boolean mismoTurno = a.getFecha().equals(b.getFecha()) && a.getHorario().equals(b.getHorario());
        assertEquals(false, mismoTurno && a.getCancha() == b.getCancha());
    }
}
