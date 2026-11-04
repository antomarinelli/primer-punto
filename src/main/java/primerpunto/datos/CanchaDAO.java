package primerpunto.datos;

import java.util.ArrayList;
import java.util.List;
import primerpunto.excepciones.PersistenciaException;
import primerpunto.modelo.Cancha;

/** Tabla cancha (CU02). */
public class CanchaDAO extends DAO {

    public CanchaDAO(IAccesoDatos acceso) {
        super(acceso);
    }

    public List<Cancha> cargar() throws PersistenciaException {
        List<Cancha> canchas = new ArrayList<>();
        List<Object[]> filas = acceso.ejecutarConsulta(
                "SELECT id_cancha, numero, disponibilidad, hora_apertura, hora_cierre FROM cancha ORDER BY numero");
        for (Object[] fila : filas) {
            Cancha cancha = new Cancha(entero(fila[0]), entero(fila[1]), hora(fila[3]), hora(fila[4]));
            cancha.setDisponibilidad(logico(fila[2]));
            canchas.add(cancha);
        }
        return canchas;
    }

    public void insertar(Cancha cancha) throws PersistenciaException {
        acceso.ejecutarActualizacion(
                "INSERT INTO cancha (id_cancha, numero, disponibilidad, hora_apertura, hora_cierre) VALUES (?, ?, ?, ?, ?)",
                cancha.getIdCancha(), cancha.getNumero(), cancha.getDisponibilidad(), cancha.getHoraApertura(),
                cancha.getHoraCierre());
    }

    public void actualizar(Cancha cancha) throws PersistenciaException {
        acceso.ejecutarActualizacion(
                "UPDATE cancha SET numero = ?, disponibilidad = ?, hora_apertura = ?, hora_cierre = ? WHERE id_cancha = ?",
                cancha.getNumero(), cancha.getDisponibilidad(), cancha.getHoraApertura(), cancha.getHoraCierre(),
                cancha.getIdCancha());
    }

    public int maximoId() throws PersistenciaException {
        return maximoId("cancha", "id_cancha");
    }
}
