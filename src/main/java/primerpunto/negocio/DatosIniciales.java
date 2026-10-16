package primerpunto.negocio;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.Cancha;
import primerpunto.modelo.Cuadro;
import primerpunto.modelo.FormatoResultado;
import primerpunto.modelo.Jugador;
import primerpunto.modelo.Modalidad;
import primerpunto.modelo.Pareja;
import primerpunto.modelo.Partido;
import primerpunto.modelo.Torneo;
import primerpunto.modelo.Zona;

/**
 * Carga datos de ejemplo al iniciar el prototipo, para poder recorrer todas las
 * funcionalidades sin cargar todo desde cero. Mientras el prototipo trabaje en
 * memoria, estos datos reemplazan a los que en la próxima etapa vendrán de MySQL.
 *
 * Las fechas se calculan a partir del día en que se ejecuta el programa, de modo
 * que siempre haya un torneo con la inscripción abierta, uno con el plazo vencido,
 * uno en curso y uno finalizado.
 */
public final class DatosIniciales {

    private DatosIniciales() {
    }

    public static void cargar(Sistema sistema) throws PrimerPuntoException {
        GestionTorneos torneos = sistema.getGestionTorneos();
        GestionParticipantes participantes = sistema.getGestionParticipantes();
        GestionCuadrosZonas cuadros = sistema.getGestionCuadrosZonas();
        GestionPartidosCanchas partidos = sistema.getGestionPartidosCanchas();
        LocalDate hoy = LocalDate.now();

        // Canchas del club
        Cancha cancha1 = partidos.registrarCancha(1, LocalTime.of(8, 0), LocalTime.of(22, 0));
        Cancha cancha2 = partidos.registrarCancha(2, LocalTime.of(8, 0), LocalTime.of(22, 0));
        partidos.registrarCancha(3, LocalTime.of(9, 0), LocalTime.of(21, 0));

        // Jugadores: nombre, apellido, DNI, celular y email
        String[][] datos = {
            {"Antonella", "Marinelli", "46512132", "351 555-0101", "antonella@email.com"},
            {"Marcos", "Diaz", "39876543", "351 555-0102", "marcos@email.com"},
            {"Lucas", "Gomez", "38765432", "351 555-0103", ""},
            {"Sofia", "Perez", "41234567", "351 555-0104", "sofia@email.com"},
            {"Juan", "Lopez", "37654321", "", "juan@email.com"},
            {"Federico", "Ruiz", "40123456", "351 555-0106", "federico@email.com"},
            {"Nicolas", "Suarez", "42345678", "351 555-0107", "nicolas@email.com"},
            {"Camila", "Paz", "43456789", "351 555-0108", "camila@email.com"},
            {"Valentina", "Rossi", "44567890", "351 555-0109", "valentina@email.com"},
            {"Tomas", "Fernandez", "36543210", "351 555-0110", ""},
            {"Julieta", "Molina", "45678901", "351 555-0111", "julieta@email.com"},
            {"Martin", "Acosta", "35432109", "351 555-0112", "martin@email.com"}
        };
        List<Jugador> j = new ArrayList<>();
        for (String[] d : datos) {
            j.add(participantes.registrarJugador(d[0], d[1], d[2], d[3], d[4]));
        }

        // 1. Torneo finalizado (historial): cuadro de 4 jugado completo
        Torneo invierno = torneos.crearTorneo("Torneo Invierno", Modalidad.SINGLE, 7000,
                hoy.minusDays(60), hoy.minusDays(55), hoy.minusDays(65), "Club Los Cuervos");
        inscribir(participantes, invierno, hoy.minusDays(70), j.get(0), j.get(2), j.get(4), j.get(6));
        torneos.cerrarInscripcion(invierno);
        cuadros.generarCuadro(invierno, List.of(), false);
        partidos.programarPartidos(invierno, invierno.getFechaInicio());
        jugarPendientes(partidos, invierno, new String[] {"6-3 6-4", "4-6 6-2 7-5"});
        jugarPendientes(partidos, invierno, new String[] {"7-6 6-4"});
        torneos.finalizarTorneo(invierno, cuadros.todosLosPartidosJugados(invierno));

        // 2. Torneo en curso: cuadro de 8, cuartos jugados, semifinales hoy y final mañana
        Torneo copa = torneos.crearTorneo("Copa Los Cuervos", Modalidad.SINGLE, 9000,
                hoy.minusDays(2), hoy.plusDays(5), hoy.minusDays(7), "Club Los Cuervos", 90, FormatoResultado.MEJOR_DE_TRES);
        inscribir(participantes, copa, hoy.minusDays(12),
                j.get(4), j.get(1), j.get(3), j.get(5), j.get(0), j.get(2), j.get(6), j.get(7));
        torneos.cerrarInscripcion(copa);
        Cuadro cuadroCopa = cuadros.generarCuadro(copa, List.of(), false);
        LocalTime[] horasCuartos = {LocalTime.of(9, 0), LocalTime.of(9, 0), LocalTime.of(11, 0), LocalTime.of(11, 0)};
        Cancha[] canchasCuartos = {cancha1, cancha2, cancha1, cancha2};
        for (int i = 0; i < 4; i++) {
            partidos.programarPartido(copa, cuadroCopa.buscarPartido(0, i), canchasCuartos[i], hoy.minusDays(2), horasCuartos[i]);
        }
        jugarPendientes(partidos, copa, new String[] {"6-2 6-3", "3-6 6-4 6-4", "6-1 6-0", "7-5 6-7 6-3"});
        partidos.programarPartido(copa, cuadroCopa.buscarPartido(1, 0), cancha2, hoy, LocalTime.of(18, 0));
        partidos.programarPartido(copa, cuadroCopa.buscarPartido(1, 1), cancha1, hoy, LocalTime.of(18, 0));
        partidos.programarPartido(copa, cuadroCopa.buscarPartido(2, 0), cancha1, hoy.plusDays(1), LocalTime.of(19, 0));

        // 3. Torneo de dobles con el plazo vencido: listo para generar el formato
        Torneo apertura = torneos.crearTorneo("Torneo Apertura 2026", Modalidad.DOBLES, 8500,
                hoy.plusDays(3), hoy.plusDays(10), hoy.minusDays(1), "Club Los Cuervos", 75,
                FormatoResultado.DOS_SETS_Y_SUPER_TIEBREAK);
        participantes.inscribirParticipante(apertura, participantes.asociarPareja(j.get(0), j.get(1)), true, hoy.minusDays(9));
        participantes.inscribirParticipante(apertura, participantes.asociarPareja(j.get(2), j.get(3)), true, hoy.minusDays(10));
        participantes.inscribirParticipante(apertura, participantes.asociarPareja(j.get(4), j.get(5)), true, hoy.minusDays(6));
        participantes.inscribirParticipante(apertura, participantes.asociarPareja(j.get(6), j.get(7)), true, hoy.minusDays(5));
        Pareja rossiFernandez = participantes.asociarPareja(j.get(8), j.get(9));
        participantes.inscribirParticipante(apertura, rossiFernandez, false, hoy.minusDays(3));
        torneos.actualizarEstados(hoy);

        // 4. Torneo con la inscripción abierta
        Torneo primavera = torneos.crearTorneo("Torneo Primavera", Modalidad.SINGLE, 6500,
                hoy.plusDays(20), hoy.plusDays(26), hoy.plusDays(12), "Club Los Cuervos", 60, FormatoResultado.PRO_SET);
        participantes.inscribirParticipante(primavera, j.get(8), true, hoy.minusDays(2));
        participantes.inscribirParticipante(primavera, j.get(9), true, hoy.minusDays(1));
        participantes.inscribirParticipante(primavera, j.get(10), false, hoy);

        // 5. Torneo de zonas en curso (mixto)
        Torneo mixto = torneos.crearTorneo("Mixto de Primavera", Modalidad.MIXTO, 8000,
                hoy.minusDays(1), hoy.plusDays(6), hoy.minusDays(4), "Club Los Cuervos", 75,
                FormatoResultado.DOS_SETS_Y_SUPER_TIEBREAK);
        participantes.inscribirParticipante(mixto, participantes.asociarPareja(j.get(0), j.get(9)), true, hoy.minusDays(8));
        participantes.inscribirParticipante(mixto, participantes.asociarPareja(j.get(3), j.get(4)), true, hoy.minusDays(8));
        participantes.inscribirParticipante(mixto, participantes.asociarPareja(j.get(7), j.get(2)), true, hoy.minusDays(7));
        participantes.inscribirParticipante(mixto, participantes.asociarPareja(j.get(8), j.get(11)), true, hoy.minusDays(6));
        participantes.inscribirParticipante(mixto, participantes.asociarPareja(j.get(10), j.get(5)), true, hoy.minusDays(6));
        participantes.inscribirParticipante(mixto, participantes.asociarPareja(j.get(1), j.get(6)), true, hoy.minusDays(5));
        torneos.cerrarInscripcion(mixto);
        List<Zona> zonas = cuadros.generarZonas(mixto, 2, false);
        partidos.programarPartidos(mixto, mixto.getFechaInicio());
        for (Zona zona : zonas) {
            Partido primero = zona.getPartidos().get(0);
            partidos.registrarResultado(mixto, primero, "6-4 6-4", null, false, false);
        }
    }

    private static void inscribir(GestionParticipantes participantes, Torneo torneo, LocalDate fecha,
                                  Jugador... jugadores) throws PrimerPuntoException {
        for (Jugador jugador : jugadores) {
            participantes.inscribirParticipante(torneo, jugador, true, fecha);
        }
    }

    /** Carga, en orden, los marcadores indicados en los partidos pendientes ya definidos del torneo. */
    private static void jugarPendientes(GestionPartidosCanchas partidos, Torneo torneo, String[] marcadores)
            throws PrimerPuntoException {
        int i = 0;
        for (Partido partido : new ArrayList<>(torneo.getPartidos())) {
            if (!partido.estaFinalizado() && partido.estaDefinido() && i < marcadores.length) {
                partidos.registrarResultado(torneo, partido, marcadores[i++], null, false, false);
            }
        }
    }
}
