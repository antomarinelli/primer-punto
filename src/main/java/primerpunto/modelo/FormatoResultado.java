package primerpunto.modelo;

/**
 * Forma en que se juegan y se cargan los resultados de los partidos de un torneo.
 *
 * Por defecto un set termina 6-0 a 6-4, 7-5 o 7-6. Los formatos que se juegan distinto
 * tienen su propia versión de esSetValido(): en "2 sets y super tie-break" el tercer
 * set es un tie-break a 10 puntos, y el pro set se juega a 8 games.
 */
public enum FormatoResultado {

    MEJOR_DE_TRES("Al mejor de 3 sets", 2, 3, "6-4 3-6 7-5"),

    DOS_SETS_Y_SUPER_TIEBREAK("2 sets y super tie-break", 2, 3, "6-4 3-6 10-8") {
        /** El tercer set es un super tie-break: a 10 puntos, con 2 de diferencia (10-8, 11-9, 12-10...). */
        @Override
        public boolean esSetValido(int numeroSet, int g1, int g2) {
            if (numeroSet < 3) {
                return super.esSetValido(numeroSet, g1, g2);
            }
            int mayor = Math.max(g1, g2);
            int menor = Math.min(g1, g2);
            return mayor == 10 ? menor <= 8 : mayor > 10 && mayor - menor == 2;
        }
    },

    UN_SET("Un set", 1, 1, "6-4"),

    PRO_SET("Pro set a 8 games", 1, 1, "8-6") {
        /** Un único set a 8 games (8-0 a 8-6, 9-7, o 9-8 con tie-break). */
        @Override
        public boolean esSetValido(int numeroSet, int g1, int g2) {
            int mayor = Math.max(g1, g2);
            int menor = Math.min(g1, g2);
            return (mayor == 8 && menor <= 6) || (mayor == 9 && (menor == 7 || menor == 8));
        }
    };

    private final String descripcion;
    private final int setsParaGanar;
    private final int maximoSets;
    private final String ejemplo;

    FormatoResultado(String descripcion, int setsParaGanar, int maximoSets, String ejemplo) {
        this.descripcion = descripcion;
        this.setsParaGanar = setsParaGanar;
        this.maximoSets = maximoSets;
        this.ejemplo = ejemplo;
    }

    public int getSetsParaGanar() {
        return setsParaGanar;
    }

    public int getMaximoSets() {
        return maximoSets;
    }

    /** Un marcador de ejemplo de este formato, para ayudar al organizador a cargarlo. */
    public String getEjemplo() {
        return ejemplo;
    }

    /** Regla general de un set de tenis: 6-0 a 6-4, 7-5 o 7-6. */
    public boolean esSetValido(int numeroSet, int g1, int g2) {
        int mayor = Math.max(g1, g2);
        int menor = Math.min(g1, g2);
        return (mayor == 6 && menor >= 0 && menor <= 4) || (mayor == 7 && (menor == 5 || menor == 6));
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
