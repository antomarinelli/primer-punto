package primerpunto.negocio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import primerpunto.excepciones.DatoInvalidoException;
import primerpunto.modelo.Cuadro;
import primerpunto.modelo.EstadoTorneo;
import primerpunto.modelo.FilaPosicion;
import primerpunto.modelo.Formato;
import primerpunto.modelo.Participante;
import primerpunto.modelo.Partido;
import primerpunto.modelo.Torneo;
import primerpunto.modelo.Zona;

/**
 * Controlador del módulo Cuadros y zonas: genera el formato de competencia de un
 * torneo a partir de sus participantes habilitados (CU10) y permite consultarlo.
 */
public class GestionCuadrosZonas {

    private final GestionTorneos gestionTorneos;
    private int proximoIdFormato = 1;

    public GestionCuadrosZonas(GestionTorneos gestionTorneos) {
        this.gestionTorneos = gestionTorneos;
    }

    /** Indica si la cantidad de participantes habilitados es impar (CU10, flujo alternativo 1). */
    public boolean tieneCantidadImpar(Torneo torneo) {
        return torneo.getParticipantesHabilitados().size() % 2 != 0;
    }

    /**
     * Genera un cuadro de eliminación directa (RF19).
     *
     * @param conBye   participantes elegidos por el organizador para pasar directo a la segunda
     *                 ronda; si la lista está vacía, los byes se definen por sorteo
     * @param sortear  si es true, el orden del resto de los participantes se sortea
     */
    public Cuadro generarCuadro(Torneo torneo, List<Participante> conBye, boolean sortear)
            throws DatoInvalidoException {
        validarQueSePuedaGenerar(torneo);
        List<Participante> participantes = torneo.getParticipantesHabilitados();
        if (participantes.size() < 2) {
            throw new DatoInvalidoException("No hay inscriptos suficientes para generar el formato.");
        }
        Cuadro cuadro = new Cuadro(proximoIdFormato++);
        if (conBye.size() > cuadro.calcularByes(participantes.size())) {
            throw new DatoInvalidoException("Se eligieron más participantes con bye que los byes del cuadro ("
                    + cuadro.calcularByes(participantes.size()) + ").");
        }
        // Primero van los elegidos para el bye; después, el resto (sorteado si corresponde)
        List<Participante> resto = new ArrayList<>(participantes);
        resto.removeAll(conBye);
        if (sortear) {
            Collections.shuffle(resto);
        }
        List<Participante> orden = new ArrayList<>(conBye);
        orden.addAll(resto);

        cuadro.armarCruces(orden);
        torneo.agregarFormato(cuadro);
        gestionTorneos.iniciarTorneo(torneo);
        return cuadro;
    }

    /**
     * Genera el cuadro con el orden que armó el organizador a mano: los primeros de la
     * lista reciben los byes (si hacen falta) y el resto se enfrenta de a pares, en orden.
     */
    public Cuadro generarCuadroManual(Torneo torneo, List<Participante> orden) throws DatoInvalidoException {
        validarQueSePuedaGenerar(torneo);
        validarMismosParticipantes(torneo, orden);
        if (orden.size() < 2) {
            throw new DatoInvalidoException("No hay inscriptos suficientes para generar el formato.");
        }
        Cuadro cuadro = new Cuadro(proximoIdFormato++);
        cuadro.armarCruces(orden);
        torneo.agregarFormato(cuadro);
        gestionTorneos.iniciarTorneo(torneo);
        return cuadro;
    }

    /** Genera las zonas con el reparto que hizo el organizador a mano (cada zona, con al menos tres participantes). */
    public List<Zona> generarZonasManual(Torneo torneo, List<List<Participante>> reparto) throws DatoInvalidoException {
        validarQueSePuedaGenerar(torneo);
        List<Participante> todos = new ArrayList<>();
        for (int i = 0; i < reparto.size(); i++) {
            if (reparto.get(i).size() < 3) {
                throw new DatoInvalidoException("La zona " + (i + 1) + " necesita al menos 3 participantes.");
            }
            todos.addAll(reparto.get(i));
        }
        validarMismosParticipantes(torneo, todos);
        return crearZonas(torneo, reparto);
    }

