package com.coffeecommits.brakket.progression.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.progression.model.Logro;
import com.coffeecommits.brakket.progression.model.LogroJugador;
import com.coffeecommits.brakket.progression.repository.LogroJugadorRepository;
import com.coffeecommits.brakket.progression.repository.LogroRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** Deriva logros únicamente de resultados oficiales ya persistidos. */
@Component
public class AchievementSynchronizer {

  private static final Logger log = LoggerFactory.getLogger(AchievementSynchronizer.class);

  static final String PRIMERA_VICTORIA = "Primera victoria";
  static final String COMPETIDOR_CONSTANTE = "Competidor constante";
  static final String CAMPEON = "Campeón";
  static final String EN_EQUIPO = "En equipo";
  static final String NUEVA_IDENTIDAD = "Nueva identidad";
  static final String PERFIL_CON_ROSTRO = "Perfil con rostro";
  static final String DEBUT_COMPETITIVO = "Debut competitivo";
  static final String FUNDADOR = "Fundador";
  static final String HABITUAL_TORNEOS = "Habitual de torneos";

  private static final String ORIGEN = "AUTO:RESULTADOS_OFICIALES";

  private final NamedParameterJdbcTemplate jdbc;
  private final LogroRepository logros;
  private final LogroJugadorRepository logrosJugador;

  public AchievementSynchronizer(
      NamedParameterJdbcTemplate jdbc,
      LogroRepository logros,
      LogroJugadorRepository logrosJugador) {
    this.jdbc = jdbc;
    this.logros = logros;
    this.logrosJugador = logrosJugador;
  }

  public void sincronizar(Usuario usuario) {
    Resumen resumen = resumenCompetitivo(usuario.getId());
    Onboarding onboarding = onboarding(usuario.getId());
    Set<String> alcanzados = calcularAlcanzados(usuario, resumen, onboarding);

    // El catálogo y los registros se cargan una sola vez. La sincronización
    // ocurre en cada GET de progresión, por lo que evitar consultas por logro
    // reduce sustancialmente el costo de la lectura.
    Map<String, Logro> logrosActivos =
        logros.findByActivoTrue().stream()
            .collect(Collectors.toMap(Logro::getNombre, Function.identity()));
    Map<Long, LogroJugador> registros =
        logrosJugador.findByUsuarioId(usuario.getId()).stream()
            .collect(
                Collectors.toMap(
                    registro -> registro.getLogro().getId(),
                    Function.identity(),
                    (primero, ignorado) -> primero,
                    HashMap::new));

    sincronizar(
        usuario,
        logrosActivos,
        registros,
        PRIMERA_VICTORIA,
        alcanzados.contains(PRIMERA_VICTORIA),
        "Primera victoria oficial");
    sincronizar(
        usuario,
        logrosActivos,
        registros,
        COMPETIDOR_CONSTANTE,
        alcanzados.contains(COMPETIDOR_CONSTANTE),
        resumen.partidas + " partidas oficiales");
    sincronizar(
        usuario,
        logrosActivos,
        registros,
        CAMPEON,
        alcanzados.contains(CAMPEON),
        "Campeonato oficial confirmado");
    sincronizar(
        usuario,
        logrosActivos,
        registros,
        EN_EQUIPO,
        alcanzados.contains(EN_EQUIPO),
        "Membresía de equipo registrada");
    sincronizar(
        usuario,
        logrosActivos,
        registros,
        NUEVA_IDENTIDAD,
        alcanzados.contains(NUEVA_IDENTIDAD),
        "Nombre visible actualizado");
    sincronizar(
        usuario,
        logrosActivos,
        registros,
        PERFIL_CON_ROSTRO,
        alcanzados.contains(PERFIL_CON_ROSTRO),
        "Foto de perfil configurada");
    sincronizar(
        usuario,
        logrosActivos,
        registros,
        DEBUT_COMPETITIVO,
        alcanzados.contains(DEBUT_COMPETITIVO),
        "Primera participación en torneo");
    sincronizar(
        usuario,
        logrosActivos,
        registros,
        FUNDADOR,
        alcanzados.contains(FUNDADOR),
        "Equipo creado como capitán");
    sincronizar(
        usuario,
        logrosActivos,
        registros,
        HABITUAL_TORNEOS,
        alcanzados.contains(HABITUAL_TORNEOS),
        onboarding.torneos + " torneos registrados");
  }

