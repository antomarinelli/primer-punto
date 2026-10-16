package primerpunto.modelo;

/**
 * Jugador del club. Compite solo en la modalidad Single, o forma parte de una
 * Pareja en las modalidades Dobles y Mixto.
 */
public class Jugador extends Participante {

    private final String nombre;
    private final String apellido;
    private final String dni;
    private String celular;
    private String email;

    public Jugador(int idParticipante, String nombre, String apellido, String dni, String celular, String email) {
        super(idParticipante, nombre + " " + apellido);
        if (apellido == null || apellido.isBlank()) {
            throw new IllegalArgumentException("El jugador debe tener un apellido.");
        }
        if (dni == null || !dni.matches("\\d{7,8}")) {
            throw new IllegalArgumentException("El DNI debe tener 7 u 8 dígitos numéricos.");
        }
        this.nombre = nombre.trim();
        this.apellido = apellido.trim();
        this.dni = dni;
        setContacto(celular, email);
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getDni() {
        return dni;
    }

    public String getCelular() {
        return celular;
    }

    public String getEmail() {
        return email;
    }

    /**
     * Los datos de contacto son los únicos del jugador que pueden cambiar con el tiempo.
     * Se exige al menos uno de los dos, y cada uno se valida según su formato.
     */
    public final void setContacto(String celular, String email) {
        String cel = celular == null ? "" : celular.trim();
        String mail = email == null ? "" : email.trim();
        if (cel.isEmpty() && mail.isEmpty()) {
            throw new IllegalArgumentException("Indicá al menos un medio de contacto: celular o email.");
        }
        if (!cel.isEmpty() && (!cel.matches("[0-9 +()-]+") || cel.replaceAll("\\D", "").length() < 8)) {
            throw new IllegalArgumentException("El celular debe tener al menos 8 números.");
        }
        if (!mail.isEmpty() && !mail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("El email «" + mail + "» no tiene un formato válido.");
        }
        this.celular = cel;
        this.email = mail;
    }

    @Override
    public String obtenerNombreCompleto() {
        return nombre + " " + apellido;
    }
}