    /** Verifica que el armado manual incluya a todos los habilitados, una sola vez cada uno. */
    private void validarMismosParticipantes(Torneo torneo, List<Participante> elegidos) throws DatoInvalidoException {
        List<Participante> habilitados = torneo.getParticipantesHabilitados();
        if (elegidos.size() != habilitados.size() || !elegidos.containsAll(habilitados)
                || new java.util.HashSet<>(elegidos).size() != elegidos.size()) {
            throw new DatoInvalidoException("Cada participante habilitado tiene que estar una sola vez.");
        }
    }

    /**
     * Genera las zonas de todos contra todos (RF19). Los participantes se reparten
     * de a uno por zona, para que las zonas queden parejas.
     * Si la cantidad no es múltiplo de las zonas, algunas quedan con un participante más.
     */
    public List<Zona> generarZonas(Torneo torneo, int cantidadZonas, boolean sortear) throws DatoInvalidoException {
        validarQueSePuedaGenerar(torneo);
        List<Participante> participantes = new ArrayList<>(torneo.getParticipantesHabilitados());
        if (cantidadZonas < 1) {
            throw new DatoInvalidoException("Indicá al menos una zona.");
        }
        if (participantes.size() < cantidadZonas * 3) {
            throw new DatoInvalidoException("Se necesitan al menos 3 participantes por zona (hay "
                    + participantes.size() + " habilitados).");
        }
        if (sortear) {
            Collections.shuffle(participantes);
        }
        List<List<Participante>> reparto = new ArrayList<>();
        for (int i = 0; i < cantidadZonas; i++) {
            reparto.add(new ArrayList<>());
        }
        for (int i = 0; i < participantes.size(); i++) {
            reparto.get(i % cantidadZonas).add(participantes.get(i));
        }
        return crearZonas(torneo, reparto);
    }

    private List<Zona> crearZonas(Torneo torneo, List<List<Participante>> reparto) {
        List<Zona> zonas = new ArrayList<>();
        for (int i = 0; i < reparto.size(); i++) {
            Zona zona = new Zona(proximoIdFormato++, i + 1);
            zona.armarCruces(reparto.get(i));
            torneo.agregarFormato(zona);
            zonas.add(zona);
        }
        gestionTorneos.iniciarTorneo(torneo);
        return zonas;
    }

    /**
     * Para el formato "zonas y luego cuadro": cuando terminan todas las zonas, arma el
     * cuadro con el primero (y, si se indica, también el segundo) de cada zona.
     * Los primeros de zona reciben los byes si hacen falta, y cada primero se cruza
     * con un segundo de otra zona, para no repetir un partido ya jugado en la zona.
     */
    public Cuadro generarCuadroDesdeZonas(Torneo torneo, int clasificadosPorZona) throws DatoInvalidoException {
        List<Zona> zonas = obtenerZonas(torneo);
        if (zonas.isEmpty()) {
            throw new DatoInvalidoException("El torneo no tiene zonas generadas.");
        }
        if (obtenerCuadro(torneo) != null) {
            throw new DatoInvalidoException("El cuadro final ya fue generado.");
        }
        if (clasificadosPorZona < 1 || clasificadosPorZona > 2) {
            throw new DatoInvalidoException("Clasifican uno o dos participantes por zona.");
        }
        for (Zona zona : zonas) {
            if (!zona.estaCompleto()) {
                throw new DatoInvalidoException("Todavía hay partidos sin jugar en la " + zona.getDescripcion() + ".");
            }
        }
        List<Participante> primeros = new ArrayList<>();
        List<Participante> segundos = new ArrayList<>();
        List<Integer> zonaDeSegundo = new ArrayList<>();
        for (int z = 0; z < zonas.size(); z++) {
            List<FilaPosicion> tabla = zonas.get(z).calcularTablaPosiciones();
            primeros.add(tabla.get(0).getParticipante());
            if (clasificadosPorZona == 2) {
                segundos.add(tabla.get(1).getParticipante());
                zonaDeSegundo.add(z);
            }
        }
        int total = primeros.size() + segundos.size();
        if (total < 2) {
            throw new DatoInvalidoException("Se necesitan al menos dos clasificados para armar el cuadro.");
        }
        Cuadro cuadro = new Cuadro(proximoIdFormato++);
        int byes = cuadro.calcularByes(total);

        // 1. Los byes se asignan primero a los primeros de zona (y, si sobran, a los segundos)
        List<Participante> orden = new ArrayList<>();
        int conBye = Math.min(byes, primeros.size());
        orden.addAll(primeros.subList(0, conBye));
        for (int i = 0; i < byes - conBye; i++) {
            orden.add(segundos.remove(0));
            zonaDeSegundo.remove(0);
        }
        // 2. Cada primero restante se cruza con un segundo de otra zona
        for (int z = conBye; z < primeros.size(); z++) {
            orden.add(primeros.get(z));
            int elegido = -1;
            for (int s = 0; s < segundos.size() && elegido == -1; s++) {
                if (zonaDeSegundo.get(s) != z) {
                    elegido = s;
                }
            }
            if (elegido == -1 && !segundos.isEmpty()) {
                elegido = 0; // no queda otra opción
            }
            if (elegido != -1) {
                orden.add(segundos.remove(elegido));
                zonaDeSegundo.remove(elegido);
            }
        }
        // 3. Si quedaron segundos sin rival asignado, se cruzan entre sí
        orden.addAll(segundos);

        cuadro.armarCruces(orden);
        torneo.agregarFormato(cuadro);
        return cuadro;
    }

