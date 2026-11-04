package primerpunto.modelo;

/**
 * Usuario que ingresa al sistema (el organizador del club, RNF08). La contraseña no
 * se guarda: se guarda su hash, calculado junto con una sal aleatoria propia de cada usuario.
 */
public class Usuario {

    private final int idUsuario;
    private final String nombreUsuario;
    private final String hashContrasena;
    private final String sal;

    public Usuario(int idUsuario, String nombreUsuario, String hashContrasena, String sal) {
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            throw new IllegalArgumentException("Indicá el nombre de usuario.");
        }
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario.trim();
        this.hashContrasena = hashContrasena;
        this.sal = sal;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getHashContrasena() {
        return hashContrasena;
    }

    public String getSal() {
        return sal;
    }
}
