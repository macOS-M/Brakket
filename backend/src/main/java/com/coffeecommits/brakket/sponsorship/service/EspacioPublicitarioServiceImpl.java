package com.coffeecommits.brakket.sponsorship.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.sponsorship.dto.CrearEspacioPublicitarioRequest;
import com.coffeecommits.brakket.sponsorship.dto.EditarEspacioPublicitarioRequest;
import com.coffeecommits.brakket.sponsorship.dto.EspacioPublicitarioResponse;
import com.coffeecommits.brakket.sponsorship.model.EspacioPublicitario;
import com.coffeecommits.brakket.sponsorship.model.Patrocinio;
import com.coffeecommits.brakket.sponsorship.repository.EspacioPublicitarioRepository;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class EspacioPublicitarioServiceImpl implements EspacioPublicitarioService {

    private static final String ESTADO_ACTIVO = "ACTIVO";

    // IMPORTANTE: esta lista es la fuente de la app, pero se duplica en el
    // CHECK ck_espacio_publicitario_ubicacion de la migracion V66. Si se agrega
    // o quita una ubicacion, hay que actualizar AMBOS lugares.
    private static final Set<String> UBICACIONES_VALIDAS = Set.of(
            "TRANSMISION_INFERIOR",
            "TORNEO_CABECERA",
            "LIGA_CABECERA"
    );

    private final EspacioPublicitarioRepository espacioRepository;
    private final PatrocinioRepository patrocinioRepository;

    public EspacioPublicitarioServiceImpl(EspacioPublicitarioRepository espacioRepository,
                                          PatrocinioRepository patrocinioRepository) {
        this.espacioRepository = espacioRepository;
        this.patrocinioRepository = patrocinioRepository;
    }

    @Override
    @Transactional
    public EspacioPublicitarioResponse crear(CrearEspacioPublicitarioRequest request) {

        Patrocinio patrocinio = patrocinioRepository.findById(request.patrocinioId())
                .orElseThrow(() -> new ResourceNotFoundException("Patrocinio", request.patrocinioId()));

        if (!ESTADO_ACTIVO.equals(patrocinio.getEstado())) {
            throw new BusinessException(
                    "El patrocinio debe estar activo para poder definir espacios publicitarios");
        }

        String ubicacion = request.ubicacion().toUpperCase();
        validarUbicacion(ubicacion, patrocinio);

        boolean ocupado = existeOcupadoParaPatrocinio(patrocinio, ubicacion, null);
        if (ocupado) {
            throw new BusinessException(
                    "El espacio publicitario ya está ocupado para el período seleccionado en esta competencia");
        }

        EspacioPublicitario espacio = EspacioPublicitario.builder()
                .patrocinio(patrocinio)
                .ubicacion(ubicacion)
                .imagenUrl(request.imagenUrl())
                .enlaceUrl(request.enlaceUrl())
                .estado(ESTADO_ACTIVO)
                .build();

        espacio = espacioRepository.save(espacio);
        return EspacioPublicitarioResponse.fromEntity(espacio);
    }

    @Override
    @Transactional
    public EspacioPublicitarioResponse editar(Long id, EditarEspacioPublicitarioRequest request) {
        EspacioPublicitario espacio = espacioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio publicitario", id));

        Patrocinio patrocinio = espacio.getPatrocinio();
        String ubicacion = request.ubicacion().toUpperCase();
        validarUbicacion(ubicacion, patrocinio);

        // Se excluye el propio id: si la ubicación no cambia, no debe chocar
        // contra sí mismo al validar ocupación.
        boolean ocupado = existeOcupadoParaPatrocinio(patrocinio, ubicacion, id);
        if (ocupado) {
            throw new BusinessException(
                    "El espacio publicitario ya está ocupado para el período seleccionado en esta competencia");
        }

        espacio.setUbicacion(ubicacion);
        espacio.setImagenUrl(request.imagenUrl());
        espacio.setEnlaceUrl(request.enlaceUrl());

        espacio = espacioRepository.save(espacio);
        return EspacioPublicitarioResponse.fromEntity(espacio);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        EspacioPublicitario espacio = espacioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio publicitario", id));
        espacioRepository.delete(espacio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EspacioPublicitarioResponse> listarPorPatrocinio(Long patrocinioId) {
        return espacioRepository.findByPatrocinioId(patrocinioId).stream()
                .map(EspacioPublicitarioResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EspacioPublicitarioResponse> buscarVigente(Long ligaId, Long temporadaId, Long torneoId,
                                                               String ubicacion) {
        String ubicacionNormalizada = ubicacion.toUpperCase();
        if (!UBICACIONES_VALIDAS.contains(ubicacionNormalizada)) {
            throw new BusinessException(
                    "La ubicación debe ser una de: %s".formatted(String.join(", ", UBICACIONES_VALIDAS)));
        }

        return espacioRepository.buscarVigente(ligaId, temporadaId, torneoId, ubicacionNormalizada, LocalDate.now())
                .map(EspacioPublicitarioResponse::fromEntity);
    }

    private void validarUbicacion(String ubicacion, Patrocinio patrocinio) {
        if (!UBICACIONES_VALIDAS.contains(ubicacion)) {
            throw new BusinessException(
                    "La ubicación debe ser una de: %s".formatted(String.join(", ", UBICACIONES_VALIDAS)));
        }
        Set<String> permitidas = ubicacionesPermitidasPara(patrocinio);
        if (!permitidas.contains(ubicacion)) {
            throw new BusinessException(
                    "La ubicación %s no corresponde al alcance de este patrocinio. Ubicaciones válidas para este alcance: %s"
                            .formatted(ubicacion, String.join(", ", permitidas)));
        }
    }

    private Set<String> ubicacionesPermitidasPara(Patrocinio patrocinio) {
        if (patrocinio.getLiga() != null) {
            return Set.of("LIGA_CABECERA");
        }
        if (patrocinio.getTorneo() != null) {
            return Set.of("TORNEO_CABECERA", "TRANSMISION_INFERIOR");
        }
        return UBICACIONES_VALIDAS;
    }

    private boolean existeOcupadoParaPatrocinio(Patrocinio patrocinio, String ubicacion, Long excluirEspacioId) {
        Long ligaId = patrocinio.getLiga() != null ? patrocinio.getLiga().getId() : null;
        Long temporadaId = patrocinio.getTemporada() != null ? patrocinio.getTemporada().getId() : null;
        Long torneoId = patrocinio.getTorneo() != null ? patrocinio.getTorneo().getId() : null;
        return espacioRepository.existeEspacioOcupado(
                ligaId, temporadaId, torneoId, ubicacion,
                patrocinio.getFechaInicio(), patrocinio.getFechaFin(), excluirEspacioId);
    }
}