package com.coffeecommits.brakket.admin.service;

import com.coffeecommits.brakket.admin.dto.LogAuditoriaResponse;
import com.coffeecommits.brakket.admin.dto.PanelGlobalResponse;
import com.coffeecommits.brakket.admin.dto.ResumenPlataformaResponse;
import com.coffeecommits.brakket.admin.repository.LogAuditoriaRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.dispute.repository.DisputaRepository;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.league.repository.LigaRepository;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.tournament.model.EstadoTorneo;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Panel global de administración de la plataforma (RF-49, EPIC-14).
 *
 * <p>Consolida en una sola vista los indicadores de estado de todos los módulos
 * (usuarios, equipos, catálogo, ligas, torneos, transmisiones, disputas) y la
 * actividad de auditoría reciente, para que un ADMIN supervise la plataforma de
 * un vistazo. Es de solo lectura: agrega datos que otros módulos producen.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminPanelService {

    /** Entradas de auditoría que se muestran en la actividad del panel. */
    private static final int ACTIVIDAD_PANEL = 8;

    /** Tope de entradas del listado de auditoría dedicado. */
    private static final int AUDITORIA_MAX = 100;

    private final UsuarioRepository usuarioRepository;
    private final EquipoRepository equipoRepository;
    private final JuegoRepository juegoRepository;
    private final LigaRepository ligaRepository;
    private final TorneoRepository torneoRepository;
    private final TransmisionTwitchRepository transmisionRepository;
    private final DisputaRepository disputaRepository;
    private final LogAuditoriaRepository logAuditoriaRepository;

    /** Resumen + actividad reciente para el tablero del panel global. */
    @Transactional(readOnly = true)
    public PanelGlobalResponse panel() {
        return new PanelGlobalResponse(resumen(), auditoriaReciente(ACTIVIDAD_PANEL));
    }

    /** Conteos globales de la plataforma. */
    @Transactional(readOnly = true)
    public ResumenPlataformaResponse resumen() {
        return new ResumenPlataformaResponse(
                usuarioRepository.count(),
                usuarioRepository.countByBloqueadoTrue(),
                equipoRepository.count(),
                juegoRepository.count(),
                juegoRepository.countByActivoTrue(),
                ligaRepository.count(),
                torneoRepository.count(),
                torneoRepository.countByEstado(EstadoTorneo.EN_CURSO),
                transmisionRepository.countByActivaTrue(),
                disputaRepository.count()
        );
    }

    /** Últimas entradas de auditoría (más recientes primero), acotadas por límite. */
    @Transactional(readOnly = true)
    public List<LogAuditoriaResponse> auditoriaReciente(int limite) {
        int tope = Math.min(Math.max(limite, 1), AUDITORIA_MAX);
        return logAuditoriaRepository
                .findAllByOrderByFechaDescIdDesc(PageRequest.of(0, tope))
                .stream()
                .map(LogAuditoriaResponse::from)
                .toList();
    }
}
