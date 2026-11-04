package primerpunto.datos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import primerpunto.excepciones.PersistenciaException;
import primerpunto.modelo.Cancha;
import primerpunto.modelo.Cuadro;
import primerpunto.modelo.EstadoPartido;
import primerpunto.modelo.Formato;
import primerpunto.modelo.Participante;
import primerpunto.modelo.Partido;
import primerpunto.modelo.Resultado;
import primerpunto.modelo.Torneo;
import primerpunto.modelo.Zona;

/**
 * Tablas formato, cuadro, zona, partido y resultado (CU10, CU11, CU13). Cuadro y Zona
 * heredan de Formato con una tabla por subclase, igual que Jugador y Pareja.
 */
public class FormatoDAO extends DAO {

    public FormatoDAO(IAccesoDatos acceso) {
        super(acceso);
    }

    // ------------------------------------------------------------ carga

    /**
     * Carga los formatos de cada torneo con sus partidos, su programación y sus
     * resultados. Los participantes y las canchas ya cargados se buscan por su identificador.
     */
    public void cargar(Map<Integer, Torneo> torneosPorId, Map<Integer, Participante> participantesPorId,
                       Map<Integer, Cancha> canchasPorId) throws PersistenciaException {
        // 1. Partidos de cada formato, con su resultado si ya se jugaron
        Map<Integer, Torneo> torneoDeFormato = new HashMap<>();
        for (Object[] fila : acceso.ejecutarConsulta("SELECT id_formato, id_torneo FROM formato")) {
            torneoDeFormato.put(entero(fila[0]), torneosPorId.get(entero(fila[1])));
        }
        Map<Integer, List<Partido>> partidosDeFormato = new HashMap<>();
        for (Object[] fila : acceso.ejecutarConsulta("SELECT p.id_partido, p.id_formato, p.id_participante1, "
                + "p.id_participante2, p.instancia, p.ronda, p.posicion, p.id_cancha, p.fecha, p.horario, "
                + "r.id_resultado, r.marcador, r.id_ganador, r.es_wo, r.es_retiro "
                + "FROM partido p LEFT JOIN resultado r ON r.id_partido = p.id_partido ORDER BY p.id_partido")) {
            int idFormato = entero(fila[1]);
            Partido partido = new Partido(entero(fila[0]), buscar(participantesPorId, fila[2]),
                    buscar(participantesPorId, fila[3]), texto(fila[4]), entero(fila[5]), entero(fila[6]));
            if (fila[7] != null) {
                partido.setHorarioYCancha(canchasPorId.get(entero(fila[7])), fecha(fila[8]), hora(fila[9]));
            }
            if (fila[10] != null) {
                partido.setResultado(new Resultado(entero(fila[10]), texto(fila[11]),
                        participantesPorId.get(entero(fila[12])), logico(fila[13]), logico(fila[14]),
                        torneoDeFormato.get(idFormato).getFormatoResultado()));
            }
            partidosDeFormato.computeIfAbsent(idFormato, id -> new ArrayList<>()).add(partido);
        }

        // 2. Cada formato se arma como Cuadro o como Zona, según en qué tabla esté
        for (Object[] fila : acceso.ejecutarConsulta("SELECT f.id_formato, c.tamanio, c.cantidad_byes, z.numero_zona "
                + "FROM formato f LEFT JOIN cuadro c ON c.id_formato = f.id_formato "
                + "LEFT JOIN zona z ON z.id_formato = f.id_formato ORDER BY f.id_formato")) {
            int idFormato = entero(fila[0]);
            List<Partido> partidos = partidosDeFormato.getOrDefault(idFormato, new ArrayList<>());
            Formato formato;
            if (fila[1] != null) {
                Cuadro cuadro = new Cuadro(idFormato);
                cuadro.restaurar(entero(fila[1]), entero(fila[2]), partidos);
                formato = cuadro;
            } else {
                Zona zona = new Zona(idFormato, entero(fila[3]));
                zona.restaurar(partidos);
                formato = zona;
            }
            torneoDeFormato.get(idFormato).agregarFormato(formato);
        }
    }

    // ------------------------------------------------------------ alta y baja de formatos

