package primerpunto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import primerpunto.modelo.FormatoResultado;
import primerpunto.modelo.Jugador;
import primerpunto.modelo.Resultado;

/**
 * Validación del marcador según el formato de resultado de cada torneo: cada formato
 * redefine qué sets son válidos y cuántos hay que ganar.
 */
@DisplayName("Formatos de resultado: validación del marcador")
class FormatoResultadoTest {

    private final Jugador ganador = new Jugador(1, "Antonella", "Marinelli", "46512132", "", "antonella@email.com");

    @ParameterizedTest(name = "{0}: «{1}» es válido")
    @CsvSource({
        "MEJOR_DE_TRES, 6-4 3-6 7-5",
        "MEJOR_DE_TRES, 7-6 6-4",
        "DOS_SETS_Y_SUPER_TIEBREAK, 6-4 3-6 10-8",
        "DOS_SETS_Y_SUPER_TIEBREAK, 6-4 3-6 12-10",
        "UN_SET, 6-4",
        "PRO_SET, 8-6",
        "PRO_SET, 9-8"
    })
    void aceptaMarcadoresValidos(FormatoResultado formato, String marcador) {
        Resultado resultado = new Resultado(1, marcador, ganador, false, false, formato);
        assertEquals(marcador, resultado.getMarcador());
    }

    @ParameterizedTest(name = "{0}: «{1}» se rechaza")
    @CsvSource({
        "MEJOR_DE_TRES, 6-5 6-4",
        "MEJOR_DE_TRES, 6-4",
        "DOS_SETS_Y_SUPER_TIEBREAK, 6-4 3-6 7-5",
        "DOS_SETS_Y_SUPER_TIEBREAK, 6-4 3-6 10-9",
        "UN_SET, 6-4 6-3",
        "PRO_SET, 6-4"
    })
    void rechazaMarcadoresInvalidos(FormatoResultado formato, String marcador) {
        assertThrows(IllegalArgumentException.class, () -> new Resultado(1, marcador, ganador, false, false, formato));
    }
}
