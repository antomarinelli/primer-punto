package primerpunto;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.negocio.DatosIniciales;
import primerpunto.negocio.Sistema;
import primerpunto.vista.VentanaPrincipal;

/**
 * Punto de entrada de Primer Punto, sistema de gestión y organización de torneos
 * de tenis del Club Los Cuervos Tenis.
 *
 * Arquitectura en tres capas:
 * - Presentación (paquete vista): ventanas JavaFX.
 * - Negocio (paquete negocio): controladores GestionTorneos, GestionParticipantes,
 *   GestionCuadrosZonas y GestionPartidosCanchas.
 * - Modelo (paquete modelo): entidades del dominio. En este prototipo los datos se
 *   mantienen en memoria; la persistencia en MySQL se incorpora en la próxima etapa.
 */
public class App extends Application {

    private static final String RECURSOS = "/primerpunto/vista/";

    @Override
    public void start(Stage stage) {
        cargarFuentes();

        Sistema sistema = new Sistema();
        try {
            DatosIniciales.cargar(sistema);
        } catch (PrimerPuntoException e) {
            // Si los datos de ejemplo no se pueden cargar, el sistema igual arranca vacío
            new Alert(Alert.AlertType.WARNING, "No se pudieron cargar los datos de ejemplo: " + e.getMessage())
                    .showAndWait();
        }

        Scene scene = new Scene(new VentanaPrincipal(sistema), 1360, 860);
        scene.getStylesheets().add(getClass().getResource(RECURSOS + "estilos.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Primer Punto · Gestión de torneos de tenis");
        stage.getIcons().add(new Image(getClass().getResourceAsStream(RECURSOS + "logo.png")));
        stage.setMinWidth(1180);
        stage.setMinHeight(700);
        stage.show();
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
