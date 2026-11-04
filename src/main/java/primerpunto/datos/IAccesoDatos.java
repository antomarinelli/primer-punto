package primerpunto.datos;

import java.util.List;
import primerpunto.excepciones.PersistenciaException;

/**
 * Operaciones de acceso a la base de datos que usan los DAO. Los DAO dependen de
 * esta interfaz y no de MySQL: AccesoDatosJDBC la implementa contra MySQL y
 * SinPersistencia la implementa sin base, para las pruebas automatizadas.
 *
 * Las consultas reciben los valores por separado (parámetros "?"), nunca pegados
 * en el texto del SQL.
 */
public interface IAccesoDatos {

    void conectar() throws PersistenciaException;

    void desconectar();

    /**
     * Ejecuta un SELECT. Cada fila del resultado es un arreglo con los valores de sus
     * columnas, en el mismo orden que en la consulta.
     */
    List<Object[]> ejecutarConsulta(String sql, Object... parametros) throws PersistenciaException;

    /** Ejecuta un INSERT, UPDATE o DELETE y devuelve la cantidad de filas afectadas. */
    int ejecutarActualizacion(String sql, Object... parametros) throws PersistenciaException;

    /** Agrupa las operaciones siguientes: se guardan todas juntas o ninguna. */
    void iniciarTransaccion() throws PersistenciaException;

    void confirmarTransaccion() throws PersistenciaException;

    void deshacerTransaccion();
}
