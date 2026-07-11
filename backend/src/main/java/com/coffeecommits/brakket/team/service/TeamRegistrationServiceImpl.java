package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.team.dto.CrearEquipoRequest;
import com.coffeecommits.brakket.team.dto.EquipoResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.EquipoRedSocial;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRedSocialRepository;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TeamRegistrationServiceImpl implements TeamRegistrationService {

    private final EquipoRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;
    private final JuegoRepository juegoRepository;
    private final MiembroEquipoRepository miembroEquipoRepository;
    private final EquipoRedSocialRepository redSocialRepository;

    public TeamRegistrationServiceImpl(EquipoRepository equipoRepository,
                                       UsuarioRepository usuarioRepository,
                                       JuegoRepository juegoRepository,
                                       MiembroEquipoRepository miembroEquipoRepository,
                                       EquipoRedSocialRepository redSocialRepository) {
        this.equipoRepository = equipoRepository;
        this.usuarioRepository = usuarioRepository;
        this.juegoRepository = juegoRepository;
        this.miembroEquipoRepository = miembroEquipoRepository;
        this.redSocialRepository = redSocialRepository;
    }

    @Override
    @Transactional
    public EquipoResponse crear(CrearEquipoRequest request, String creadorCorreo) {

        Usuario creador = usuarioRepository.findByCorreo(creadorCorreo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", creadorCorreo));

        equipoRepository.findByNombre(request.nombre()).ifPresent(e -> {
            throw new BusinessException(
                    "Ya existe un equipo con el nombre '%s'".formatted(request.nombre()));
        });

        Juego juego = juegoRepository.findById(request.juegoId())
                .orElseThrow(() -> new ResourceNotFoundException("Juego", request.juegoId()));

        if (!Boolean.TRUE.equals(juego.getActivo())) {
            throw new BusinessException("El juego seleccionado no esta activo");
        }

        Equipo equipoNuevo = Equipo.builder()
                .nombre(request.nombre())
                .logo(request.logo())
                .descripcion(request.descripcion())
                .capitan(creador)
                .juego(juego)
                .build();

        final Equipo equipoGuardado;
        try {
            equipoGuardado = equipoRepository.save(equipoNuevo);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(
                    "Ya existe un equipo con el nombre '%s'".formatted(request.nombre()));
        }

        MiembroEquipo miembro = MiembroEquipo.builder()
                .equipo(equipoGuardado)
                .usuario(creador)
                .estado("ACTIVO")
                .fechaUnion(LocalDate.now())
                .rol("CAPITAN")
                .build();
        miembroEquipoRepository.save(miembro);

        List<String> redes = request.redesSociales() == null ? List.of() : request.redesSociales();
        List<String> redesGuardadas = redes.stream()
                .map(url -> redSocialRepository.save(
                        EquipoRedSocial.builder().equipo(equipoGuardado).url(url).build()))
                .map(EquipoRedSocial::getUrl)
                .toList();

        return EquipoResponse.fromEntity(equipoGuardado, redesGuardadas);
    }
}