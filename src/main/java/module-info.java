module primerpunto {
    requires javafx.controls;
    requires java.sql;
    requires mysql.connector.j;

    exports primerpunto;
    exports primerpunto.modelo;
    exports primerpunto.negocio;
    exports primerpunto.datos;
    exports primerpunto.excepciones;
    exports primerpunto.vista;
}
