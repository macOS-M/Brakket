/** Regla de partida definida por el organizador (tipo "Game settings" de CM). */
export interface AjustePartida {
  clave: string;
  valor: string;
}

/** Torneo con modelo abierto de organizadores (RF-24/RF-25). */
export interface Torneo {
  id: number;
  nombre: string;
  descripcion: string | null;
  juegoId: number;
  juegoNombre: string;
  juegoImagenUrl: string | null;
  ligaId: number | null;
  ligaNombre: string | null;
  temporadaId: number | null;
  temporadaNombre: string | null;
  organizadorId: number;
  organizadorNombre: string;
  formato: string;
  tamanoEquipo: number;
  maxEquipos: number;
  inscritos: number;
  fechaInicio: string;
  estado: string;
  publico: boolean;
  premio: string | null;
  ajustesPartida: AjustePartida[];
  campeonEquipoId: number | null;
  campeonNombre: string | null;
}

export interface JugadorInscrito {
  usuarioId: number;
  nombre: string;
  rol: string;
}

export interface EquipoInscrito {
  equipoId: number;
  nombre: string;
  logo: string | null;
  /** Gamertag del capitán dentro del juego (null en datos previos). */
  usuarioEnJuego: string | null;
  jugadores: JugadorInscrito[];
}

export interface TorneoDetalle {
  torneo: Torneo;
  equipos: EquipoInscrito[];
  arbitrosIds: number[];
}

export interface EquipoElegible {
  id: number;
  nombre: string;
  logo: string | null;
}

export interface CrearTorneoRequest {
  nombre: string;
  juegoId: number;
  temporadaId: number | null;
  formato: string;
  tamanoEquipo: number;
  maxEquipos: number;
  fechaInicio: string;
  publico: boolean;
  descripcion: string | null;
  premio: string | null;
  ajustesPartida: AjustePartida[];
}

/**
 * Enfrentamiento del bracket (RF-26/27). La lobby (nombre + clave) es el
 * puente con el juego: Brakket genera las credenciales de la partida
 * privada que ambos capitanes usan dentro del juego.
 */
export interface Partida {
  id: number;
  ronda: number;
  orden: number;
  /** Sección: WINNERS/LOSERS/GRAN_FINAL (doble elim), GRUPOS/ELIMINACION (fase de grupos). */
  fase: 'WINNERS' | 'LOSERS' | 'GRAN_FINAL' | 'GRUPOS' | 'ELIMINACION' | null;
  /** Índice del grupo (0-based) cuando fase = GRUPOS. */
  grupo: number | null;
  equipoAId: number | null;
  equipoANombre: string | null;
  equipoALogo: string | null;
  equipoBId: number | null;
  equipoBNombre: string | null;
  equipoBLogo: string | null;
  marcadorA: number | null;
  marcadorB: number | null;
  ganadorEquipoId: number | null;
  reportadoPorEquipoId: number | null;
  estado: 'PENDIENTE' | 'REPORTADA' | 'EN_DISPUTA' | 'FINALIZADA' | 'CANCELADA';
  bye: boolean;
  lobbyNombre: string | null;
  lobbyClave: string | null;
  siguientePartidaId: number | null;
}