  private Resumen resumenCompetitivo(Long usuarioId) {
    Resumen resultado =
        jdbc.queryForObject(
            """
            select count(distinct p.id) partidas,
                   count(distinct p.id) filter (where p.ganador_id=m.equipo_id) victorias,
                   count(distinct t.id) filter (where t.campeon_equipo_id=m.equipo_id) campeonatos
            from miembro_equipo m
            join partida p on m.equipo_id in (p.equipo_a_id,p.equipo_b_id)
            join torneo t on t.id=p.torneo_id
            where m.usuario_id=:usuarioId
              and p.estado='FINALIZADA'
              and p.equipo_a_id is not null and p.equipo_b_id is not null
              and t.fecha_inicio::date >= m.fecha_union
              and (m.fecha_baja is null or t.fecha_inicio <= m.fecha_baja)
            """,
            Map.of("usuarioId", usuarioId),
            (rs, row) ->
                new Resumen(
                    rs.getLong("partidas"), rs.getLong("victorias"), rs.getLong("campeonatos")));
    return resultado == null ? new Resumen(0, 0, 0) : resultado;
  }

  private Onboarding onboarding(Long usuarioId) {
    Onboarding resultado =
        jdbc.queryForObject(
            """
            select
              (select count(*) from miembro_equipo m where m.usuario_id=:usuarioId) membresias,
              (select count(distinct i.torneo_id)
                 from inscripcion i join miembro_equipo m on m.equipo_id=i.equipo_id
                where m.usuario_id=:usuarioId and i.estado not in ('RECHAZADA','CANCELADA')
                  and i.fecha_solicitud >= m.fecha_union
                  and (m.fecha_baja is null or i.fecha_solicitud <= m.fecha_baja::date)) torneos,
              (select count(*) from equipo e where e.capitan_id=:usuarioId) equipos_creados
            """,
            Map.of("usuarioId", usuarioId),
            (rs, row) ->
                new Onboarding(
                    rs.getLong("membresias"),
                    rs.getLong("torneos"),
                    rs.getLong("equipos_creados")));
    return resultado == null ? new Onboarding(0, 0, 0) : resultado;
  }

  private Set<String> calcularAlcanzados(Usuario usuario, Resumen resumen, Onboarding onboarding) {
    Set<String> alcanzados = new HashSet<>();
    if (resumen.victorias >= 1) alcanzados.add(PRIMERA_VICTORIA);
    if (resumen.partidas >= 10) alcanzados.add(COMPETIDOR_CONSTANTE);
    if (resumen.campeonatos >= 1) alcanzados.add(CAMPEON);
    if (onboarding.membresias >= 1) alcanzados.add(EN_EQUIPO);
    if (Boolean.TRUE.equals(usuario.getNombreVisibleCambiado())) alcanzados.add(NUEVA_IDENTIDAD);
    if (usuario.getFotoUrl() != null && !usuario.getFotoUrl().isBlank())
      alcanzados.add(PERFIL_CON_ROSTRO);
    if (onboarding.torneos >= 1) alcanzados.add(DEBUT_COMPETITIVO);
    if (onboarding.equiposCreados >= 1) alcanzados.add(FUNDADOR);
    if (onboarding.torneos >= 5) alcanzados.add(HABITUAL_TORNEOS);
    return alcanzados;
  }

  private void sincronizar(
      Usuario usuario,
      Map<String, Logro> logrosActivos,
      Map<Long, LogroJugador> registros,
      String nombre,
      boolean alcanzado,
      String referencia) {
    Logro logro = logrosActivos.get(nombre);
    if (logro == null) {
      log.warn("No se sincronizó el logro '{}': no existe o está inactivo", nombre);
      return;
    }

    LogroJugador registro = registros.get(logro.getId());
    if (alcanzado) {
      if (registro == null) {
        LogroJugador nuevo =
            LogroJugador.builder()
                .usuario(usuario)
                .logro(logro)
                .fechaDesbloqueo(LocalDate.now())
                .activo(true)
                .referenciaSistema(ORIGEN + ":" + referencia)
                .build();
        logrosJugador.save(nuevo);
        registros.put(logro.getId(), nuevo);
      } else if (!Boolean.TRUE.equals(registro.getActivo())) {
        registro.setActivo(true);
        registro.setFechaDesbloqueo(LocalDate.now());
        registro.setFechaReversion(null);
        registro.setReferenciaSistema(ORIGEN + ":" + referencia);
      }
    } else if (esRegistroAutomaticoActivo(registro)) {
      registro.setActivo(false);
      registro.setFechaReversion(LocalDateTime.now());
    }
  }

  private boolean esRegistroAutomaticoActivo(LogroJugador registro) {
    return registro != null
        && Boolean.TRUE.equals(registro.getActivo())
        && registro.getReferenciaSistema() != null
        && registro.getReferenciaSistema().startsWith(ORIGEN);
  }

  private record Resumen(long partidas, long victorias, long campeonatos) {}

  private record Onboarding(long membresias, long torneos, long equiposCreados) {}
}
