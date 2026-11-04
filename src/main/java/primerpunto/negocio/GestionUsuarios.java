package primerpunto.negocio;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import primerpunto.datos.SinPersistencia;
import primerpunto.datos.UsuarioDAO;
import primerpunto.excepciones.CredencialesInvalidasException;
import primerpunto.excepciones.DatoInvalidoException;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.Usuario;

/**
 * Controlador del ingreso al sistema (RNF08): crea la cuenta del organizador la
 * primera vez que se usa el programa y verifica el usuario y la contraseña al ingresar.
 *
 * La contraseña se guarda como hash PBKDF2 con una sal aleatoria: aunque alguien lea
 * la tabla usuario, no puede saber cuál es la contraseña.
 */
public class GestionUsuarios {

    public static final int LARGO_MINIMO_CONTRASENA = 6;
    private static final int ITERACIONES = 120_000;
    private static final int LARGO_HASH_BITS = 256;

    private final UsuarioDAO dao;
    private final List<Usuario> usuarios = new ArrayList<>();
    private int proximoId = 1;

    /** Sin base de datos (lo usan las pruebas automatizadas). */
    public GestionUsuarios() {
        this(new UsuarioDAO(new SinPersistencia()));
    }

    public GestionUsuarios(UsuarioDAO dao) {
        this.dao = dao;
    }

    /** Recibe los usuarios leídos de la base al iniciar el programa. */
    void restaurar(List<Usuario> guardados, int ultimoIdGuardado) {
        usuarios.clear();
        usuarios.addAll(guardados);
        proximoId = ultimoIdGuardado + 1;
    }

    /** Si todavía no hay usuarios, la primera pantalla pide crear la cuenta del organizador. */
    public boolean hayUsuarios() {
        return !usuarios.isEmpty();
    }

    /** Crea la cuenta del organizador, con la contraseña escrita dos veces para evitar errores de tipeo. */
    public Usuario registrarUsuario(String nombreUsuario, String contrasena, String repeticion)
            throws PrimerPuntoException {
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            throw new DatoInvalidoException("Indicá el nombre de usuario.");
        }
        if (buscar(nombreUsuario) != null) {
            throw new DatoInvalidoException("Ya existe el usuario «" + nombreUsuario.trim() + "».");
        }
        if (contrasena == null || contrasena.length() < LARGO_MINIMO_CONTRASENA) {
            throw new DatoInvalidoException("La contraseña debe tener al menos " + LARGO_MINIMO_CONTRASENA
                    + " caracteres.");
        }
        if (!contrasena.equals(repeticion)) {
            throw new DatoInvalidoException("Las dos contraseñas no coinciden.");
        }
        String sal = nuevaSal();
        Usuario usuario = new Usuario(proximoId, nombreUsuario, calcularHash(contrasena, sal), sal);
        dao.insertar(usuario);
        usuarios.add(usuario);
        proximoId++;
        return usuario;
    }

    /** Verifica el usuario y la contraseña; si no coinciden, lanza CredencialesInvalidasException. */
    public Usuario iniciarSesion(String nombreUsuario, String contrasena) throws PrimerPuntoException {
        Usuario usuario = nombreUsuario == null ? null : buscar(nombreUsuario);
        // MessageDigest.isEqual compara en tiempo constante: no deja deducir cuántos caracteres coinciden
        if (usuario == null || contrasena == null || !MessageDigest.isEqual(
                calcularHash(contrasena, usuario.getSal()).getBytes(), usuario.getHashContrasena().getBytes())) {
            throw new CredencialesInvalidasException();
        }
        return usuario;
    }

    private Usuario buscar(String nombreUsuario) {
        for (Usuario usuario : usuarios) {
            if (usuario.getNombreUsuario().equalsIgnoreCase(nombreUsuario.trim())) {
                return usuario;
            }
        }
        return null;
    }

    private static String nuevaSal() {
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        return Base64.getEncoder().encodeToString(sal);
    }

    /** Hash PBKDF2 de la contraseña con la sal del usuario, codificado en texto (Base64) para guardarlo. */
    private static String calcularHash(String contrasena, String sal) throws PrimerPuntoException {
        try {
            PBEKeySpec especificacion = new PBEKeySpec(contrasena.toCharArray(), Base64.getDecoder().decode(sal),
                    ITERACIONES, LARGO_HASH_BITS);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(especificacion).getEncoded();
            especificacion.clearPassword();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // No debería pasar: el algoritmo viene incluido en todas las versiones de Java
            throw new PrimerPuntoException("No se pudo verificar la contraseña: " + e.getMessage(), e);
        }
    }
}
