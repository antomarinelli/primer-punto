package primerpunto.negocio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import primerpunto.excepciones.CanchaOcupadaException;
import primerpunto.excepciones.DatoInvalidoException;
import primerpunto.excepciones.DescansoInsuficienteException;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.Cancha;
import primerpunto.modelo.Cuadro;
import primerpunto.modelo.Formato;
import primerpunto.modelo.Jugador;
import primerpunto.modelo.Pareja;
import primerpunto.modelo.Participante;
import primerpunto.modelo.Partido;
import primerpunto.modelo.Resultado;
import primerpunto.modelo.Torneo;

/**
 * Controlador del módulo Partidos y canchas: registra las canchas del club,
 * programa los partidos sin superponer horarios y registra los resultados.
 *
 * Las canchas son las mismas para todos los torneos del club, por eso los
 * controles de superposición recorren los partidos de todos los torneos.
 */
public class GestionPartidosCanchas {

    /** Descanso mínimo de un jugador entre el fin de un partido y el inicio del siguiente. */
    public static final int DESCANSO_MINIMO_MINUTOS = 30;

    /** Cantidad máxima de partidos que puede jugar un jugador en un mismo día. */
    public static final int MAXIMO_PARTIDOS_POR_DIA = 2;

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM");

    private final GestionTorneos gestionTorneos;
    private final List<Cancha> canchas = new ArrayList<>();
    private int proximoIdCancha = 1;
    private int proximoIdResultado = 1;

    public GestionPartidosCanchas(GestionTorneos gestionTorneos) {
        this.gestionTorneos = gestionTorneos;
    }

    // ------------------------------------------------------------ canchas

    /** Registra una cancha del club con su horario de funcionamiento (CU02). */
    public Cancha registrarCancha(int numero, LocalTime horaApertura, LocalTime horaCierre)
            throws DatoInvalidoException {
        validarNumeroLibre(numero, null);
        try {
            Cancha cancha = new Cancha(proximoIdCancha, numero, horaApertura, horaCierre);
            canchas.add(cancha);
            proximoIdCancha++;
            return cancha;
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException(e.getMessage(), e);
        }
    }

    /** Modifica el número o el horario de una cancha existente (CU02, flujo alternativo 1). */
    public void modificarCancha(Cancha cancha, int numero, LocalTime horaApertura, LocalTime horaCierre)
            throws DatoInvalidoException {
        validarNumeroLibre(numero, cancha);
        try {
            cancha.setDatos(numero, horaApertura, horaCierre);
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException(e.getMessage(), e);
        }
    }

    public List<Cancha> listarCanchas() {
        List<Cancha> lista = new ArrayList<>(canchas);
        lista.sort(Comparator.comparingInt(Cancha::getNumero));
        return lista;
    }

    private void validarNumeroLibre(int numero, Cancha excepto) throws DatoInvalidoException {
        for (Cancha cancha : canchas) {
            if (cancha != excepto && cancha.getNumero() == numero) {
                throw new DatoInvalidoException("Ya existe la cancha número " + numero + ".");
            }
        }
    }

    // ------------------------------------------------------------ programación

    /**
     * Asigna (o cambia) la cancha, la fecha y el horario de un partido, exigiendo el descanso mínimo.
     */
    public void programarPartido(Torneo torneo, Partido partido, Cancha cancha, LocalDate fecha, LocalTime horario)
            throws PrimerPuntoException {
        programarPartido(torneo, partido, cancha, fecha, horario, false);
    }

