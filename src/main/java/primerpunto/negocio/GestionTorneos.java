package primerpunto.negocio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import primerpunto.datos.SinPersistencia;
import primerpunto.datos.TorneoDAO;
import primerpunto.excepciones.DatoInvalidoException;
import primerpunto.excepciones.PersistenciaException;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.EstadoTorneo;
import primerpunto.modelo.FormatoResultado;
import primerpunto.modelo.Modalidad;
import primerpunto.modelo.Torneo;

/**
 * Controlador del módulo Torneos: crea torneos, los lista, ordena y busca, y
 * administra los cambios de estado (cierre de inscripción y finalización).
 *
 * Los torneos se mantienen en una lista mientras el programa está abierto, y cada
 * alta o cambio de estado se guarda en la base de datos a través de TorneoDAO.
 */
public class GestionTorneos {

    private final TorneoDAO dao;
    private final List<Torneo> torneos = new ArrayList<>();
    private int proximoId = 1;

    /** Sin base de datos (lo usan las pruebas automatizadas). */
    public GestionTorneos() {
        this(new TorneoDAO(new SinPersistencia()));
    }

    public GestionTorneos(TorneoDAO dao) {
        this.dao = dao;
    }

    /** Recibe los torneos leídos de la base al iniciar el programa. */
    void restaurar(List<Torneo> guardados, int ultimoIdGuardado) {
        torneos.clear();
        torneos.addAll(guardados);
        proximoId = ultimoIdGuardado + 1;
    }

    /** Crea un torneo con partidos de 1 h 30 min al mejor de 3 sets. */
    public Torneo crearTorneo(String nombre, Modalidad modalidad, double costo, LocalDate fechaInicio,
                              LocalDate fechaFin, LocalDate plazoInscripcion, String lugar)
            throws PrimerPuntoException {
        return crearTorneo(nombre, modalidad, costo, fechaInicio, fechaFin, plazoInscripcion, lugar, 90,
                FormatoResultado.MEJOR_DE_TRES);
    }

    /**
     * Crea un torneo (CU03), con la duración promedio de sus partidos y su formato de
     * resultado. Si algún dato no respeta las reglas del negocio, informa el motivo.
     */
    public Torneo crearTorneo(String nombre, Modalidad modalidad, double costo, LocalDate fechaInicio,
                              LocalDate fechaFin, LocalDate plazoInscripcion, String lugar,
                              int duracionPartidoMinutos, FormatoResultado formatoResultado)
            throws PrimerPuntoException {
        for (Torneo existente : torneos) {
            if (nombre != null && existente.getNombre().equalsIgnoreCase(nombre.trim())) {
                throw new DatoInvalidoException("Ya existe un torneo con el nombre «" + nombre.trim() + "».");
            }
        }
        Torneo torneo;
        try {
            torneo = new Torneo(proximoId, nombre, modalidad, costo, fechaInicio, fechaFin,
                    plazoInscripcion, lugar, duracionPartidoMinutos, formatoResultado);
        } catch (IllegalArgumentException e) {
            // El constructor de Torneo rechazó los datos: se informa como un error del negocio
            throw new DatoInvalidoException(e.getMessage(), e);
        }
        // Primero se guarda en la base: si falla, el torneo no se agrega a la lista
        dao.insertar(torneo);
        torneos.add(torneo);
        proximoId++;
        return torneo;
    }

    /** Busca un torneo por su identificador. */
    public Torneo buscarTorneo(int idTorneo) throws DatoInvalidoException {
        for (Torneo torneo : torneos) {
            if (torneo.getIdTorneo() == idTorneo) {
                return torneo;
            }
        }
        throw new DatoInvalidoException("No existe un torneo con el número " + idTorneo + ".");
    }

    /** Lista los torneos, filtrando por estado si se indica uno (CU04, RF28). */
    public List<Torneo> listarTorneos(EstadoTorneo filtroEstado) {
        try {
            actualizarEstados(LocalDate.now());
        } catch (PersistenciaException e) {
            // El cierre por plazo se recalcula cada vez que se inicia el programa: si ahora no se
            // pudo guardar, el listado se muestra igual con el estado correcto
            System.err.println("No se pudo guardar el cierre de inscripción: " + e.getMessage());
        }
        List<Torneo> resultado = new ArrayList<>();
        for (Torneo torneo : torneos) {
            if (filtroEstado == null || torneo.getEstado() == filtroEstado) {
                resultado.add(torneo);
            }
        }
        return resultado;
    }

    /** Devuelve la fecha límite de inscripción de un torneo (CU07, RF13). */
    public LocalDate consultarPlazoInscripcion(int idTorneo) throws DatoInvalidoException {
        return buscarTorneo(idTorneo).getPlazoInscripcion();
    }

    /** Torneos finalizados, para el historial (CU16, RF32). */
    public List<Torneo> consultarHistorialTorneos() {
        return listarTorneos(EstadoTorneo.FINALIZADO);
    }

