package primerpunto.datos;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de IAccesoDatos que no guarda nada: las consultas devuelven listas
 * vacías y las actualizaciones no hacen nada. La usan las pruebas automatizadas,
 * que verifican las reglas del negocio sin depender de que haya un MySQL instalado.
 */
public class SinPersistencia implements IAccesoDatos {

    @Override
    public void conectar() {
    }

    @Override
    public void desconectar() {
    }

    @Override
    public List<Object[]> ejecutarConsulta(String sql, Object... parametros) {
        return new ArrayList<>();
    }

    @Override
    public int ejecutarActualizacion(String sql, Object... parametros) {
        return 0;
    }

    @Override
    public void iniciarTransaccion() {
    }

    @Override
    public void confirmarTransaccion() {
    }

    @Override
    public void deshacerTransaccion() {
    }
}
