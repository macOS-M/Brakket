export type TournamentStatus = 'DRAFT' | 'REGISTRATION' | 'ONGOING' | 'FINISHED';

export interface Tournament {
  id: string;
  nombre: string;
  juegoId?: string;
  ligaId?: string;
  estado: TournamentStatus;
  fechaInicio?: string;
  fechaFin?: string;
  equipos?: string[];
}