    /**
     * Cierra automáticamente la inscripción de los torneos cuyo plazo venció (RF12).
     * No hace falta que el organizador lo haga a mano: se revisa cada vez que se consultan los torneos.
     */
    public void actualizarEstados(LocalDate hoy) throws PersistenciaException {
        for (Torneo torneo : torneos) {
            if (torneo.getEstado() == EstadoTorneo.INSCRIPCION_ABIERTA && torneo.verificarPlazoVencido(hoy)) {
                cambiarEstado(torneo, EstadoTorneo.INSCRIPCION_CERRADA);
            }
        }
    }

    /** Permite al organizador cerrar la inscripción antes del plazo (por ejemplo, si se completó el cupo). */
    public void cerrarInscripcion(Torneo torneo) throws PrimerPuntoException {
        if (torneo.getEstado() != EstadoTorneo.INSCRIPCION_ABIERTA) {
            throw new DatoInvalidoException("La inscripción de este torneo ya está cerrada.");
        }
        cambiarEstado(torneo, EstadoTorneo.INSCRIPCION_CERRADA);
    }

    /** Marca el inicio del torneo, una vez generado su formato. */
    public void iniciarTorneo(Torneo torneo) throws PersistenciaException {
        cambiarEstado(torneo, EstadoTorneo.EN_CURSO);
    }

    /** Cambia el estado del torneo y lo guarda; si no se puede guardar, el torneo vuelve a su estado anterior. */
    void cambiarEstado(Torneo torneo, EstadoTorneo nuevo) throws PersistenciaException {
        EstadoTorneo anterior = torneo.getEstado();
        torneo.setEstado(nuevo);
        try {
            dao.actualizarEstado(torneo);
        } catch (PersistenciaException e) {
            torneo.setEstado(anterior);
            throw e;
        }
    }

    /**
     * Finaliza un torneo una vez disputados todos sus partidos (CU15, RF29 y RF31).
     * Al pasar a "Finalizado", el torneo queda incluido en el historial.
     */
    public void finalizarTorneo(Torneo torneo, boolean todosLosPartidosJugados) throws PrimerPuntoException {
        if (torneo.getEstado() == EstadoTorneo.FINALIZADO) {
            throw new DatoInvalidoException("El torneo ya está finalizado.");
        }
        if (torneo.getEstado() != EstadoTorneo.EN_CURSO) {
            throw new DatoInvalidoException("Solo se puede finalizar un torneo en curso.");
        }
        if (!todosLosPartidosJugados) {
            throw new DatoInvalidoException(
                    "No se puede finalizar el torneo hasta que se hayan disputado todos los partidos.");
        }
        cambiarEstado(torneo, EstadoTorneo.FINALIZADO);
    }

    /**
     * Ordena los torneos por fecha de inicio, de la más próxima a la más lejana (RF34).
     * Algoritmo de ordenación por inserción: cada torneo se inserta en su lugar
     * dentro de la parte de la lista que ya está ordenada.
     */
    public List<Torneo> ordenarPorFecha(List<Torneo> lista) {
        List<Torneo> ordenada = new ArrayList<>(lista);
        for (int i = 1; i < ordenada.size(); i++) {
            Torneo actual = ordenada.get(i);
            int j = i - 1;
            while (j >= 0 && ordenada.get(j).getFechaInicio().isAfter(actual.getFechaInicio())) {
                ordenada.set(j + 1, ordenada.get(j));
                j--;
            }
            ordenada.set(j + 1, actual);
        }
        return ordenada;
    }

    /**
     * Busca los torneos que comienzan en una fecha determinada (RF35).
     * Algoritmo de búsqueda binaria sobre la lista ordenada por fecha: en cada paso
     * descarta la mitad de la lista en la que el torneo no puede estar.
     */
    public List<Torneo> buscarPorFecha(LocalDate fecha) {
        List<Torneo> ordenada = ordenarPorFecha(torneos);
        List<Torneo> encontrados = new ArrayList<>();
        int inicio = 0;
        int fin = ordenada.size() - 1;
        int posicion = -1;
        while (inicio <= fin && posicion == -1) {
            int medio = (inicio + fin) / 2;
            LocalDate fechaMedio = ordenada.get(medio).getFechaInicio();
            if (fechaMedio.equals(fecha)) {
                posicion = medio;
            } else if (fechaMedio.isBefore(fecha)) {
                inicio = medio + 1;
            } else {
                fin = medio - 1;
            }
        }
        if (posicion != -1) {
            // Puede haber más de un torneo en la misma fecha: se recorren los vecinos
            int desde = posicion;
            while (desde > 0 && ordenada.get(desde - 1).getFechaInicio().equals(fecha)) {
                desde--;
            }
            for (int i = desde; i < ordenada.size() && ordenada.get(i).getFechaInicio().equals(fecha); i++) {
                encontrados.add(ordenada.get(i));
            }
        }
        return encontrados;
    }
}
