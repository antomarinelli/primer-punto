package primerpunto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import primerpunto.excepciones.CredencialesInvalidasException;
import primerpunto.excepciones.DatoInvalidoException;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.Usuario;
import primerpunto.negocio.GestionUsuarios;

/** Ingreso al sistema con usuario y contraseña (RNF08). */
@DisplayName("Ingreso al sistema")
class IngresoTest {

    private GestionUsuarios gestion;

    @BeforeEach
    void prepararDatos() throws PrimerPuntoException {
        gestion = new GestionUsuarios();
        assertFalse(gestion.hayUsuarios(), "La primera vez no hay usuarios");
        gestion.registrarUsuario("organizador", "cuervos2026", "cuervos2026");
    }

    @Test
    @DisplayName("La contraseña no se guarda: se guarda un hash distinto de ella")
    void noGuardaLaContrasena() throws PrimerPuntoException {
        Usuario usuario = gestion.iniciarSesion("organizador", "cuervos2026");
        assertNotEquals("cuervos2026", usuario.getHashContrasena());
        assertFalse(usuario.getHashContrasena().contains("cuervos2026"));
        assertTrue(gestion.hayUsuarios());
    }

    @Test
    @DisplayName("Contraseña o usuario incorrectos: el mismo mensaje, sin decir cuál falló")
    void credencialesIncorrectas() {
        CredencialesInvalidasException contrasenaMal = assertThrows(CredencialesInvalidasException.class,
                () -> gestion.iniciarSesion("organizador", "otraClave"));
        CredencialesInvalidasException usuarioMal = assertThrows(CredencialesInvalidasException.class,
                () -> gestion.iniciarSesion("otro", "cuervos2026"));
        assertEquals("Usuario o contraseña incorrectos.", contrasenaMal.getMessage());
        assertEquals(contrasenaMal.getMessage(), usuarioMal.getMessage());
    }

    @Test
    @DisplayName("Al crear la cuenta se exige un mínimo de caracteres y repetir la contraseña")
    void validacionesDeLaCuentaNueva() {
        GestionUsuarios nueva = new GestionUsuarios();
        assertEquals("La contraseña debe tener al menos 6 caracteres.", assertThrows(DatoInvalidoException.class,
                () -> nueva.registrarUsuario("organizador", "123", "123")).getMessage());
        assertEquals("Las dos contraseñas no coinciden.", assertThrows(DatoInvalidoException.class,
                () -> nueva.registrarUsuario("organizador", "cuervos2026", "cuervos2025")).getMessage());
    }
}
