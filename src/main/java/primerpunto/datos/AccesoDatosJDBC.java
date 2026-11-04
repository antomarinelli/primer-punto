package primerpunto.datos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import primerpunto.excepciones.PersistenciaException;

/**
 * Acceso a la base MySQL con JDBC. El programa lo usa una sola persona en la PC del
 * club, por eso se mantiene una única conexión abierta mientras el programa está
 * en uso. Cada SQLException se informa como PersistenciaException, con un mensaje
 * que el organizador pueda entender.
 */
public class AccesoDatosJDBC implements IAccesoDatos {

    private final ConfiguracionBD configuracion;
    private Connection conexion;

    public AccesoDatosJDBC(ConfiguracionBD configuracion) {
        this.configuracion = configuracion;
    }

    @Override
    public void conectar() throws PersistenciaException {
        try {
            conexion = DriverManager.getConnection(configuracion.getUrl(), configuracion.getUsuario(),
                    configuracion.getContrasena());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo conectar con la base de datos (" + configuracion.getUrl()
                    + "). Verificá que MySQL esté encendido y que el usuario y la contraseña de "
                    + ConfiguracionBD.ARCHIVO + " sean correctos.\n\nDetalle: " + e.getMessage(), e);
        }
    }

    @Override
    public void desconectar() {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                // El programa se está cerrando: no hay nada más que hacer con la conexión
                System.err.println("No se pudo cerrar la conexión: " + e.getMessage());
            }
        }
    }

    @Override
    public List<Object[]> ejecutarConsulta(String sql, Object... parametros) throws PersistenciaException {
        List<Object[]> filas = new ArrayList<>();
        try (PreparedStatement sentencia = prepararSentencia(sql, parametros);
             ResultSet resultado = sentencia.executeQuery()) {
            int columnas = resultado.getMetaData().getColumnCount();
            while (resultado.next()) {
                Object[] fila = new Object[columnas];
                for (int i = 0; i < columnas; i++) {
                    fila[i] = resultado.getObject(i + 1);
                }
                filas.add(fila);
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron leer los datos de la base: " + e.getMessage(), e);
        }
        return filas;
    }

    @Override
    public int ejecutarActualizacion(String sql, Object... parametros) throws PersistenciaException {
        try (PreparedStatement sentencia = prepararSentencia(sql, parametros)) {
            return sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron guardar los cambios en la base: " + e.getMessage(), e);
        }
    }

    @Override
    public void iniciarTransaccion() throws PersistenciaException {
        try {
            conexionAbierta().setAutoCommit(false);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo iniciar la operación en la base: " + e.getMessage(), e);
        }
    }

    @Override
    public void confirmarTransaccion() throws PersistenciaException {
        try {
            conexionAbierta().commit();
            conexion.setAutoCommit(true);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron confirmar los cambios en la base: " + e.getMessage(), e);
        }
    }

    @Override
    public void deshacerTransaccion() {
        try {
            conexionAbierta().rollback();
            conexion.setAutoCommit(true);
        } catch (SQLException | PersistenciaException e) {
            // Ya se está informando el error que motivó el rollback; este queda solo en la consola
            System.err.println("No se pudieron deshacer los cambios: " + e.getMessage());
        }
    }

    /** Arma la sentencia con sus parámetros: el primer valor reemplaza al primer "?", y así. */
    private PreparedStatement prepararSentencia(String sql, Object[] parametros) throws SQLException,
            PersistenciaException {
        PreparedStatement sentencia = conexionAbierta().prepareStatement(sql);
        for (int i = 0; i < parametros.length; i++) {
            sentencia.setObject(i + 1, parametros[i]);
        }
        return sentencia;
    }

    private Connection conexionAbierta() throws PersistenciaException {
        if (conexion == null) {
            throw new PersistenciaException("No hay conexión con la base de datos.");
        }
        return conexion;
    }
}
