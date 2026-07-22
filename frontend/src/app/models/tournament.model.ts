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
  jugadores: JugadorInscrito[];
}

export interface TorneoDetalle {
  torneo: Torneo;
  equipos: EquipoInscrito[];
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
}
