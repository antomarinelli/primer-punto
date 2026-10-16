package primerpunto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import primerpunto.excepciones.DatoInvalidoException;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.Cuadro;
import primerpunto.modelo.Jugador;
import primerpunto.modelo.Modalidad;
import primerpunto.modelo.Partido;
import primerpunto.modelo.Resultado;
import primerpunto.modelo.Torneo;
import primerpunto.negocio.GestionCuadrosZonas;
import primerpunto.negocio.GestionParticipantes;
import primerpunto.negocio.GestionTorneos;

/**
 * Caso de prueba CPS04 (pruebas funcionales, CU10): cálculo de byes y armado de
 * cruces de un cuadro de eliminación directa (RF19).
 */
@DisplayName("CPS04 - Generar cuadro: cálculo de byes y armado de cruces")
class CPS04GenerarCuadroTest {

    private GestionTorneos gestionTorneos;
    private GestionParticipantes gestionParticipantes;
    private GestionCuadrosZonas gestionCuadros;

    /** Torneo Single con la inscripción cerrada y la cantidad indicada de inscriptos habilitados. */
    private Torneo torneoConInscriptos(int cantidad) throws PrimerPuntoException {
        gestionTorneos = new GestionTorneos();
        gestionParticipantes = new GestionParticipantes();
        gestionCuadros = new GestionCuadrosZonas(gestionTorneos);
        Torneo torneo = gestionTorneos.crearTorneo("Torneo de prueba", Modalidad.SINGLE, 8500,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 9, 30), "Club Los Cuervos");
        for (int i = 1; i <= cantidad; i++) {
            Jugador jugador = gestionParticipantes.registrarJugador("Jugador", "Numero" + i,
                    String.valueOf(30000000 + i), "", "jugador" + i + "@email.com");
            gestionParticipantes.inscribirParticipante(torneo, jugador, true, LocalDate.of(2026, 9, 20));
        }
        gestionTorneos.cerrarInscripcion(torneo);
        return torneo;
    }

    @ParameterizedTest(name = "{0} inscriptos - cuadro de {1}, {2} byes, {3} partidos en primera ronda")
    @CsvSource({
        "2, 2, 0, 1",
        "5, 8, 3, 1",
        "8, 8, 0, 4",
        "11, 16, 5, 3"
    })
    void calculaTamanioByesYPartidos(int inscriptos, int tamanioEsperado, int byesEsperados,
                                     int partidosEsperados) throws PrimerPuntoException {
        Torneo torneo = torneoConInscriptos(inscriptos);
        Cuadro cuadro = gestionCuadros.generarCuadro(torneo, java.util.List.of(), true);

        assertEquals(tamanioEsperado, cuadro.getTamanio(), "Tamaño del cuadro");
        assertEquals(byesEsperados, cuadro.getCantidadByes(), "Cantidad de byes");
        long partidosPrimeraRonda = cuadro.getPartidos().stream().filter(p -> p.getRonda() == 0).count();
        assertEquals(partidosEsperados, partidosPrimeraRonda, "Partidos de primera ronda");
    }

    @Test
    @DisplayName("1 inscripto - «No hay inscriptos suficientes para generar el formato.»")
    void rechazaCuadroConUnSoloInscripto() throws PrimerPuntoException {
        Torneo torneo = torneoConInscriptos(1);
        DatoInvalidoException error = assertThrows(DatoInvalidoException.class,
                () -> gestionCuadros.generarCuadro(torneo, java.util.List.of(), true));
        assertEquals("No hay inscriptos suficientes para generar el formato.", error.getMessage());
    }

    @Test
    @DisplayName("Un cuadro de 5 inscriptos se juega completo hasta definir al campeón")
    void elCuadroAvanzaHastaElCampeon() throws PrimerPuntoException {
        Torneo torneo = torneoConInscriptos(5);
        Cuadro cuadro = gestionCuadros.generarCuadro(torneo, java.util.List.of(), true);
        int resultados = 1;
        // Al armarse, el cuadro ya tiene creados todos sus partidos (los de rondas futuras, "a definir")
        assertEquals(4, cuadro.getPartidos().size(), "Con 5 participantes se juegan 4 partidos");
        // Se juega siempre el primer partido pendiente ya definido; gana el participante 1
        Partido pendiente = buscarPendiente(cuadro);
        while (pendiente != null) {
            pendiente.setResultado(new Resultado(resultados++, "6-4 6-3", pendiente.getParticipante1(), false, false));
            cuadro.procesarResultado(pendiente);
            pendiente = buscarPendiente(cuadro);
        }
        assertNotNull(cuadro.getCampeon(), "Debe haber un campeón");
    }

    private Partido buscarPendiente(Cuadro cuadro) {
        for (Partido partido : cuadro.getPartidos()) {
            if (!partido.estaFinalizado() && partido.estaDefinido()) {
                return partido;
            }
        }
        return null;
    }
}
