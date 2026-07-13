package com.coffeecommits.brakket.league.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.league.dto.ActualizarLigaRequest;
import com.coffeecommits.brakket.league.dto.CrearLigaRequest;
import com.coffeecommits.brakket.league.dto.CrearTemporadaRequest;
import com.coffeecommits.brakket.league.dto.JuegoOpcionResponse;
import com.coffeecommits.brakket.league.dto.LigaResponse;
import com.coffeecommits.brakket.league.dto.TemporadaResponse;
import com.coffeecommits.brakket.league.model.Liga;
import com.coffeecommits.brakket.league.model.Temporada;
import com.coffeecommits.brakket.league.repository.LigaRepository;
import com.coffeecommits.brakket.league.repository.TemporadaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LigaServiceImpl implements LigaService {

    private final LigaRepository ligaRepository;
    private final TemporadaRepository temporadaRepository;
    private final JuegoRepository juegoRepository;
    private final UsuarioRepository usuarioRepository;

    public LigaServiceImpl(LigaRepository ligaRepository,
                           TemporadaRepository temporadaRepository,
                           JuegoRepository juegoRepository,
                           UsuarioRepository usuarioRepository) {
        this.ligaRepository = ligaRepository;
        this.temporadaRepository = temporadaRepository;
        this.juegoRepository = juegoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public LigaResponse crearLiga(String correoComisionado, CrearLigaRequest request) {
        Usuario comisionado = buscarUsuario(correoComisionado);
        Juego juego = buscarJuego(request.juegoId());

        String nombre = request.nombre().trim();
        if (ligaRepository.existsByComisionadoIdAndNombreIgnoreCase(comisionado.getId(), nombre)) {
            throw new BusinessException("Ya tienes una liga con el nombre '%s'".formatted(nombre));
        }

        Liga liga = ligaRepository.save(Liga.builder()
                .nombre(nombre)
                .juego(juego)
                .comisionado(comisionado)
                .build());
        return LigaResponse.from(liga);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LigaResponse> listarLigas() {
        return ligaRepository.findAllByOrderByIdDesc().stream()
                .map(LigaResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LigaResponse obtenerLiga(Long ligaId) {
        return LigaResponse.from(buscarLiga(ligaId));
    }

    @Override
    @Transactional
    public LigaResponse actualizarLiga(Long ligaId, String correoComisionado, ActualizarLigaRequest request) {
        Liga liga = buscarLiga(ligaId);
        Usuario comisionado = buscarUsuario(correoComisionado);
        asegurarEsComisionado(liga, comisionado);

        String nombre = request.nombre().trim();
        boolean cambioNombre = !liga.getNombre().equalsIgnoreCase(nombre);
        if (cambioNombre
                && ligaRepository.existsByComisionadoIdAndNombreIgnoreCase(comisionado.getId(), nombre)) {
            throw new BusinessException("Ya tienes una liga con el nombre '%s'".formatted(nombre));
        }

        liga.setNombre(nombre);
        liga.setJuego(buscarJuego(request.juegoId()));
        return LigaResponse.from(liga);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemporadaResponse> listarTemporadas(Long ligaId) {
        buscarLiga(ligaId); // valida que la liga exista (404 si no)
        return temporadaRepository.findByLigaId(ligaId).stream()
                .map(TemporadaResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public TemporadaResponse crearTemporada(Long ligaId, String correoComisionado, CrearTemporadaRequest request) {
        Liga liga = buscarLiga(ligaId);
        Usuario comisionado = buscarUsuario(correoComisionado);
        asegurarEsComisionado(liga, comisionado);

        if (!request.fechaFin().isAfter(request.fechaInicio())) {
            throw new BusinessException("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        Temporada temporada = temporadaRepository.save(Temporada.builder()
                .liga(liga)
                .nombre(request.nombre().trim())
                .fechaInicio(request.fechaInicio())
                .fechaFin(request.fechaFin())
                .build());
        return TemporadaResponse.from(temporada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JuegoOpcionResponse> listarJuegosDisponibles() {
        return juegoRepository.findAll().stream()
                .filter(juego -> Boolean.TRUE.equals(juego.getActivo()))
                .map(JuegoOpcionResponse::from)
                .toList();
    }

    // ---------- helpers ----------

    private Liga buscarLiga(Long ligaId) {
        return ligaRepository.findById(ligaId)
                .orElseThrow(() -> new ResourceNotFoundException("Liga", ligaId));
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));
    }

    private Juego buscarJuego(Long juegoId) {
        return juegoRepository.findById(juegoId)
                .orElseThrow(() -> new ResourceNotFoundException("Juego", juegoId));
    }

    private void asegurarEsComisionado(Liga liga, Usuario usuario) {
        if (!liga.getComisionado().getId().equals(usuario.getId())) {
            throw new BusinessException("Solo el comisionado puede configurar esta liga");
        }
    }
}
