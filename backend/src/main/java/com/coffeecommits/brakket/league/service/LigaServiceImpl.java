package com.coffeecommits.brakket.league.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.game.model.FormatoCompetitivo;
import com.coffeecommits.brakket.game.repository.FormatoCompetitivoRepository;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.league.dto.ActualizarLigaRequest;
import com.coffeecommits.brakket.league.dto.ActualizarTemporadaRequest;
import com.coffeecommits.brakket.league.dto.FormatoOpcionResponse;
import com.coffeecommits.brakket.league.dto.CrearLigaRequest;
import com.coffeecommits.brakket.league.dto.CrearTemporadaRequest;
import com.coffeecommits.brakket.league.dto.JuegoOpcionResponse;
import com.coffeecommits.brakket.league.dto.LigaResponse;
import com.coffeecommits.brakket.league.dto.TemporadaResponse;
import com.coffeecommits.brakket.league.model.Liga;
import com.coffeecommits.brakket.league.model.Temporada;
import com.coffeecommits.brakket.league.repository.LigaRepository;
import com.coffeecommits.brakket.league.repository.TemporadaRepository;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class LigaServiceImpl implements LigaService {

    private final LigaRepository ligaRepository;
    private final TemporadaRepository temporadaRepository;
    private final JuegoRepository juegoRepository;
    private final UsuarioRepository usuarioRepository;
    private final FormatoCompetitivoRepository formatoRepository;
    private final TorneoRepository torneoRepository;

    public LigaServiceImpl(LigaRepository ligaRepository,
                           TemporadaRepository temporadaRepository,
                           JuegoRepository juegoRepository,
                           UsuarioRepository usuarioRepository,
                           FormatoCompetitivoRepository formatoRepository,
                           TorneoRepository torneoRepository) {
        this.ligaRepository = ligaRepository;
        this.temporadaRepository = temporadaRepository;
        this.juegoRepository = juegoRepository;
        this.usuarioRepository = usuarioRepository;
        this.formatoRepository = formatoRepository;
        this.torneoRepository = torneoRepository;
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
        return temporadaRepository.findByLigaIdOrderByFechaInicioAsc(ligaId).stream()
                .map(t -> TemporadaResponse.from(t, null))
                .toList();
    }

    @Override
    @Transactional
    public TemporadaResponse crearTemporada(Long ligaId, String correoComisionado, CrearTemporadaRequest request) {
        Liga liga = buscarLiga(ligaId);
        Usuario comisionado = buscarUsuario(correoComisionado);
        asegurarEsComisionado(liga, comisionado);
        asegurarLigaActiva(liga);
        validarConfiguracion(liga, request, null);
        if (temporadaRepository.existsByLigaIdAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                ligaId, request.fechaFin(), request.fechaInicio())) {
            throw new BusinessException("Las fechas se solapan con otra temporada de esta liga");
        }

        FormatoCompetitivo formato = buscarFormatoCompatible(liga, request.formatoId());

        Temporada temporada = temporadaRepository.save(Temporada.builder()
                .liga(liga)
                .nombre(request.nombre().trim())
                .fechaInicio(request.fechaInicio())
                .fechaFin(request.fechaFin())
                .reglas(request.reglas().trim())
                .estado(request.estado())
                .cupoEquipos(request.cupoEquipos())
                .formato(formato)
                .version(0L)
                .build());
        return TemporadaResponse.from(temporada, "Temporada creada correctamente");
    }

    @Override
    @Transactional
    public TemporadaResponse actualizarTemporada(Long ligaId, Long temporadaId, String correoComisionado,
                                                 ActualizarTemporadaRequest request) {
        Liga liga = buscarLiga(ligaId);
        Usuario comisionado = buscarUsuario(correoComisionado);
        asegurarEsComisionado(liga, comisionado);
        asegurarLigaActiva(liga);
        Temporada temporada = temporadaRepository.findById(temporadaId)
                .filter(t -> t.getLiga().getId().equals(ligaId))
                .orElseThrow(() -> new ResourceNotFoundException("Temporada", temporadaId));
        if (!temporada.getVersion().equals(request.version())) {
            throw new OptimisticLockException("La temporada fue modificada por otro usuario");
        }
        CrearTemporadaRequest config = request.configuracion();
        validarConfiguracion(liga, config, temporadaId);
        if (temporadaRepository.existsByLigaIdAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqualAndIdNot(
                ligaId, config.fechaFin(), config.fechaInicio(), temporadaId)) {
            throw new BusinessException("Las fechas se solapan con otra temporada de esta liga");
        }
        // Las temporadas creadas antes de RF-23 tienen formato nulo (la columna
        // es nullable). Sin este guard, editarlas lanza NullPointerException.
        Long formatoActualId = temporada.getFormato() == null ? null : temporada.getFormato().getId();
        boolean cambioCritico = !temporada.getFechaInicio().equals(config.fechaInicio())
                || !temporada.getFechaFin().equals(config.fechaFin())
                || !temporada.getCupoEquipos().equals(config.cupoEquipos())
                || !Objects.equals(formatoActualId, config.formatoId());
        if (cambioCritico && torneoRepository.existsActivoByTemporadaId(temporadaId)) {
            throw new BusinessException("No se pueden cambiar fechas, cupo o formato mientras existan torneos activos");
        }
        temporada.setNombre(config.nombre().trim());
        temporada.setFechaInicio(config.fechaInicio());
        temporada.setFechaFin(config.fechaFin());
        temporada.setReglas(config.reglas().trim());
        temporada.setEstado(config.estado());
        temporada.setCupoEquipos(config.cupoEquipos());
        temporada.setFormato(buscarFormatoCompatible(liga, config.formatoId()));
        // El flush fuerza el incremento de @Version antes de construir la respuesta.
        // Sin esto el cliente recibiria la version anterior y el siguiente cambio
        // produciria un falso conflicto de concurrencia.
        Temporada actualizada = temporadaRepository.saveAndFlush(temporada);
        return TemporadaResponse.from(actualizada, "Temporada actualizada correctamente");
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormatoOpcionResponse> listarFormatosDisponibles(Long ligaId) {
        Liga liga = buscarLiga(ligaId);
        asegurarLigaActiva(liga);
        return formatoRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(FormatoOpcionResponse::from)
                .toList();
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

    private void asegurarLigaActiva(Liga liga) {
        if (!Boolean.TRUE.equals(liga.getActivo())) {
            throw new BusinessException("La liga esta inactiva; no se pueden configurar temporadas");
        }
    }

    private void validarConfiguracion(Liga liga, CrearTemporadaRequest request, Long temporadaId) {
        if (request.fechaFin().isBefore(request.fechaInicio())) {
            throw new BusinessException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        String nombre = request.nombre().trim();
        boolean duplicada = temporadaId == null
                ? temporadaRepository.existsByLigaIdAndNombreIgnoreCase(liga.getId(), nombre)
                : temporadaRepository.existsByLigaIdAndNombreIgnoreCaseAndIdNot(liga.getId(), nombre, temporadaId);
        if (duplicada) {
            throw new BusinessException("Ya existe una temporada con ese nombre dentro de la liga");
        }
    }

    private FormatoCompetitivo buscarFormatoCompatible(Liga liga, Long formatoId) {
        if (formatoId == null) {
            throw new BusinessException("Debe seleccionar un formato activo y compatible con el juego");
        }
        FormatoCompetitivo formato = formatoRepository.findById(formatoId)
                .orElseThrow(() -> new ResourceNotFoundException("Formato competitivo", formatoId));
        if (!Boolean.TRUE.equals(formato.getActivo())) {
            throw new BusinessException("El formato seleccionado no esta activo");
        }
        return formato;
    }
}
