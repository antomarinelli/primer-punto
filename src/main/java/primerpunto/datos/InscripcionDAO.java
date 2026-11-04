package primerpunto.datos;

import java.util.Map;
import primerpunto.excepciones.PersistenciaException;
import primerpunto.modelo.EstadoInscripcion;
import primerpunto.modelo.Inscripcion;
import primerpunto.modelo.Participante;
import primerpunto.modelo.Torneo;

/** Tabla inscripcion (CU05, CU06, CU09). */
public class InscripcionDAO extends DAO {

    public InscripcionDAO(IAccesoDatos acceso) {
        super(acceso);
    }

    /** Carga las inscripciones y las agrega a su torneo. */
    public void cargar(Map<Integer, Torneo> torneosPorId, Map<Integer, Participante> participantesPorId)
            throws PersistenciaException {
        for (Object[] fila : acceso.ejecutarConsulta("SELECT id_inscripcion, id_torneo, id_participante, fecha, "
                + "estado_pago, estado_inscripcion, motivo_baja FROM inscripcion ORDER BY id_inscripcion")) {
            Inscripcion inscripcion = new Inscripcion(entero(fila[0]), participantesPorId.get(entero(fila[2])),
                    fecha(fila[3]), logico(fila[4]));
            inscripcion.setEstadoInscripcion(EstadoInscripcion.valueOf(texto(fila[5])));
            inscripcion.setMotivoBaja(texto(fila[6]));
            torneosPorId.get(entero(fila[1])).agregarInscripcion(inscripcion);
        }
    }

    public void insertar(Torneo torneo, Inscripcion inscripcion) throws PersistenciaException {
        acceso.ejecutarActualizacion("INSERT INTO inscripcion (id_inscripcion, id_torneo, id_participante, fecha, "
                        + "estado_pago, estado_inscripcion, motivo_baja) VALUES (?, ?, ?, ?, ?, ?, ?)",
                inscripcion.getIdInscripcion(), torneo.getIdTorneo(),
                inscripcion.getParticipante().getIdParticipante(), inscripcion.getFecha(), inscripcion.isEstadoPago(),
                inscripcion.getEstadoInscripcion().name(), inscripcion.getMotivoBaja());
    }

    /** Guarda el pago y la baja (lo único que cambia de una inscripción). */
    public void actualizar(Inscripcion inscripcion) throws PersistenciaException {
        acceso.ejecutarActualizacion("UPDATE inscripcion SET estado_pago = ?, estado_inscripcion = ?, motivo_baja = ? "
                        + "WHERE id_inscripcion = ?",
                inscripcion.isEstadoPago(), inscripcion.getEstadoInscripcion().name(), inscripcion.getMotivoBaja(),
                inscripcion.getIdInscripcion());
    }

    public int maximoId() throws PersistenciaException {
        return maximoId("inscripcion", "id_inscripcion");
    }
}
