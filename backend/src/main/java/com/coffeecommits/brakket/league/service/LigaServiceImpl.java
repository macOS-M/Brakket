package com.coffeecommits.brakket.league.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
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
                .descripcion(normalizar(request.descripcion()))
                .reglas(normalizar(request.reglas()))
                .fotoUrl(normalizar(request.fotoUrl()))
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
        liga.setDescripcion(normalizar(request.descripcion()));
        liga.setReglas(normalizar(request.reglas()));
        liga.setFotoUrl(normalizar(request.fotoUrl()));
        return LigaResponse.from(liga);
    }

    @Override
    @Transactional
    public void eliminarLiga(Long ligaId, String correo, boolean esAdmin) {
        Liga liga = buscarLiga(ligaId);
        if (!esAdmin) {
            Usuario usuario = buscarUsuario(correo);
            if (!liga.getComisionado().getId().equals(usuario.getId())) {
                throw new ForbiddenException("Solo el comisionado o un administrador pueden eliminar esta liga");
            }
        }
        // Las temporadas no tienen borrado en cascada en el esquema; se
        // eliminan primero para no violar la clave foránea.
        temporadaRepository.deleteByLigaId(ligaId);
        ligaRepository.delete(liga);
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
        if (temporadaRepository.existsByLigaIdAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                ligaId, request.fechaFin(), request.fechaInicio())) {
            throw new BusinessException("Las fechas se solapan con otra temporada de esta liga");
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

    /** Campos opcionales: el texto en blanco se guarda como null. */
    private static String normalizar(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : valor.trim();
    }

    private Liga buscarLiga(Long ligaId) {
        return ligaRepository.findById(ligaId)
                .orElseThrow(() -> new ResourceNotFoundException("Liga", ligaId));
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));
    }

    private Juego buscarJuego(Long juegoId) {
        Juego juego = juegoRepository.findById(juegoId)
                .orElseThrow(() -> new ResourceNotFoundException("Juego", juegoId));
        if (!Boolean.TRUE.equals(juego.getActivo())) {
            throw new IllegalArgumentException(
                    "El juego '%s' no está disponible para ligas".formatted(juego.getNombre()));
        }
        return juego;
    }

    private void asegurarEsComisionado(Liga liga, Usuario usuario) {
        if (!liga.getComisionado().getId().equals(usuario.getId())) {
            throw new ForbiddenException("Solo el comisionado puede configurar esta liga");
        }
    }
}
