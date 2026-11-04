package primerpunto.datos;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import primerpunto.excepciones.PersistenciaException;

/**
 * Base de los DAO (Data Access Object): cada DAO traduce las entidades de un módulo
 * a filas de sus tablas y viceversa. Esta clase guarda el acceso a datos que usan
 * todos, y las conversiones de los valores que devuelve MySQL a tipos de Java.
 */
public abstract class DAO {

    /** Operación con la base que puede formar parte de una transacción. */
    @FunctionalInterface
    protected interface Operacion {
        void ejecutar() throws PersistenciaException;
    }

    protected final IAccesoDatos acceso;

    protected DAO(IAccesoDatos acceso) {
        this.acceso = acceso;
    }

    /**
     * Ejecuta varias operaciones como una sola: si alguna falla, se deshacen todas
     * y la base queda como estaba (por ejemplo, un jugador no queda guardado como
     * participante sin sus datos de jugador).
     */
    protected void enTransaccion(Operacion operacion) throws PersistenciaException {
        acceso.iniciarTransaccion();
        try {
            operacion.ejecutar();
            acceso.confirmarTransaccion();
        } catch (PersistenciaException e) {
            acceso.deshacerTransaccion();
            throw e;
        }
    }

    protected static int entero(Object valor) {
        return ((Number) valor).intValue();
    }

    protected static double decimal(Object valor) {
        return ((Number) valor).doubleValue();
    }

    /** MySQL guarda BOOLEAN como TINYINT(1): según el driver llega como Boolean o como número. */
    protected static boolean logico(Object valor) {
        if (valor instanceof Boolean b) {
            return b;
        }
        return ((Number) valor).intValue() != 0;
    }

    protected static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    protected static LocalDate fecha(Object valor) {
        if (valor == null) {
            return null;
        }
        return valor instanceof LocalDate f ? f : ((Date) valor).toLocalDate();
    }

    protected static LocalTime hora(Object valor) {
        if (valor == null) {
            return null;
        }
        return valor instanceof LocalTime h ? h : ((Time) valor).toLocalTime();
    }

    /** Mayor identificador guardado en una columna (0 si la tabla está vacía). */
    protected int maximoId(String tabla, String columna) throws PersistenciaException {
        int maximo = 0;
        for (Object[] fila : acceso.ejecutarConsulta("SELECT MAX(" + columna + ") FROM " + tabla)) {
            if (fila[0] != null) {
                maximo = entero(fila[0]);
            }
        }
        return maximo;
    }
}
