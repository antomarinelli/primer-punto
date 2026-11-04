package primerpunto.datos;

import java.util.ArrayList;
import java.util.List;
import primerpunto.excepciones.PersistenciaException;
import primerpunto.modelo.EstadoTorneo;
import primerpunto.modelo.FormatoResultado;
import primerpunto.modelo.Modalidad;
import primerpunto.modelo.Torneo;

/**
 * Tabla torneo (CU03, CU04, CU15). Los enum se guardan con su nombre
 * ("DOBLES", "EN_CURSO") y se recuperan con valueOf().
 */
public class TorneoDAO extends DAO {

    public TorneoDAO(IAccesoDatos acceso) {
        super(acceso);
    }

    public List<Torneo> cargar() throws PersistenciaException {
        List<Torneo> torneos = new ArrayList<>();
        List<Object[]> filas = acceso.ejecutarConsulta("SELECT id_torneo, nombre, modalidad, costo, fecha_inicio, "
                + "fecha_fin, plazo_inscripcion, lugar, duracion_partido, formato_resultado, estado "
                + "FROM torneo ORDER BY id_torneo");
        for (Object[] fila : filas) {
            Torneo torneo = new Torneo(entero(fila[0]), texto(fila[1]), Modalidad.valueOf(texto(fila[2])),
                    decimal(fila[3]), fecha(fila[4]), fecha(fila[5]), fecha(fila[6]), texto(fila[7]),
                    entero(fila[8]), FormatoResultado.valueOf(texto(fila[9])));
            torneo.setEstado(EstadoTorneo.valueOf(texto(fila[10])));
            torneos.add(torneo);
        }
        return torneos;
    }

    public void insertar(Torneo torneo) throws PersistenciaException {
        acceso.ejecutarActualizacion("INSERT INTO torneo (id_torneo, nombre, modalidad, costo, fecha_inicio, "
                        + "fecha_fin, plazo_inscripcion, estado, lugar, duracion_partido, formato_resultado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                torneo.getIdTorneo(), torneo.getNombre(), torneo.getModalidad().name(), torneo.getCosto(),
                torneo.getFechaInicio(), torneo.getFechaFin(), torneo.getPlazoInscripcion(),
                torneo.getEstado().name(), torneo.getLugar(), torneo.getDuracionPartidoMinutos(),
                torneo.getFormatoResultado().name());
    }

    public void actualizarEstado(Torneo torneo) throws PersistenciaException {
        acceso.ejecutarActualizacion("UPDATE torneo SET estado = ? WHERE id_torneo = ?",
                torneo.getEstado().name(), torneo.getIdTorneo());
    }

    public int maximoId() throws PersistenciaException {
        return maximoId("torneo", "id_torneo");
    }
}