    /**
     * Asigna (o cambia) la cancha, la fecha y el horario de un partido (CU11, flujo
     * alternativo 1). Verifica que la cancha esté abierta y libre (RF20), que en un
     * cuadro se respete el orden de las rondas, que ningún jugador tenga dos partidos
     * superpuestos ni más de dos en el día, y que tenga el descanso mínimo entre partidos.
     *
     * @param permitirSinDescanso  si es true, se programa aunque el descanso sea menor al
     *                             mínimo (el organizador lo confirmó, por ejemplo a pedido del jugador)
     * @throws DescansoInsuficienteException si falta descanso y no se permitió programar igual
     */
    public void programarPartido(Torneo torneo, Partido partido, Cancha cancha, LocalDate fecha, LocalTime horario,
                                 boolean permitirSinDescanso) throws PrimerPuntoException {
        if (cancha == null || fecha == null || horario == null) {
            throw new DatoInvalidoException("Seleccioná la cancha, la fecha y el horario del partido.");
        }
        if (partido.estaFinalizado()) {
            throw new DatoInvalidoException("El partido ya se jugó: no se puede reprogramar.");
        }
        if (fecha.isBefore(torneo.getFechaInicio()) || fecha.isAfter(torneo.getFechaFin())) {
            throw new DatoInvalidoException("La fecha debe estar dentro del período del torneo («" + torneo.getNombre()
                    + "» se juega del " + torneo.getFechaInicio().format(FECHA) + " al "
                    + torneo.getFechaFin().format(FECHA) + ").");
        }
        if (!cancha.getDisponibilidad()) {
            throw new DatoInvalidoException("La " + cancha + " está inhabilitada.");
        }
        if (!cancha.estaDisponible(horario, torneo.getDuracionPartidoMinutos())) {
            throw new DatoInvalidoException("El horario seleccionado está fuera del horario disponible de la cancha.");
        }
        Partido ocupado = buscarCanchaOcupada(partido, cancha, fecha, horario);
        if (ocupado != null) {
            throw new CanchaOcupadaException(cancha.getNumero(), ocupado.getFecha(), ocupado.getHorario());
        }
        String problema = validarOrdenDeRondas(torneo, partido, fecha.atTime(horario));
        if (problema == null) {
            problema = validarJugadores(partido, fecha, horario, false);
        }
        if (problema != null) {
            throw new DatoInvalidoException(problema);
        }
        if (!permitirSinDescanso) {
            String faltaDescanso = validarJugadores(partido, fecha, horario, true);
            if (faltaDescanso != null) {
                throw new DescansoInsuficienteException(faltaDescanso);
            }
        }
        partido.setHorarioYCancha(cancha, fecha, horario);
    }

    /** Libera la cancha y el horario de un partido que todavía no se jugó. */
    public void quitarProgramacion(Partido partido) throws DatoInvalidoException {
        if (partido.estaFinalizado()) {
            throw new DatoInvalidoException("El partido ya se jugó: no se puede quitar de la programación.");
        }
        partido.quitarProgramacion();
    }

    /** Programa automáticamente todos los partidos pendientes, desde hoy o desde el inicio del torneo. */
    public int programarPartidos(Torneo torneo) throws DatoInvalidoException {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = hoy.isAfter(torneo.getFechaInicio()) && !hoy.isAfter(torneo.getFechaFin())
                ? hoy : torneo.getFechaInicio();
        return programarPartidos(torneo, desde);
    }

    /**
     * Programa automáticamente todos los partidos del torneo que no tienen cancha (CU11, RF20),
     * incluidos los de rondas futuras del cuadro, que quedan reservados "a definir".
     * Para cada partido busca el primer turno libre recorriendo días, horarios y canchas,
     * siempre después de los partidos de la ronda anterior.
     *
     * @return la cantidad de partidos programados
     */
    public int programarPartidos(Torneo torneo, LocalDate desde) throws DatoInvalidoException {
        List<Cancha> habilitadas = new ArrayList<>();
        for (Cancha cancha : listarCanchas()) {
            if (cancha.getDisponibilidad()) {
                habilitadas.add(cancha);
            }
        }
        if (habilitadas.isEmpty()) {
            throw new DatoInvalidoException("No hay canchas habilitadas para programar los partidos.");
        }
        List<Partido> pendientes = new ArrayList<>();
        for (Partido partido : torneo.getPartidos()) {
            if (!partido.estaProgramado() && !partido.estaFinalizado()) {
                pendientes.add(partido);
            }
        }
        pendientes.sort(Comparator.comparingInt(Partido::getRonda)); // primero las rondas iniciales
        int programados = 0;
        for (Partido partido : pendientes) {
            if (!asignarPrimerTurnoLibre(torneo, partido, habilitadas, desde)) {
                throw new DatoInvalidoException("No hay canchas u horarios suficientes: se programaron "
                        + programados + " de " + pendientes.size() + " partidos.");
            }
            programados++;
        }
        return programados;
    }

