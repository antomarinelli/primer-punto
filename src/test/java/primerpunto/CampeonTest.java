package primerpunto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import primerpunto.excepciones.DatoInvalidoException;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.Jugador;
import primerpunto.modelo.Modalidad;
import primerpunto.modelo.Partido;
import primerpunto.modelo.Torneo;
import primerpunto.modelo.Zona;
import primerpunto.negocio.GestionCuadrosZonas;
import primerpunto.negocio.GestionParticipantes;
import primerpunto.negocio.GestionPartidosCanchas;
import primerpunto.negocio.GestionTorneos;

/**
 * Campeón de un torneo solo de zonas: con una zona es el primero de la tabla, una vez
 * jugados todos los partidos; con varias zonas hay que jugar el cuadro final.
 */
@DisplayName("Campeón de un torneo por zonas")
class CampeonTest {

    private GestionTorneos gestionTorneos;
    private GestionParticipantes gestionParticipantes;
    private GestionCuadrosZonas gestionCuadros;
    private GestionPartidosCanchas gestionPartidos;
    private final List<Jugador> jugadores = new ArrayList<>();

    @BeforeEach
    void prepararDatos() throws PrimerPuntoException {
        gestionTorneos = new GestionTorneos();
        gestionParticipantes = new GestionParticipantes();
        gestionCuadros = new GestionCuadrosZonas(gestionTorneos);
        gestionPartidos = new GestionPartidosCanchas(gestionTorneos);
        gestionPartidos.registrarCancha(1, LocalTime.of(8, 0), LocalTime.of(22, 0));
        gestionPartidos.registrarCancha(2, LocalTime.of(8, 0), LocalTime.of(22, 0));
    }

    private Torneo torneoConInscriptos(int cantidad) throws PrimerPuntoException {
        Torneo torneo = gestionTorneos.crearTorneo("Torneo de zonas", Modalidad.SINGLE, 8500,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 9, 30), "Club Los Cuervos");
        for (int i = 1; i <= cantidad; i++) {
            Jugador jugador = gestionParticipantes.registrarJugador("Jugador", "Numero" + i,
                    String.valueOf(30000000 + i), "", "jugador" + i + "@email.com");
            jugadores.add(jugador);
            gestionParticipantes.inscribirParticipante(torneo, jugador, true, LocalDate.of(2026, 9, 20));
        }
        gestionTorneos.cerrarInscripcion(torneo);
        return torneo;
    }

    /** Programa y juega todos los partidos pendientes: siempre gana el participante 1 del partido. */
    private void jugarTodo(Torneo torneo) throws PrimerPuntoException {
        gestionPartidos.programarPartidos(torneo, torneo.getFechaInicio());
        boolean quedanPendientes = true;
        while (quedanPendientes) {
            quedanPendientes = false;
            for (Partido partido : torneo.getPartidos()) {
                if (!partido.estaFinalizado() && partido.estaDefinido()) {
                    gestionPartidos.registrarResultado(torneo, partido, "6-0 6-0", null, false, false);
                    quedanPendientes = true;
                }
            }
        }
    }

    @Test
    @DisplayName("Una zona: el primero de la tabla es campeón recién cuando se jugaron todos los partidos")
    void unaZonaElPrimeroEsCampeon() throws PrimerPuntoException {
        Torneo torneo = torneoConInscriptos(4);
        Zona zona = gestionCuadros.generarZonas(torneo, 1, false).get(0);
        assertNull(gestionCuadros.obtenerCampeon(torneo), "Sin partidos jugados no hay campeón");

        jugarTodo(torneo);

        assertEquals(zona.calcularTablaPosiciones().get(0).getParticipante(), gestionCuadros.obtenerCampeon(torneo));
        assertEquals(jugadores.get(0), gestionCuadros.obtenerCampeon(torneo));
        assertDoesNotThrow(() -> gestionCuadros.validarFinalizacion(torneo));
    }

    @Test
    @DisplayName("Varias zonas: no se puede finalizar sin jugar el cuadro final")
    void variasZonasRequierenCuadroFinal() throws PrimerPuntoException {
        Torneo torneo = torneoConInscriptos(6);
        gestionCuadros.generarZonas(torneo, 2, false);
        jugarTodo(torneo);

        assertNull(gestionCuadros.obtenerCampeon(torneo));
        DatoInvalidoException error = assertThrows(DatoInvalidoException.class,
                () -> gestionCuadros.validarFinalizacion(torneo));
        assertEquals("El torneo tiene varias zonas: para definir al campeón hay que generar y jugar el cuadro final "
                + "con los clasificados.", error.getMessage());

        gestionCuadros.generarCuadroDesdeZonas(torneo, 1);
        jugarTodo(torneo);
        assertEquals(gestionCuadros.obtenerCuadro(torneo).getCampeon(), gestionCuadros.obtenerCampeon(torneo));
        assertDoesNotThrow(() -> gestionCuadros.validarFinalizacion(torneo));
    }
}
