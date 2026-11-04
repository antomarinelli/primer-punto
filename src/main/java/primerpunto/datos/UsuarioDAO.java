package primerpunto.datos;

import java.util.ArrayList;
import java.util.List;
import primerpunto.excepciones.PersistenciaException;
import primerpunto.modelo.Usuario;

/** Tabla usuario (RNF08). */
public class UsuarioDAO extends DAO {

    public UsuarioDAO(IAccesoDatos acceso) {
        super(acceso);
    }

    public List<Usuario> cargar() throws PersistenciaException {
        List<Usuario> usuarios = new ArrayList<>();
        for (Object[] fila : acceso.ejecutarConsulta(
                "SELECT id_usuario, nombre_usuario, hash_contrasena, sal FROM usuario ORDER BY id_usuario")) {
            usuarios.add(new Usuario(entero(fila[0]), texto(fila[1]), texto(fila[2]), texto(fila[3])));
        }
        return usuarios;
    }

    public void insertar(Usuario usuario) throws PersistenciaException {
        acceso.ejecutarActualizacion(
                "INSERT INTO usuario (id_usuario, nombre_usuario, hash_contrasena, sal) VALUES (?, ?, ?, ?)",
                usuario.getIdUsuario(), usuario.getNombreUsuario(), usuario.getHashContrasena(), usuario.getSal());
    }

    public int maximoId() throws PersistenciaException {
        return maximoId("usuario", "id_usuario");
    }
}