    /** Guarda los formatos recién generados con todos sus partidos, en una sola transacción. */
    public void insertar(Torneo torneo, List<? extends Formato> formatos) throws PersistenciaException {
        enTransaccion(() -> {
            for (Formato formato : formatos) {
                acceso.ejecutarActualizacion("INSERT INTO formato (id_formato, id_torneo) VALUES (?, ?)",
                        formato.getIdFormato(), torneo.getIdTorneo());
                if (formato instanceof Cuadro cuadro) {
                    acceso.ejecutarActualizacion("INSERT INTO cuadro (id_cuadro, id_formato, tipo_cuadro, tamanio, "
                                    + "cantidad_byes) VALUES (?, ?, ?, ?, ?)",
                            cuadro.getIdFormato(), cuadro.getIdFormato(), cuadro.getTipoCuadro(), cuadro.getTamanio(),
                            cuadro.getCantidadByes());
                } else if (formato instanceof Zona zona) {
                    acceso.ejecutarActualizacion("INSERT INTO zona (id_zona, id_formato, numero_zona) VALUES (?, ?, ?)",
                            zona.getIdFormato(), zona.getIdFormato(), zona.getNumeroZona());
                }
                for (Partido partido : formato.getPartidos()) {
                    acceso.ejecutarActualizacion("INSERT INTO partido (id_partido, id_formato, id_participante1, "
                                    + "id_participante2, instancia, ronda, posicion, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            partido.getIdPartido(), formato.getIdFormato(), idDe(partido.getParticipante1()),
                            idDe(partido.getParticipante2()), partido.getInstancia(), partido.getRonda(),
                            partido.getPosicion(), partido.getEstado().name());
                }
            }
        });
    }

    /** Borra los formatos de un torneo (solo se permite mientras no haya resultados cargados). */
    public void eliminar(List<Formato> formatos) throws PersistenciaException {
        enTransaccion(() -> {
            for (Formato formato : formatos) {
                int id = formato.getIdFormato();
                acceso.ejecutarActualizacion("DELETE FROM resultado WHERE id_partido IN "
                        + "(SELECT id_partido FROM partido WHERE id_formato = ?)", id);
                acceso.ejecutarActualizacion("DELETE FROM partido WHERE id_formato = ?", id);
                acceso.ejecutarActualizacion("DELETE FROM cuadro WHERE id_formato = ?", id);
                acceso.ejecutarActualizacion("DELETE FROM zona WHERE id_formato = ?", id);
                acceso.ejecutarActualizacion("DELETE FROM formato WHERE id_formato = ?", id);
            }
        });
    }

    // ------------------------------------------------------------ programación y resultados

    /** Guarda la cancha, la fecha y el horario de un partido (null en los tres para quitarle la programación). */
    public void actualizarProgramacion(Partido partido, Cancha cancha, LocalDate fecha, LocalTime horario)
            throws PersistenciaException {
        EstadoPartido estado = partido.estaFinalizado() ? EstadoPartido.FINALIZADO
                : cancha == null ? EstadoPartido.PENDIENTE : EstadoPartido.PROGRAMADO;
        acceso.ejecutarActualizacion("UPDATE partido SET id_cancha = ?, fecha = ?, horario = ?, estado = ? "
                + "WHERE id_partido = ?", cancha == null ? null : cancha.getIdCancha(), fecha, horario,
                estado.name(), partido.getIdPartido());
    }

    /** Guarda la programación de varios partidos juntos (programación automática). */
    public void actualizarProgramaciones(List<Partido> partidos) throws PersistenciaException {
        enTransaccion(() -> {
            for (Partido partido : partidos) {
                actualizarProgramacion(partido, partido.getCancha(), partido.getFecha(), partido.getHorario());
            }
        });
    }

    /**
     * Guarda el resultado de un partido (o lo reemplaza si se corrige) y, en un cuadro,
     * ubica al ganador en el partido siguiente.
     *
     * @param siguiente  partido al que avanza el ganador (null en una zona o en la final)
     * @param lado       1 o 2: lugar que ocupa el ganador en el partido siguiente
     */
    public void guardarResultado(Partido partido, Resultado resultado, Partido siguiente, int lado)
            throws PersistenciaException {
        enTransaccion(() -> {
            acceso.ejecutarActualizacion("DELETE FROM resultado WHERE id_partido = ?", partido.getIdPartido());
            acceso.ejecutarActualizacion("INSERT INTO resultado (id_resultado, id_partido, marcador, id_ganador, "
                            + "es_wo, es_retiro) VALUES (?, ?, ?, ?, ?, ?)",
                    resultado.getIdResultado(), partido.getIdPartido(), resultado.getMarcador(),
                    resultado.getGanador().getIdParticipante(), resultado.isEsWO(), resultado.isEsRetiro());
            acceso.ejecutarActualizacion("UPDATE partido SET estado = ? WHERE id_partido = ?",
                    EstadoPartido.FINALIZADO.name(), partido.getIdPartido());
            if (siguiente != null) {
                acceso.ejecutarActualizacion("UPDATE partido SET id_participante" + lado + " = ? WHERE id_partido = ?",
                        resultado.getGanador().getIdParticipante(), siguiente.getIdPartido());
            }
        });
    }

    public int maximoIdFormato() throws PersistenciaException {
        return maximoId("formato", "id_formato");
    }

    public int maximoIdPartido() throws PersistenciaException {
        return maximoId("partido", "id_partido");
    }

    public int maximoIdResultado() throws PersistenciaException {
        return maximoId("resultado", "id_resultado");
    }

    private static Integer idDe(Participante participante) {
        return participante == null ? null : participante.getIdParticipante();
    }

    private static Participante buscar(Map<Integer, Participante> participantesPorId, Object id) {
        return id == null ? null : participantesPorId.get(entero(id));
    }
}
