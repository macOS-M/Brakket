package com.coffeecommits.brakket.statistics.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.league.repository.LigaRepository;
import com.coffeecommits.brakket.league.repository.TemporadaRepository;
import com.coffeecommits.brakket.statistics.dto.CatalogoEstadisticasResponse;
import com.coffeecommits.brakket.statistics.dto.EstadisticasHistoricasResponse;
import com.coffeecommits.brakket.statistics.dto.OpcionEstadisticaResponse;
import com.coffeecommits.brakket.statistics.dto.PaginaOpcionesResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class EstadisticasHistoricasService {
    private final NamedParameterJdbcTemplate jdbc;
    private final UsuarioRepository usuarios;
    private final EquipoRepository equipos;
    private final JuegoRepository juegos;
    private final TemporadaRepository temporadas;
    private final LigaRepository ligas;

    public EstadisticasHistoricasService(NamedParameterJdbcTemplate jdbc, UsuarioRepository usuarios,
            EquipoRepository equipos, JuegoRepository juegos, TemporadaRepository temporadas,
            LigaRepository ligas) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
        this.equipos = equipos;
        this.juegos = juegos;
        this.temporadas = temporadas;
        this.ligas = ligas;
    }

    public CatalogoEstadisticasResponse catalogo() {
        return new CatalogoEstadisticasResponse(
                ligas.buscarOpcionesConResultadosOficiales(),
                temporadas.buscarOpcionesConResultadosOficiales());
    }

    public PaginaOpcionesResponse buscarSujetos(String tipo, String texto, Long juegoId,
                                                 int pagina, int tamano) {
        String tipoNormalizado = tipo == null ? "" : tipo.trim().toUpperCase();
        String criterio = texto == null ? "" : texto.trim();
        if (!tipoNormalizado.equals("JUGADOR") && !tipoNormalizado.equals("EQUIPO")) {
            throw new IllegalArgumentException("El tipo debe ser JUGADOR o EQUIPO.");
        }
        if (criterio.length() < 2) {
            return new PaginaOpcionesResponse(List.of(), 0, tamano, 0);
        }
        if (juegoId != null && !juegos.existsById(juegoId)) {
            throw new IllegalArgumentException("El juego seleccionado no existe.");
        }
        int paginaSegura = Math.max(0, pagina);
        int tamanoSeguro = Math.max(1, Math.min(tamano, 5));
        PageRequest pageable = PageRequest.of(paginaSegura, tamanoSeguro);
        Page<OpcionEstadisticaResponse> resultado = tipoNormalizado.equals("JUGADOR")
                ? usuarios.buscarOpcionesEstadisticas(criterio, juegoId, pageable)
                : equipos.buscarOpcionesEstadisticas(criterio, juegoId, pageable);
        return new PaginaOpcionesResponse(resultado.getContent(), resultado.getNumber(),
                resultado.getSize(), resultado.getTotalElements());
    }

    public EstadisticasHistoricasResponse consultar(Long jugadorId, Long equipoId, Long juegoId,
            Long temporadaId, Long ligaId, LocalDate desde, LocalDate hasta) {
        if ((jugadorId == null) == (equipoId == null)) {
            throw new IllegalArgumentException("Debe seleccionar exactamente un jugador o un equipo.");
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        validarFiltros(juegoId, temporadaId, ligaId);

        String tipo;
        long sujetoId;
        String nombre;
        if (jugadorId != null) {
            Usuario usuario = usuarios.findById(jugadorId)
                    .orElseThrow(() -> new IllegalArgumentException("El jugador consultado no existe."));
            tipo = "JUGADOR"; sujetoId = jugadorId; nombre = usuario.getNombre();
        } else {
            Equipo equipo = equipos.findById(equipoId)
                    .orElseThrow(() -> new IllegalArgumentException("El equipo consultado no existe."));
            tipo = "EQUIPO"; sujetoId = equipoId; nombre = equipo.getNombre();
        }

        QueryParts query = construirQuery(jugadorId, equipoId, juegoId, temporadaId, ligaId, desde, hasta);
        List<Fila> filas = jdbc.query(query.sql(), query.params(), (rs, n) -> new Fila(
                rs.getLong("juego_id"), rs.getString("juego_nombre"), rs.getLong("torneo_id"),
                rs.getString("torneo_nombre"), rs.getDate("fecha").toLocalDate(),
                (Long) rs.getObject("temporada_id"), rs.getString("temporada_nombre"),
                (Long) rs.getObject("liga_id"), rs.getString("liga_nombre"),
                rs.getLong("equipo_id"), rs.getString("equipo_nombre"),
                rs.getInt("partidas"), rs.getInt("victorias")));

        Map<Long, Acumulado> porJuego = new LinkedHashMap<>();
        for (Fila f : filas) {
            Acumulado a = porJuego.computeIfAbsent(f.juegoId(), id -> new Acumulado(id, f.juegoNombre()));
            int derrotas = f.partidas() - f.victorias();
            a.partidas += f.partidas(); a.victorias += f.victorias(); a.torneos++;
            a.timeline.add(new EstadisticasHistoricasResponse.Participacion(f.torneoId(), f.torneoNombre(),
                    f.fecha(), f.temporadaId(), f.temporadaNombre(), f.ligaId(), f.ligaNombre(),
                    f.equipoId(), f.equipoNombre(), f.partidas(), f.victorias(), derrotas,
                    porcentaje(f.victorias(), f.partidas())));
        }
        List<EstadisticasHistoricasResponse.ResumenJuego> resumenes = porJuego.values().stream()
                .map(a -> new EstadisticasHistoricasResponse.ResumenJuego(a.juegoId, a.juegoNombre,
                        a.partidas, a.victorias, a.partidas - a.victorias,
                        porcentaje(a.victorias, a.partidas), a.torneos, a.timeline)).toList();
        Integer noDefinitivos = jdbc.queryForObject(query.pendientesSql(), query.params(), Integer.class);
        return new EstadisticasHistoricasResponse(tipo, sujetoId, nombre,
                new EstadisticasHistoricasResponse.FiltrosAplicados(juegoId, temporadaId, ligaId, desde, hasta),
                resumenes, noDefinitivos == null ? 0 : noDefinitivos);
    }

    private void validarFiltros(Long juegoId, Long temporadaId, Long ligaId) {
        if (juegoId != null && !juegos.existsById(juegoId)) throw new IllegalArgumentException("El juego seleccionado no existe.");
        if (temporadaId != null && !temporadas.existsById(temporadaId)) throw new IllegalArgumentException("La temporada seleccionada no existe.");
        if (ligaId != null && !ligas.existsById(ligaId)) throw new IllegalArgumentException("La liga seleccionada no existe.");
        if (temporadaId != null && ligaId != null && temporadas.findById(temporadaId)
                .filter(t -> t.getLiga().getId().equals(ligaId)).isEmpty())
            throw new IllegalArgumentException("La temporada no pertenece a la liga seleccionada.");
    }

    private QueryParts construirQuery(Long jugadorId, Long equipoId, Long juegoId, Long temporadaId,
            Long ligaId, LocalDate desde, LocalDate hasta) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String membresia = "";
        String sujeto;
        if (jugadorId != null) {
            membresia = " join miembro_equipo m on m.equipo_id in (p.equipo_a_id,p.equipo_b_id)"
                    + " and m.usuario_id=:jugadorId and t.fecha_inicio::date >= m.fecha_union"
                    + " and (m.fecha_baja is null or t.fecha_inicio <= m.fecha_baja) ";
            sujeto = "m.equipo_id";
            p.addValue("jugadorId", jugadorId);
        } else {
            sujeto = ":equipoId";
            p.addValue("equipoId", equipoId);
        }
        String joins = " from partida p join torneo t on t.id=p.torneo_id join juego j on j.id=t.juego_id"
                + membresia + " join equipo e on e.id=" + sujeto
                + " left join temporada s on s.id=t.temporada_id left join liga l on l.id=s.liga_id ";
        List<String> filtros = new ArrayList<>();
        filtros.add("(" + sujeto + "=p.equipo_a_id or " + sujeto + "=p.equipo_b_id)");
        if (juegoId != null) { filtros.add("j.id=:juegoId"); p.addValue("juegoId", juegoId); }
        if (temporadaId != null) { filtros.add("s.id=:temporadaId"); p.addValue("temporadaId", temporadaId); }
        if (ligaId != null) { filtros.add("l.id=:ligaId"); p.addValue("ligaId", ligaId); }
        if (desde != null) { filtros.add("t.fecha_inicio::date>=:desde"); p.addValue("desde", Date.valueOf(desde)); }
        if (hasta != null) { filtros.add("t.fecha_inicio::date<=:hasta"); p.addValue("hasta", Date.valueOf(hasta)); }
        String comunes = " and " + String.join(" and ", filtros);
        String oficial = " where p.estado='FINALIZADA' and p.equipo_a_id is not null and p.equipo_b_id is not null" + comunes;
        String sql = "select j.id juego_id,j.nombre juego_nombre,t.id torneo_id,t.nombre torneo_nombre,"
                + "t.fecha_inicio::date fecha,s.id temporada_id,s.nombre temporada_nombre,l.id liga_id,l.nombre liga_nombre,"
                + "e.id equipo_id,e.nombre equipo_nombre,count(distinct p.id) partidas,"
                + "count(distinct p.id) filter(where p.ganador_id=" + sujeto + ") victorias" + joins + oficial
                + " group by j.id,j.nombre,t.id,t.nombre,t.fecha_inicio,s.id,s.nombre,l.id,l.nombre,e.id,e.nombre"
                + " order by j.nombre,t.fecha_inicio,t.id";
        String pendientes = "select count(distinct p.id)" + joins
                + " where p.estado in ('REPORTADA','EN_DISPUTA')" + comunes;
        return new QueryParts(sql, pendientes, p);
    }

    private BigDecimal porcentaje(int victorias, int partidas) {
        return partidas == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(victorias * 100.0 / partidas)
                .setScale(1, RoundingMode.HALF_UP);
    }

    private record QueryParts(String sql, String pendientesSql, MapSqlParameterSource params) {}
    private record Fila(long juegoId, String juegoNombre, long torneoId, String torneoNombre, LocalDate fecha,
                        Long temporadaId, String temporadaNombre, Long ligaId, String ligaNombre,
                        long equipoId, String equipoNombre, int partidas, int victorias) {}
    private static class Acumulado {
        final long juegoId; final String juegoNombre; int partidas; int victorias; int torneos;
        final List<EstadisticasHistoricasResponse.Participacion> timeline = new ArrayList<>();
        Acumulado(long juegoId, String juegoNombre) { this.juegoId = juegoId; this.juegoNombre = juegoNombre; }
    }
}
