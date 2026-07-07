package cr.brakket.equipos.repository;

import cr.brakket.equipos.domain.MiembroEquipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MiembroEquipoRepository extends JpaRepository<MiembroEquipo, Long> {

    List<MiembroEquipo> findByEquipoId(Long equipoId);

    List<MiembroEquipo> findByUsuarioId(Long usuarioId);
}
