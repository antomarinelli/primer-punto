package primerpunto.excepciones;

/**
 * No se pudo leer o guardar información en la base de datos: MySQL no está
 * encendido, el usuario o la contraseña son incorrectos, o falló una consulta.
 * Guarda como causa la SQLException original.
 */
public class PersistenciaException extends PrimerPuntoException {

    public PersistenciaException(String mensaje) {
        super(mensaje);
    }

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
