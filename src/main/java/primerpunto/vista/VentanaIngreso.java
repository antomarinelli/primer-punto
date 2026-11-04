package primerpunto.vista;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.negocio.GestionUsuarios;

/**
 * Pantalla de ingreso (RNF08), antes del menú principal. La primera vez que se usa el
 * programa todavía no hay usuarios, así que pide crear la cuenta del organizador;
 * después pide el usuario y la contraseña.
 */
public class VentanaIngreso extends StackPane {

    private final GestionUsuarios gestion;
    private final Runnable alIngresar;
    private final boolean creandoCuenta;

    private final TextField txtUsuario = new TextField();
    private final PasswordField txtContrasena = new PasswordField();
    private final PasswordField txtRepeticion = new PasswordField();
    private final Label lblError = new Label();

    public VentanaIngreso(GestionUsuarios gestion, Runnable alIngresar) {
        this.gestion = gestion;
        this.alIngresar = alIngresar;
        this.creandoCuenta = !gestion.hayUsuarios();
        getStyleClass().add("contenido");
        getChildren().add(crearFormulario());
    }

    private VBox crearFormulario() {
        ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/primerpunto/vista/logo.png")));
        logo.setFitWidth(190);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);

        Label titulo = new Label(creandoCuenta ? "Crear cuenta del organizador" : "Ingresar");
        titulo.getStyleClass().add("tarjeta-titulo");
        for (TextField campo : new TextField[] {txtUsuario, txtContrasena, txtRepeticion}) {
            campo.getStyleClass().add("campo");
            campo.setOnAction(e -> confirmar());
        }
        lblError.getStyleClass().add("dato-aclaracion");
        lblError.setStyle("-fx-text-fill: -rojo;");
        lblError.setWrapText(true);
        Button boton = new Button(creandoCuenta ? "Crear cuenta" : "Ingresar");
        boton.getStyleClass().add("boton-primario");
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setOnAction(e -> confirmar());

        VBox tarjeta = new VBox(14, titulo, campo("Usuario", txtUsuario), campo("Contraseña", txtContrasena));
        if (creandoCuenta) {
            Label aclaracion = new Label("Es la primera vez que se usa el sistema. La contraseña debe tener al menos "
                    + GestionUsuarios.LARGO_MINIMO_CONTRASENA + " caracteres.");
            aclaracion.getStyleClass().add("dato-aclaracion");
            aclaracion.setWrapText(true);
            tarjeta.getChildren().addAll(campo("Repetir contraseña", txtRepeticion), aclaracion);
        }
        tarjeta.getChildren().addAll(lblError, boton);
        tarjeta.getStyleClass().add("tarjeta");
        tarjeta.setMaxWidth(400);

        VBox contenido = new VBox(30, logo, tarjeta);
        contenido.setAlignment(Pos.CENTER);
        contenido.setMaxWidth(400);
        return contenido;
    }

    private VBox campo(String etiqueta, TextField control) {
        Label label = new Label(etiqueta.toUpperCase());
        label.getStyleClass().add("etiqueta");
        return new VBox(7, label, control);
    }

    private void confirmar() {
        try {
            if (creandoCuenta) {
                gestion.registrarUsuario(txtUsuario.getText(), txtContrasena.getText(), txtRepeticion.getText());
            } else {
                gestion.iniciarSesion(txtUsuario.getText(), txtContrasena.getText());
            }
            alIngresar.run();
        } catch (PrimerPuntoException e) {
            lblError.setText(e.getMessage());
            txtContrasena.clear();
            txtRepeticion.clear();
            txtContrasena.requestFocus();
        }
    }
}
