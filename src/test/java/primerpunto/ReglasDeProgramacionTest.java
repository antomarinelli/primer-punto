package primerpunto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import primerpunto.excepciones.DatoInvalidoException;
import primerpunto.excepciones.DescansoInsuficienteException;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.Cancha;
import primerpunto.modelo.Cuadro;
import primerpunto.modelo.Jugador;
import primerpunto.modelo.Modalidad;
import primerpunto.modelo.Participante;
import primerpunto.modelo.Partido;
import primerpunto.modelo.Torneo;
import primerpunto.modelo.Zona;
import primerpunto.negocio.GestionCuadrosZonas;
import primerpunto.negocio.GestionParticipantes;
import primerpunto.negocio.GestionPartidosCanchas;
import primerpunto.negocio.GestionTorneos;

/**
 * Reglas incorporadas a la programación y al armado del formato: descanso mínimo de
 * 30 minutos entre partidos (salvo que el organizador confirme), máximo de dos
 * partidos por día por jugador y armado manual del cuadro.
 */
@DisplayName("Reglas de programación y armado manual del formato")
class ReglasDeProgramacionTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 10);

    private GestionTorneos gestionTorneos;
    private GestionParticipantes gestionParticipantes;
    private GestionCuadrosZonas gestionCuadros;
    private GestionPartidosCanchas gestionPartidos;
    private Cancha cancha1;
    private Cancha cancha2;

    @BeforeEach
    void prepararDatos() throws PrimerPuntoException {
        gestionTorneos = new GestionTorneos();
        gestionParticipantes = new GestionParticipantes();
        gestionCuadros = new GestionCuadrosZonas(gestionTorneos);
        gestionPartidos = new GestionPartidosCanchas(gestionTorneos);
        cancha1 = gestionPartidos.registrarCancha(1, LocalTime.of(8, 0), LocalTime.of(22, 0));
        cancha2 = gestionPartidos.registrarCancha(2, LocalTime.of(8, 0), LocalTime.of(22, 0));
    }

    private Torneo torneoConInscriptos(int cantidad) throws PrimerPuntoException {
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

    @Test
    @DisplayName("Descanso: sin 30 minutos de descanso se pide confirmación; con 30 minutos se programa")
    void respetaElDescansoMinimo() throws PrimerPuntoException {
        Torneo torneo = torneoConInscriptos(4);
        Cuadro cuadro = gestionCuadros.generarCuadro(torneo, List.of(), false);
        Partido semifinal = cuadro.buscarPartido(0, 0);
        Partido fin = cuadro.buscarPartido(1, 0);
        gestionPartidos.programarPartido(torneo, semifinal, cancha1, DIA, LocalTime.of(10, 0)); // 1 h 30 min: termina 11:30

        DescansoInsuficienteException error = assertThrows(DescansoInsuficienteException.class,
                () -> gestionPartidos.programarPartido(torneo, fin, cancha2, DIA, LocalTime.of(11, 30)));
        assertTrue(error.getMessage().contains("descanso"), error.getMessage());
        assertDoesNotThrow(() -> gestionPartidos.programarPartido(torneo, fin, cancha2, DIA, LocalTime.of(12, 0)));
    }

    @Test
    @DisplayName("Descanso: el organizador puede programar igual si el jugador pide jugar seguido")
    void permiteJugarSeguidoSiSeConfirma() throws PrimerPuntoException {
        Torneo torneo = torneoConInscriptos(4);
        Cuadro cuadro = gestionCuadros.generarCuadro(torneo, List.of(), false);
        gestionPartidos.programarPartido(torneo, cuadro.buscarPartido(0, 0), cancha1, DIA, LocalTime.of(10, 0));
        Partido fin = cuadro.buscarPartido(1, 0);
        gestionPartidos.programarPartido(torneo, fin, cancha2, DIA, LocalTime.of(11, 30), true); // sin descanso
        assertEquals(LocalTime.of(11, 30), fin.getHorario());
    }

    @Test
    @DisplayName("Máximo por día: un jugador no puede tener un tercer partido el mismo día")
    void maximoDosPartidosPorDia() throws PrimerPuntoException {
        Torneo torneo = torneoConInscriptos(4);
        Zona zona = gestionCuadros.generarZonas(torneo, 1, false).get(0); // 4 participantes: 3 partidos cada uno
        Participante jugador1 = zona.getParticipantes().get(0);
        List<Partido> partidosDelJugador1 = new ArrayList<>();
        for (Partido partido : zona.getPartidos()) {
            if (partido.participa(jugador1)) {
                partidosDelJugador1.add(partido);
            }
        }
        gestionPartidos.programarPartido(torneo, partidosDelJugador1.get(0), cancha1, DIA, LocalTime.of(8, 0));
        gestionPartidos.programarPartido(torneo, partidosDelJugador1.get(1), cancha1, DIA, LocalTime.of(11, 0));

        DatoInvalidoException error = assertThrows(DatoInvalidoException.class, () -> gestionPartidos
                .programarPartido(torneo, partidosDelJugador1.get(2), cancha1, DIA, LocalTime.of(14, 0)));
        assertTrue(error.getMessage().contains("máximo es 2"), error.getMessage());
    }

    @Test
    @DisplayName("Programación automática: cumple el descanso y el máximo diario para todos los jugadores")
    void programacionAutomaticaCumpleLasReglas() throws PrimerPuntoException {
        Torneo torneo = torneoConInscriptos(4);
        gestionCuadros.generarZonas(torneo, 1, false);
        gestionPartidos.programarPartidos(torneo, torneo.getFechaInicio());
        for (Partido a : torneo.getPartidos()) {
            long mismoDia = torneo.getPartidos().stream()
                    .filter(b -> b.getFecha().equals(a.getFecha()) && b.participa(a.getParticipante1())).count();
            assertTrue(mismoDia <= 2, "Más de dos partidos en un día para " + a.getParticipante1());
        }
    }

    @Test
    @DisplayName("Duración por torneo: con partidos de 1 hora, la final puede empezar 1 h 30 min después de la semifinal")
    void usaLaDuracionDelTorneo() throws PrimerPuntoException {
        Torneo torneo = gestionTorneos.crearTorneo("Single damas", Modalidad.SINGLE, 6000,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 9, 30), "Club Los Cuervos",
                60, primerpunto.modelo.FormatoResultado.UN_SET);
        for (int i = 1; i <= 4; i++) {
            Jugador jugadora = gestionParticipantes.registrarJugador("Jugadora", "Numero" + i,
                    String.valueOf(31000000 + i), "", "jugadora" + i + "@email.com");
            gestionParticipantes.inscribirParticipante(torneo, jugadora, true, LocalDate.of(2026, 9, 20));
        }
        gestionTorneos.cerrarInscripcion(torneo);
        Cuadro cuadro = gestionCuadros.generarCuadro(torneo, List.of(), false);
        gestionPartidos.programarPartido(torneo, cuadro.buscarPartido(0, 0), cancha1, DIA, LocalTime.of(10, 0)); // termina 11:00
        Partido fin = cuadro.buscarPartido(1, 0);
        assertDoesNotThrow(() -> gestionPartidos.programarPartido(torneo, fin, cancha2, DIA, LocalTime.of(11, 30)));
        assertEquals(60, gestionPartidos.duracionDe(fin));
    }

    @Test
    @DisplayName("Armado manual: el cuadro respeta el orden elegido por el organizador")
    void cuadroArmadoAMano() throws PrimerPuntoException {
        Torneo torneo = torneoConInscriptos(4);
        List<Participante> orden = new ArrayList<>(torneo.getParticipantesHabilitados());
        java.util.Collections.reverse(orden);
        Cuadro cuadro = gestionCuadros.generarCuadroManual(torneo, orden);
        assertEquals(orden.get(0), cuadro.buscarPartido(0, 0).getParticipante1());
        assertEquals(orden.get(1), cuadro.buscarPartido(0, 0).getParticipante2());
        assertEquals(orden.get(3), cuadro.buscarPartido(0, 1).getParticipante2());
    }
}