    /**
     * Descarta el formato generado para poder elegir otro. Solo se permite mientras
     * el torneo no haya comenzado, es decir, mientras no haya ningún resultado cargado.
     */
    public void eliminarFormato(Torneo torneo) throws DatoInvalidoException {
        if (torneo.getFormatos().isEmpty()) {
            throw new DatoInvalidoException("El torneo todavía no tiene un formato generado.");
        }
        for (Partido partido : torneo.getPartidos()) {
            if (partido.estaFinalizado()) {
                throw new DatoInvalidoException("El torneo ya comenzó: hay resultados cargados, por eso el formato "
                        + "no se puede modificar.");
            }
        }
        torneo.quitarFormatos();
        torneo.setEstado(EstadoTorneo.INSCRIPCION_CERRADA);
    }

    /** El cuadro del torneo, o null si todavía no se generó (CU12). */
    public Cuadro obtenerCuadro(Torneo torneo) {
        for (Formato formato : torneo.getFormatos()) {
            if (formato instanceof Cuadro cuadro) {
                return cuadro;
            }
        }
        return null;
    }

    public List<Zona> obtenerZonas(Torneo torneo) {
        List<Zona> zonas = new ArrayList<>();
        for (Formato formato : torneo.getFormatos()) {
            if (formato instanceof Zona zona) {
                zonas.add(zona);
            }
        }
        return zonas;
    }

    /** Tabla de posiciones de una zona (CU14, RF27). */
    public List<FilaPosicion> obtenerTablaPosiciones(Zona zona) {
        return zona.calcularTablaPosiciones();
    }

    /** Verifica si ya se jugaron todos los partidos del torneo (se usa para finalizarlo). */
    public boolean todosLosPartidosJugados(Torneo torneo) {
        if (torneo.getFormatos().isEmpty()) {
            return false;
        }
        for (Formato formato : torneo.getFormatos()) {
            if (!formato.estaCompleto()) {
                return false;
            }
        }
        return true;
    }

    /** El formato se genera una sola vez, con la inscripción ya cerrada (precondición de CU10). */
    private void validarQueSePuedaGenerar(Torneo torneo) throws DatoInvalidoException {
        if (!torneo.getFormatos().isEmpty()) {
            throw new DatoInvalidoException("El formato de este torneo ya fue generado.");
        }
        if (torneo.getEstado() != EstadoTorneo.INSCRIPCION_CERRADA) {
            throw new DatoInvalidoException(
                    "El formato se genera una vez finalizado el plazo de inscripción.");
        }
    }
}
