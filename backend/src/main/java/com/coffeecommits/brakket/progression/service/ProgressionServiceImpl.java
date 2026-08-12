package com.coffeecommits.brakket.progression.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.progression.dto.ProgresionResponse;
import com.coffeecommits.brakket.progression.dto.PerfilPersonalizadoResponse;
import com.coffeecommits.brakket.auth.model.VisibilidadPerfil;
import com.coffeecommits.brakket.progression.model.*;
import com.coffeecommits.brakket.progression.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class ProgressionServiceImpl implements ProgressionService {
    private final UsuarioRepository usuarios; private final LogroRepository logros;
    private final LogroJugadorRepository logrosJugador; private final ElementoPersonalizacionRepository elementos;
    private final CanjePersonalizacionRepository canjes; private final PersonalizacionPerfilRepository perfiles;
    private final AchievementSynchronizer achievementSynchronizer;
    public ProgressionServiceImpl(UsuarioRepository u, LogroRepository l, LogroJugadorRepository lj,
            ElementoPersonalizacionRepository e, CanjePersonalizacionRepository c, PersonalizacionPerfilRepository p,
            AchievementSynchronizer achievementSynchronizer) {
        usuarios=u; logros=l; logrosJugador=lj; elementos=e; canjes=c; perfiles=p;
        this.achievementSynchronizer=achievementSynchronizer;
    }

    @Override @Transactional public ProgresionResponse consultar(String correo) {
        Usuario u=usuario(correo); achievementSynchronizer.sincronizar(u); return respuesta(u);
    }

    @Override @Transactional public ProgresionResponse canjear(String correo, Long elementoId) {
        Usuario u=usuarioBloqueado(correo); achievementSynchronizer.sincronizar(u); ElementoPersonalizacion e=elemento(elementoId);
        if (!Boolean.TRUE.equals(e.getActivo())) throw new BusinessException("El elemento ya no está disponible.");
        if (canjes.existsByUsuarioIdAndElementoId(u.getId(), e.getId())) throw new BusinessException("Ya canjeaste este elemento.");
        if (e.getLogroRequerido()!=null && !logrosJugador.existsByUsuarioIdAndLogroIdAndActivoTrue(u.getId(), e.getLogroRequerido().getId()))
            throw new BusinessException("El logro requerido aún no está registrado.");
        if (saldo(u.getId()) < e.getCostoPuntos()) throw new BusinessException("No tienes puntos suficientes para este canje.");
        canjes.save(CanjePersonalizacion.builder().usuario(u).elemento(e).costoPuntos(e.getCostoPuntos()).fechaCanje(LocalDateTime.now()).build());
        return respuesta(u);
    }

    @Override @Transactional public ProgresionResponse aplicar(String correo, Long elementoId) {
        Usuario u=usuarioBloqueado(correo); ElementoPersonalizacion e=elemento(elementoId);
        if (!Boolean.TRUE.equals(e.getActivo())) throw new BusinessException("El elemento ya no está disponible.");
        if (!canjes.existsByUsuarioIdAndElementoId(u.getId(), e.getId())) throw new BusinessException("Debes canjear el elemento antes de aplicarlo.");
        PersonalizacionPerfilId id=new PersonalizacionPerfilId(u.getId(), e.getTipo());
        PersonalizacionPerfil p=perfiles.findById(id).orElseGet(() -> PersonalizacionPerfil.builder().usuario(u).tipo(e.getTipo()).build());
        p.setElemento(e); perfiles.save(p); return respuesta(u);
    }

    @Override @Transactional public ProgresionResponse quitar(String correo, Long elementoId) {
        Usuario u=usuarioBloqueado(correo); ElementoPersonalizacion e=elemento(elementoId);
        PersonalizacionPerfilId id=new PersonalizacionPerfilId(u.getId(),e.getTipo());
        PersonalizacionPerfil aplicada=perfiles.findById(id)
                .orElseThrow(() -> new BusinessException("Este elemento no está aplicado."));
        if (!aplicada.getElemento().getId().equals(e.getId()))
            throw new BusinessException("Este elemento no está aplicado.");
        perfiles.delete(aplicada);
        return respuesta(u);
    }

    @Override @Transactional public void registrarLogro(Long usuarioId, Long logroId, String referencia) {
        if (referencia==null || referencia.isBlank()) throw new IllegalArgumentException("El logro requiere una referencia del sistema.");
        Usuario u=usuarios.findById(usuarioId).orElseThrow(() -> new ResourceNotFoundException("Jugador no encontrado"));
        Logro l=logros.findById(logroId).filter(x -> Boolean.TRUE.equals(x.getActivo())).orElseThrow(() -> new ResourceNotFoundException("Logro no encontrado"));
        if (!logrosJugador.existsByUsuarioIdAndLogroIdAndActivoTrue(usuarioId, logroId))
            logrosJugador.save(LogroJugador.builder().usuario(u).logro(l).fechaDesbloqueo(LocalDate.now()).activo(true).referenciaSistema(referencia.trim()).build());
    }

    @Override @Transactional public void revertirLogro(Long usuarioId, Long logroId) {
        logrosJugador.findByUsuarioIdAndActivoTrue(usuarioId).stream().filter(x -> x.getLogro().getId().equals(logroId)).forEach(x -> {
            x.setActivo(false); x.setFechaReversion(LocalDateTime.now());
        });
    }

    @Override @Transactional(readOnly=true) public PerfilPersonalizadoResponse perfilPublico(Long usuarioId) {
        Usuario u=usuarios.findById(usuarioId).orElseThrow(() -> new ResourceNotFoundException("Jugador no encontrado"));
        if (u.getVisibilidadPerfil()==VisibilidadPerfil.PRIVATE)
            throw new com.coffeecommits.brakket.common.exception.ForbiddenException("Este jugador mantiene su perfil en privado");
        var aplicados=perfiles.findByUsuarioId(usuarioId);
        var titulo=aplicados.stream().filter(p -> p.getTipo()==TipoPersonalizacion.TITULO)
                .map(PersonalizacionPerfil::getElemento).filter(e -> Boolean.TRUE.equals(e.getActivo()))
                .map(this::elementoAplicado).findFirst().orElse(null);
        var insignias=aplicados.stream().filter(p -> p.getTipo()==TipoPersonalizacion.INSIGNIA)
                .map(PersonalizacionPerfil::getElemento).filter(e -> Boolean.TRUE.equals(e.getActivo()))
                .map(this::elementoAplicado).toList();
        return new PerfilPersonalizadoResponse(u.getId(),u.getNombre(),titulo,insignias);
    }

    private ProgresionResponse respuesta(Usuario u) {
        var obtenidos=logrosJugador.findByUsuarioIdAndActivoTrue(u.getId());
        Set<Long> logroIds=new HashSet<>(); obtenidos.forEach(x -> logroIds.add(x.getLogro().getId()));
        var comprados=canjes.findByUsuarioId(u.getId()); Set<Long> compradosIds=new HashSet<>(); comprados.forEach(x -> compradosIds.add(x.getElemento().getId()));
        Map<TipoPersonalizacion,Long> aplicados=new EnumMap<>(TipoPersonalizacion.class);
        perfiles.findByUsuarioId(u.getId()).forEach(x -> aplicados.put(x.getTipo(), x.getElemento().getId()));
        Map<Long,LogroJugador> registrosActivos=new HashMap<>();
        obtenidos.forEach(x -> registrosActivos.put(x.getLogro().getId(),x));
        var lr=logros.findAll().stream().filter(l -> Boolean.TRUE.equals(l.getActivo()))
                .sorted(Comparator
                        .comparing((Logro l) -> !registrosActivos.containsKey(l.getId()))
                        .thenComparing(Logro::getPuntosValor)
                        .thenComparing(Logro::getNombre))
                .map(l -> {
                    LogroJugador registro=registrosActivos.get(l.getId());
                    return new ProgresionResponse.LogroResponse(l.getId(),l.getNombre(),l.getDescripcion(),
                            l.getPuntosValor(),l.getOrigen(),registro!=null,
                            registro==null?null:registro.getFechaDesbloqueo());
                }).toList();
        var er=elementos.findAllByOrderByCostoPuntosAsc().stream().map(e -> new ProgresionResponse.ElementoResponse(e.getId(),e.getNombre(),e.getDescripcion(),e.getTipo().name(),e.getCostoPuntos(),Boolean.TRUE.equals(e.getActivo()),compradosIds.contains(e.getId()),Objects.equals(aplicados.get(e.getTipo()),e.getId()),e.getLogroRequerido()==null||logroIds.contains(e.getLogroRequerido().getId()),e.getLogroRequerido()==null?"Disponible por puntos":"Requiere: "+e.getLogroRequerido().getNombre()+" · "+e.getLogroRequerido().getOrigen())).toList();
        return new ProgresionResponse(saldo(u.getId()),lr,er);
    }
    private int saldo(Long id) { return logrosJugador.findByUsuarioIdAndActivoTrue(id).stream().mapToInt(x->x.getLogro().getPuntosValor()).sum()-canjes.findByUsuarioId(id).stream().mapToInt(CanjePersonalizacion::getCostoPuntos).sum(); }
    private Usuario usuario(String correo) { return usuarios.findByCorreo(correo).orElseThrow(() -> new ResourceNotFoundException("Jugador no encontrado")); }
    private Usuario usuarioBloqueado(String correo) { return usuarios.findLockedByCorreo(correo).orElseThrow(() -> new ResourceNotFoundException("Jugador no encontrado")); }
    private ElementoPersonalizacion elemento(Long id) { return elementos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Elemento no encontrado")); }
    private PerfilPersonalizadoResponse.ElementoAplicado elementoAplicado(ElementoPersonalizacion e) {
        return new PerfilPersonalizadoResponse.ElementoAplicado(e.getId(),e.getNombre(),e.getDescripcion());
    }
}
