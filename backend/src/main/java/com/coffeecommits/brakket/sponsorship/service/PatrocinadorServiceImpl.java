package com.coffeecommits.brakket.sponsorship.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.sponsorship.dto.CambiarEstadoPatrocinadorRequest;
import com.coffeecommits.brakket.sponsorship.dto.CrearPatrocinadorRequest;
import com.coffeecommits.brakket.sponsorship.dto.EditarPatrocinadorRequest;
import com.coffeecommits.brakket.sponsorship.dto.PatrocinadorResponse;
import com.coffeecommits.brakket.sponsorship.model.Patrocinador;
import com.coffeecommits.brakket.sponsorship.model.PatrocinadorEnlace;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinadorEnlaceRepository;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatrocinadorServiceImpl implements PatrocinadorService {

    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final String ESTADO_INACTIVO = "INACTIVO";

    private final PatrocinadorRepository patrocinadorRepository;
    private final PatrocinadorEnlaceRepository enlaceRepository;

    public PatrocinadorServiceImpl(PatrocinadorRepository patrocinadorRepository,
                                   PatrocinadorEnlaceRepository enlaceRepository) {
        this.patrocinadorRepository = patrocinadorRepository;
        this.enlaceRepository = enlaceRepository;
    }

    @Override
    @Transactional
    public PatrocinadorResponse crear(CrearPatrocinadorRequest request) {
        patrocinadorRepository.findByNombreIgnoreCase(request.nombre()).ifPresent(existente -> {
            if (!request.confirmarDuplicado()) {
                throw new BusinessException(
                        "Ya existe un patrocinador con un nombre similar ('%s'). Confirma si deseas crearlo de todas formas."
                                .formatted(existente.getNombre()));
            }
        });

        Patrocinador patrocinador = Patrocinador.builder()
                .nombre(request.nombre())
                .logo(request.logo())
                .contacto(request.contacto())
                .descripcion(request.descripcion())
                .estado(ESTADO_ACTIVO)
                .build();
        patrocinador = patrocinadorRepository.save(patrocinador);

        List<String> enlaces = guardarEnlaces(patrocinador, request.enlaces());
        return PatrocinadorResponse.fromEntity(patrocinador, enlaces);
    }

    @Override
    @Transactional
    public PatrocinadorResponse editar(Long id, EditarPatrocinadorRequest request) {
        Patrocinador patrocinador = patrocinadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patrocinador", id));

        patrocinadorRepository.findByNombreIgnoreCase(request.nombre())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new BusinessException(
                            "Ya existe otro patrocinador con el nombre '%s'".formatted(request.nombre()));
                });

        patrocinador.setNombre(request.nombre());
        patrocinador.setLogo(request.logo());
        patrocinador.setContacto(request.contacto());
        patrocinador.setDescripcion(request.descripcion());
        patrocinadorRepository.save(patrocinador);

        enlaceRepository.findByPatrocinadorId(id).forEach(enlaceRepository::delete);
        List<String> enlaces = guardarEnlaces(patrocinador, request.enlaces());

        return PatrocinadorResponse.fromEntity(patrocinador, enlaces);
    }

    @Override
    @Transactional
    public PatrocinadorResponse cambiarEstado(Long id, CambiarEstadoPatrocinadorRequest request) {
        Patrocinador patrocinador = patrocinadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patrocinador", id));

        patrocinador.setEstado(Boolean.TRUE.equals(request.activo()) ? ESTADO_ACTIVO : ESTADO_INACTIVO);
        patrocinadorRepository.save(patrocinador);

        List<String> enlaces = obtenerEnlaces(id);
        return PatrocinadorResponse.fromEntity(patrocinador, enlaces);
    }

    @Override
    @Transactional(readOnly = true)
    public PatrocinadorResponse obtenerPorId(Long id) {
        Patrocinador patrocinador = patrocinadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patrocinador", id));
        return PatrocinadorResponse.fromEntity(patrocinador, obtenerEnlaces(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatrocinadorResponse> listar() {
        return patrocinadorRepository.findAll().stream()
                .map(p -> PatrocinadorResponse.fromEntity(p, obtenerEnlaces(p.getId())))
                .toList();
    }

    private List<String> guardarEnlaces(Patrocinador patrocinador, List<String> enlaces) {
        List<String> lista = enlaces == null ? List.of() : enlaces;
        return lista.stream()
                .map(url -> enlaceRepository.save(
                        PatrocinadorEnlace.builder().patrocinador(patrocinador).url(url).build()))
                .map(PatrocinadorEnlace::getUrl)
                .toList();
    }

    private List<String> obtenerEnlaces(Long patrocinadorId) {
        return enlaceRepository.findByPatrocinadorId(patrocinadorId).stream()
                .map(PatrocinadorEnlace::getUrl)
                .toList();
    }
}