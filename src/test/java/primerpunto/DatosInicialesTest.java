package primerpunto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import primerpunto.negocio.DatosIniciales;
import primerpunto.negocio.Sistema;

/** Los datos de ejemplo del prototipo se cargan respetando todas las reglas del negocio. */
@DisplayName("Datos de ejemplo del prototipo")
class DatosInicialesTest {

    @Test
    @DisplayName("Se cargan sin errores: 12 jugadores, 3 canchas y 5 torneos")
    void cargaSinErrores() {
        Sistema sistema = new Sistema();
        assertDoesNotThrow(() -> DatosIniciales.cargar(sistema));
        assertEquals(12, sistema.getGestionParticipantes().listarJugadores().size());
        assertEquals(3, sistema.getGestionPartidosCanchas().listarCanchas().size());
        assertEquals(5, sistema.getGestionTorneos().listarTorneos(null).size());
    }
}
