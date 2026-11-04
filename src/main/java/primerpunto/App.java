package primerpunto;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import primerpunto.datos.AccesoDatosJDBC;
import primerpunto.datos.ConfiguracionBD;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.negocio.DatosIniciales;
import primerpunto.negocio.Sistema;
import primerpunto.vista.VentanaIngreso;
import primerpunto.vista.VentanaPrincipal;

/**
 * Punto de entrada de Primer Punto, sistema de gestión y organización de torneos
 * de tenis del Club Los Cuervos Tenis.
 *
 * Organización según el patrón modelo-vista-controlador (MVC):
 * - Vista (paquete vista): ventanas JavaFX.
 * - Controlador (paquete negocio): GestionTorneos, GestionParticipantes,
 *   GestionCuadrosZonas y GestionPartidosCanchas.
 * - Modelo (paquetes modelo y datos): entidades del dominio y su persistencia en
 *   MySQL con JDBC.
 */
public class App extends Application {

    private static final String RECURSOS = "/primerpunto/vista/";

    private Sistema sistema;

    @Override
    public void start(Stage stage) {
        cargarFuentes();

        // Sin base de datos el programa no se abre: así nunca se trabaja creyendo que se guarda algo
        try {
            AccesoDatosJDBC acceso = new AccesoDatosJDBC(ConfiguracionBD.leer(ConfiguracionBD.ARCHIVO));
            acceso.conectar();
            sistema = new Sistema(acceso);
            sistema.cargarDatos();
            if (sistema.estaVacio()) {
                // Primera vez que se usa: se cargan datos de ejemplo, que quedan guardados en la base
                DatosIniciales.cargar(sistema);
            }
        } catch (PrimerPuntoException e) {
            Alert error = new Alert(Alert.AlertType.ERROR, e.getMessage());
            error.setHeaderText("No se pudo abrir Primer Punto");
            error.showAndWait();
            if (sistema != null) {
                sistema.cerrar();
            }
            Platform.exit();
            return;
        }

        // Primero se pide el usuario y la contraseña (RNF08); al ingresar se muestra el menú principal
        Scene scene = new Scene(new javafx.scene.layout.StackPane(), 1360, 860);
        scene.setRoot(new VentanaIngreso(sistema.getGestionUsuarios(),
                () -> scene.setRoot(new VentanaPrincipal(sistema))));
        scene.getStylesheets().add(getClass().getResource(RECURSOS + "estilos.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Primer Punto · Gestión de torneos de tenis");
        stage.getIcons().add(new Image(getClass().getResourceAsStream(RECURSOS + "logo.png")));
        stage.setMinWidth(1180);
        stage.setMinHeight(700);
        stage.show();
    }

    /** Al cerrar la ventana se cierra la conexión con la base. */
    @Override
    public void stop() {
        if (sistema != null) {
            sistema.cerrar();
        }
    }

    /** Carga la tipografía Poppins incluida en el proyecto, para que se vea igual en cualquier computadora. */
    private void cargarFuentes() {
        for (String peso : new String[] {"Regular", "Medium", "SemiBold", "Bold"}) {
            Font.loadFont(getClass().getResourceAsStream(RECURSOS + "fuentes/Poppins-" + peso + ".ttf"), 13);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
