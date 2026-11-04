package primerpunto.datos;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import primerpunto.excepciones.PersistenciaException;

/**
 * Datos de conexión a MySQL, leídos del archivo de texto config.properties que está
 * en la carpeta del proyecto. Así cada computadora usa su propio usuario y
 * contraseña sin modificar el código. Formato del archivo:
 *
 *   url=jdbc:mysql://localhost:3306/primer_punto
 *   usuario=root
 *   contrasena=...
 */
public class ConfiguracionBD {

    public static final String ARCHIVO = "config.properties";

    private final String url;
    private final String usuario;
    private final String contrasena;

    private ConfiguracionBD(String url, String usuario, String contrasena) {
        this.url = url;
        this.usuario = usuario;
        this.contrasena = contrasena;
    }

    /** Lee el archivo de configuración. Si falta el archivo o algún dato, informa cuál. */
    public static ConfiguracionBD leer(String rutaArchivo) throws PersistenciaException {
        Properties propiedades = new Properties();
        try (BufferedReader lector = new BufferedReader(new FileReader(rutaArchivo))) {
            propiedades.load(lector);
        } catch (FileNotFoundException e) {
            throw new PersistenciaException("No se encontró el archivo " + rutaArchivo + " con los datos de conexión. "
                    + "Copiá config.properties.ejemplo con ese nombre y completá tu usuario y contraseña de MySQL.", e);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo leer el archivo " + rutaArchivo + ": " + e.getMessage(), e);
        }
        String url = propiedades.getProperty("url", "").trim();
        String usuario = propiedades.getProperty("usuario", "").trim();
        if (url.isEmpty() || usuario.isEmpty()) {
            throw new PersistenciaException("Al archivo " + rutaArchivo + " le falta la url o el usuario de MySQL.");
        }
        return new ConfiguracionBD(url, usuario, propiedades.getProperty("contrasena", ""));
    }

    public String getUrl() {
        return url;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getContrasena() {
        return contrasena;
    }
}
