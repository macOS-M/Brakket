package com.coffeecommits.brakket.progression.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.progression.model.Logro;
import com.coffeecommits.brakket.progression.model.LogroJugador;
import com.coffeecommits.brakket.progression.repository.LogroJugadorRepository;
import com.coffeecommits.brakket.progression.repository.LogroRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/** Deriva logros únicamente de resultados oficiales ya persistidos. */
@Component
public class AchievementSynchronizer {
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

    public AchievementSynchronizer(NamedParameterJdbcTemplate jdbc, LogroRepository logros,
                                   LogroJugadorRepository logrosJugador) {
        this.jdbc=jdbc; this.logros=logros; this.logrosJugador=logrosJugador;
    }

    public void sincronizar(Usuario usuario) {
        Resumen r=jdbc.queryForObject("""
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
            """, Map.of("usuarioId",usuario.getId()),
                (rs,n) -> new Resumen(rs.getLong("partidas"),rs.getLong("victorias"),rs.getLong("campeonatos")));
        if (r==null) r=new Resumen(0,0,0);
        Set<String> alcanzados=new java.util.HashSet<>();
        if (r.victorias>=1) alcanzados.add(PRIMERA_VICTORIA);
        if (r.partidas>=10) alcanzados.add(COMPETIDOR_CONSTANTE);
        if (r.campeonatos>=1) alcanzados.add(CAMPEON);
        Onboarding o=onboarding(usuario);
        if (o.membresias>=1) alcanzados.add(EN_EQUIPO);
        if (Boolean.TRUE.equals(usuario.getNombreVisibleCambiado())) alcanzados.add(NUEVA_IDENTIDAD);
        if (usuario.getFotoUrl()!=null && !usuario.getFotoUrl().isBlank()) alcanzados.add(PERFIL_CON_ROSTRO);
        if (o.torneos>=1) alcanzados.add(DEBUT_COMPETITIVO);
        if (o.equiposCreados>=1) alcanzados.add(FUNDADOR);
        if (o.torneos>=5) alcanzados.add(HABITUAL_TORNEOS);
        sincronizar(usuario,PRIMERA_VICTORIA,alcanzados.contains(PRIMERA_VICTORIA),"Primera victoria oficial");
        sincronizar(usuario,COMPETIDOR_CONSTANTE,alcanzados.contains(COMPETIDOR_CONSTANTE),r.partidas+" partidas oficiales");
        sincronizar(usuario,CAMPEON,alcanzados.contains(CAMPEON),"Campeonato oficial confirmado");
        sincronizar(usuario,EN_EQUIPO,alcanzados.contains(EN_EQUIPO),"Membresía de equipo registrada");
        sincronizar(usuario,NUEVA_IDENTIDAD,alcanzados.contains(NUEVA_IDENTIDAD),"Nombre visible actualizado");
        sincronizar(usuario,PERFIL_CON_ROSTRO,alcanzados.contains(PERFIL_CON_ROSTRO),"Foto de perfil configurada");
        sincronizar(usuario,DEBUT_COMPETITIVO,alcanzados.contains(DEBUT_COMPETITIVO),"Primera participación en torneo");
        sincronizar(usuario,FUNDADOR,alcanzados.contains(FUNDADOR),"Equipo creado como capitán");
        sincronizar(usuario,HABITUAL_TORNEOS,alcanzados.contains(HABITUAL_TORNEOS),o.torneos+" torneos registrados");
    }

    private Onboarding onboarding(Usuario usuario) {
        Onboarding resultado=jdbc.queryForObject("""
            select
              (select count(*) from miembro_equipo m where m.usuario_id=:usuarioId) membresias,
              (select count(distinct i.torneo_id)
                 from inscripcion i join miembro_equipo m on m.equipo_id=i.equipo_id
                where m.usuario_id=:usuarioId and i.estado not in ('RECHAZADA','CANCELADA')
                  and i.fecha_solicitud >= m.fecha_union
                  and (m.fecha_baja is null or i.fecha_solicitud <= m.fecha_baja::date)) torneos,
              (select count(*) from equipo e where e.capitan_id=:usuarioId) equipos_creados
            """, Map.of("usuarioId",usuario.getId()),
                (rs,n) -> new Onboarding(rs.getLong("membresias"),rs.getLong("torneos"),rs.getLong("equipos_creados")));
        return resultado==null ? new Onboarding(0,0,0) : resultado;
    }

    private void sincronizar(Usuario usuario,String nombre,boolean alcanzado,String referencia) {
        Logro logro=logros.findByNombreAndActivoTrue(nombre).orElse(null);
        if (logro==null) return;
        LogroJugador registro=logrosJugador.findByUsuarioId(usuario.getId()).stream()
                .filter(l -> l.getLogro().getId().equals(logro.getId())).findFirst().orElse(null);
        if (alcanzado) {
            if (registro==null) {
                logrosJugador.save(LogroJugador.builder().usuario(usuario).logro(logro)
                        .fechaDesbloqueo(LocalDate.now()).activo(true)
                        .referenciaSistema(ORIGEN+":"+referencia).build());
            } else if (!Boolean.TRUE.equals(registro.getActivo())) {
                registro.setActivo(true); registro.setFechaDesbloqueo(LocalDate.now());
                registro.setFechaReversion(null); registro.setReferenciaSistema(ORIGEN+":"+referencia);
            }
        } else if (registro!=null && Boolean.TRUE.equals(registro.getActivo())
                && registro.getReferenciaSistema()!=null && registro.getReferenciaSistema().startsWith(ORIGEN)) {
            registro.setActivo(false); registro.setFechaReversion(LocalDateTime.now());
        }
    }

    private record Resumen(long partidas,long victorias,long campeonatos) {}
    private record Onboarding(long membresias,long torneos,long equiposCreados) {}
}