    private boolean asignarPrimerTurnoLibre(Torneo torneo, Partido partido, List<Cancha> habilitadas, LocalDate desde) {
        LocalDateTime minimo = desde.atStartOfDay();
        LocalDateTime finPrevios = finDePartidosPrevios(torneo, partido);
        if (finPrevios != null && finPrevios.plusMinutes(DESCANSO_MINIMO_MINUTOS).isAfter(minimo)) {
            minimo = finPrevios.plusMinutes(DESCANSO_MINIMO_MINUTOS);
        }
        LocalTime primeraApertura = LocalTime.MAX;
        for (Cancha cancha : habilitadas) {
            if (cancha.getHoraApertura().isBefore(primeraApertura)) {
                primeraApertura = cancha.getHoraApertura();
            }
        }
        // Se prueban los horarios de media hora en media hora, para aprovechar el descanso de 30 minutos
        int turnosPorDia = (24 - primeraApertura.getHour()) * 2;
        for (LocalDate dia = torneo.getFechaInicio(); !dia.isAfter(torneo.getFechaFin()); dia = dia.plusDays(1)) {
            for (int t = 0; t < turnosPorDia; t++) {
                LocalTime hora = primeraApertura.plusMinutes(30L * t);
                if (dia.atTime(hora).isBefore(minimo)) {
                    continue;
                }
                for (Cancha cancha : habilitadas) {
                    if (cancha.estaDisponible(hora, torneo.getDuracionPartidoMinutos())
                            && buscarCanchaOcupada(partido, cancha, dia, hora) == null
                            && validarOrdenDeRondas(torneo, partido, dia.atTime(hora)) == null
                            && validarJugadores(partido, dia, hora, true) == null) {
                        partido.setHorarioYCancha(cancha, dia, hora);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * En un cuadro, un partido no puede empezar antes de que terminen los partidos de la
     * ronda anterior que lo definen, ni terminar después de que empiece el partido siguiente.
     *
     * @return el motivo por el que no se respeta el orden, o null si está todo bien
     */
    private String validarOrdenDeRondas(Torneo torneo, Partido partido, LocalDateTime inicio) {
        Cuadro cuadro = buscarCuadro(torneo, partido);
        if (cuadro == null) {
            return null;
        }
        for (Partido previo : cuadro.buscarPartidosPrevios(partido)) {
            if (previo.estaProgramado()) {
                LocalDateTime finPrevio = finDe(previo);
                if (inicio.isBefore(finPrevio)) {
                    return "Este partido depende del resultado de " + previo.getInstancia() + " (" + previo
                            + "): tiene que empezar después de que termine, el " + finPrevio.format(FECHA)
                            + " a las " + finPrevio.toLocalTime() + ".";
                }
            }
        }
        Partido siguiente = cuadro.buscarPartidoSiguiente(partido);
        if (siguiente != null && siguiente.estaProgramado()) {
            LocalDateTime inicioSiguiente = siguiente.getFecha().atTime(siguiente.getHorario());
            if (inicio.plusMinutes(torneo.getDuracionPartidoMinutos()).isAfter(inicioSiguiente)) {
                return "El partido siguiente (" + siguiente.getInstancia() + ") está programado el "
                        + inicioSiguiente.format(FECHA) + " a las " + siguiente.getHorario()
                        + ": este partido tiene que terminar antes.";
            }
        }
        return null;
    }

    /** Hora de fin del último partido previo programado (null si no tiene previos programados). */
    private LocalDateTime finDePartidosPrevios(Torneo torneo, Partido partido) {
        Cuadro cuadro = buscarCuadro(torneo, partido);
        LocalDateTime fin = null;
        if (cuadro != null) {
            for (Partido previo : cuadro.buscarPartidosPrevios(partido)) {
                if (previo.estaProgramado()) {
                    LocalDateTime finPrevio = finDe(previo);
                    if (fin == null || finPrevio.isAfter(fin)) {
                        fin = finPrevio;
                    }
                }
            }
        }
        return fin;
    }

    private Cuadro buscarCuadro(Torneo torneo, Partido partido) {
        for (Formato formato : torneo.getFormatos()) {
            if (formato instanceof Cuadro cuadro && cuadro.getPartidos().contains(partido)) {
                return cuadro;
            }
        }
        return null;
    }

    /** Busca, entre los partidos de todos los torneos, uno que ocupe la misma cancha en un horario superpuesto. */
    private Partido buscarCanchaOcupada(Partido partido, Cancha cancha, LocalDate fecha, LocalTime horario) {
        LocalDateTime inicio = fecha.atTime(horario);
        LocalDateTime fin = inicio.plusMinutes(duracionDe(partido));
        for (Partido otro : todosLosPartidos()) {
            if (otro == partido || !otro.estaProgramado() || otro.getCancha() != cancha) {
                continue;
            }
            LocalDateTime inicioOtro = otro.getFecha().atTime(otro.getHorario());
            LocalDateTime finOtro = finDe(otro);
            if (inicio.isBefore(finOtro) && inicioOtro.isBefore(fin)) {
                return otro;
            }
        }
        return null;
    }

    /**
     * Reglas de los jugadores, controladas entre todos los torneos del club:
     * - nadie puede tener dos partidos al mismo tiempo;
     * - nadie puede jugar más de dos partidos en el mismo día;
     * - si se exige descanso: entre el fin de un partido y el inicio del siguiente debe
     *   haber al menos 30 minutos.
     * En los partidos de rondas futuras se consideran todos los jugadores que podrían llegar a jugarlos.
     *
     * @return el motivo por el que no se cumplen las reglas, o null si se cumplen
     */
    private String validarJugadores(Partido partido, LocalDate fecha, LocalTime horario, boolean exigirDescanso) {
        Set<Jugador> propios = posiblesJugadores(partido);
        if (propios.isEmpty()) {
            return null;
        }
        LocalDateTime inicio = fecha.atTime(horario);
        LocalDateTime fin = inicio.plusMinutes(duracionDe(partido));
        Map<Jugador, Integer> partidosEnElDia = new HashMap<>();
        for (Partido otro : todosLosPartidos()) {
            if (otro == partido || !otro.estaProgramado()) {
                continue;
            }
            Set<Jugador> comunes = new HashSet<>(posiblesJugadores(otro));
            comunes.retainAll(propios);
            if (comunes.isEmpty()) {
                continue;
            }
            Jugador jugador = comunes.iterator().next();
            String quien = jugador.obtenerNombreCompleto() + (partido.estaDefinido() && otro.estaDefinido()
                    ? "" : " (que puede llegar a jugar este partido)");
            LocalDateTime inicioOtro = otro.getFecha().atTime(otro.getHorario());
            LocalDateTime finOtro = finDe(otro);
            String otroPartido = "el " + otro.getFecha().format(FECHA) + " de " + otro.getHorario() + " a "
                    + finOtro.toLocalTime() + " (" + otro.getInstancia() + ")";
            boolean seSuperponen = inicio.isBefore(finOtro) && inicioOtro.isBefore(fin);
            if (seSuperponen) {
                return quien + " ya tiene otro partido en ese horario: " + otroPartido + ".";
            }
            if (exigirDescanso) {
                boolean conDescanso = !fin.plusMinutes(DESCANSO_MINIMO_MINUTOS).isAfter(inicioOtro)
                        || !finOtro.plusMinutes(DESCANSO_MINIMO_MINUTOS).isAfter(inicio);
                if (!conDescanso) {
                    long minutos = Math.max(java.time.Duration.between(fin, inicioOtro).toMinutes(),
                            java.time.Duration.between(finOtro, inicio).toMinutes());
                    return quien + " tendría solo " + minutos + " minutos de descanso (el mínimo es "
                            + DESCANSO_MINIMO_MINUTOS + "): juega " + otroPartido + ".";
                }
            }
            if (otro.getFecha().equals(fecha)) {
                for (Jugador comun : comunes) {
                    int cantidad = partidosEnElDia.merge(comun, 1, Integer::sum);
                    if (cantidad >= MAXIMO_PARTIDOS_POR_DIA) {
                        return comun.obtenerNombreCompleto() + " ya tiene " + cantidad + " partidos el "
                                + fecha.format(FECHA) + ": el máximo es " + MAXIMO_PARTIDOS_POR_DIA
                                + " partidos por día.";
                    }
                }
            }
        }
        return null;
    }

    /**
     * Jugadores que juegan (o pueden llegar a jugar) un partido. Si un lugar del partido
     * todavía está "a definir", se toman los jugadores del partido anterior del cuadro
     * cuyo ganador ocupará ese lugar.
     */
    private Set<Jugador> posiblesJugadores(Partido partido) {
        Set<Jugador> jugadores = new HashSet<>();
        Cuadro cuadro = null;
        Participante[] lugares = {partido.getParticipante1(), partido.getParticipante2()};
        for (int lado = 0; lado < 2; lado++) {
            if (lugares[lado] != null) {
                jugadores.addAll(jugadoresDe(lugares[lado]));
            } else {
                if (cuadro == null) {
                    cuadro = buscarCuadro(partido);
                }
                Partido previo = cuadro == null ? null
                        : cuadro.buscarPartido(partido.getRonda() - 1, partido.getPosicion() * 2 + lado);
                if (previo != null) {
                    jugadores.addAll(posiblesJugadores(previo));
                }
            }
        }
        return jugadores;
    }

    /** Un jugador individual, o los dos integrantes de una pareja. */
    private List<Jugador> jugadoresDe(Participante participante) {
        if (participante instanceof Pareja pareja) {
            return List.of(pareja.getJugador1(), pareja.getJugador2());
        }
        return participante instanceof Jugador jugador ? List.of(jugador) : List.of();
    }

    private Cuadro buscarCuadro(Partido partido) {
        for (Torneo torneo : gestionTorneos.listarTorneos(null)) {
            Cuadro cuadro = buscarCuadro(torneo, partido);
            if (cuadro != null) {
                return cuadro;
            }
        }
        return null;
    }

    /** Duración de un partido en minutos: la duración promedio definida para su torneo. */
    public int duracionDe(Partido partido) {
        Torneo torneo = buscarTorneo(partido);
        return torneo == null ? 90 : torneo.getDuracionPartidoMinutos();
    }

    /** Fecha y hora en que termina un partido programado. */
    public LocalDateTime finDe(Partido partido) {
        return partido.getFecha().atTime(partido.getHorario()).plusMinutes(duracionDe(partido));
    }

    private List<Partido> todosLosPartidos() {
        List<Partido> todos = new ArrayList<>();
        for (Torneo torneo : gestionTorneos.listarTorneos(null)) {
            todos.addAll(torneo.getPartidos());
        }
        return todos;
    }

    /** Programación del torneo ordenada por fecha y horario (CU12, RF22). */
    public List<Partido> consultarProgramacion(Torneo torneo) {
        List<Partido> programados = new ArrayList<>();
        for (Partido partido : torneo.getPartidos()) {
            if (partido.estaProgramado()) {
                programados.add(partido);
            }
        }
        programados.sort(Comparator.comparing(Partido::getFecha).thenComparing(Partido::getHorario));
        return programados;
    }

    /** Partidos de todos los torneos programados en un día (para el calendario de canchas). */
    public List<Partido> consultarPartidosDelDia(LocalDate fecha) {
        List<Partido> delDia = new ArrayList<>();
        for (Partido partido : todosLosPartidos()) {
            if (partido.estaProgramado() && partido.getFecha().equals(fecha)) {
                delDia.add(partido);
            }
        }
        return delDia;
    }

    /** Torneo al que pertenece un partido. */
    public Torneo buscarTorneo(Partido partido) {
        for (Torneo torneo : gestionTorneos.listarTorneos(null)) {
            if (torneo.getPartidos().contains(partido)) {
                return torneo;
            }
        }
        return null;
    }

    /** Busca los partidos de un jugador o pareja por su nombre (CU12, RF21). */
    public List<Partido> buscarPartidosPorNombre(Torneo torneo, String texto) {
        List<Partido> encontrados = new ArrayList<>();
        String buscado = texto.trim().toLowerCase();
        for (Partido partido : torneo.getPartidos()) {
            if (partido.getNombreParticipante1().toLowerCase().contains(buscado)
                    || partido.getNombreParticipante2().toLowerCase().contains(buscado)) {
                encontrados.add(partido);
            }
        }
        return encontrados;
    }

    // ------------------------------------------------------------ resultados

    /**
     * Registra el resultado de un partido (CU13, RF23) y actualiza el formato: en un
     * cuadro el ganador avanza de ronda (RF25); en una zona se actualiza la tabla (RF26).
     *
     * @param ganador  obligatorio en un W.O. o un retiro; en un partido completo se
     *                 deduce del marcador y, si se indica, debe coincidir con él
     */
    public void registrarResultado(Torneo torneo, Partido partido, String marcador, Participante ganador,
                                   boolean esWO, boolean esRetiro) throws DatoInvalidoException {
        if (partido.estaFinalizado()) {
            throw new DatoInvalidoException("El partido ya tiene un resultado cargado. Usá «modificar resultado».");
        }
        guardarResultado(torneo, partido, marcador, ganador, esWO, esRetiro);
    }

    /** Corrige un resultado ya registrado (CU13, flujo alternativo 1). */
    public void modificarResultado(Torneo torneo, Partido partido, String marcador, Participante ganador,
                                   boolean esWO, boolean esRetiro) throws DatoInvalidoException {
        if (!partido.estaFinalizado()) {
            throw new DatoInvalidoException("El partido todavía no tiene un resultado para modificar.");
        }
        Formato formato = buscarFormato(torneo, partido);
        if (!formato.admiteCambioDeResultado(partido)) {
            throw new DatoInvalidoException("No se puede modificar: el partido siguiente del cuadro ya se jugó.");
        }
        guardarResultado(torneo, partido, marcador, ganador, esWO, esRetiro);
    }

    private void guardarResultado(Torneo torneo, Partido partido, String marcador, Participante ganador,
                                  boolean esWO, boolean esRetiro) throws DatoInvalidoException {
        if (!partido.estaDefinido()) {
            throw new DatoInvalidoException("Todavía no se conocen los dos participantes de este partido.");
        }
        if (!partido.estaProgramado()) {
            throw new DatoInvalidoException("El partido todavía no fue programado.");
        }
        if (esWO && esRetiro) {
            throw new DatoInvalidoException("Un partido no puede ser W.O. y retiro a la vez.");
        }
        if ((esWO || esRetiro) && (ganador == null || !partido.participa(ganador))) {
            throw new DatoInvalidoException("En un W.O. o un retiro hay que indicar cuál de los dos participantes ganó.");
        }
        Participante ganadorFinal = ganador;
        if (!esWO && !esRetiro) {
            // En un partido completo, el ganador surge del marcador
            int[] sets;
            try {
                sets = Resultado.contarSets(marcador, false, torneo.getFormatoResultado());
            } catch (IllegalArgumentException e) {
                throw new DatoInvalidoException("Marcador no válido para «" + torneo.getFormatoResultado() + "» (ej.: "
                        + torneo.getFormatoResultado().getEjemplo() + ").", e);
            }
            if (Math.max(sets[0], sets[1]) != torneo.getFormatoResultado().getSetsParaGanar()) {
                throw new DatoInvalidoException("Marcador incompleto para «" + torneo.getFormatoResultado() + "» (ej.: "
                        + torneo.getFormatoResultado().getEjemplo() + ").");
            }
            Participante segunMarcador = sets[0] > sets[1] ? partido.getParticipante1() : partido.getParticipante2();
            if (ganador != null && !ganador.equals(segunMarcador)) {
                throw new DatoInvalidoException("El ganador indicado no coincide con el marcador.");
            }
            ganadorFinal = segunMarcador;
        }
        try {
            Resultado resultado = new Resultado(proximoIdResultado, marcador, ganadorFinal, esWO, esRetiro,
                    torneo.getFormatoResultado());
            partido.setResultado(resultado);
            proximoIdResultado++;
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException(e.getMessage(), e);
        }
        buscarFormato(torneo, partido).procesarResultado(partido); // avanza el ganador o actualiza la tabla
    }

    private Formato buscarFormato(Torneo torneo, Partido partido) throws DatoInvalidoException {
        for (Formato formato : torneo.getFormatos()) {
            if (formato.getPartidos().contains(partido)) {
                return formato;
            }
        }
        throw new DatoInvalidoException("El partido no pertenece a este torneo.");
    }
}
