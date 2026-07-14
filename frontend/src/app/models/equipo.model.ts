export interface Equipo {
  id: number;
  nombre: string;
  logo: string | null;
  descripcion: string | null;
  juegoId: number;
  juegoNombre: string;
  capitanId: number;
  capitanNombre: string;
  estado: string;
  estadoPrivacidad: string;
  /** Versión para control de concurrencia optimista; se reenvía en el PUT. */
  version: number;
  redesSociales: string[];
  fechaDisolucion: string | null;
  motivoDisolucion: string | null;
}

export interface CrearEquipoRequest {
  nombre: string;
  logo: string | null;
  descripcion: string | null;
  juegoId: number;
  redesSociales: string[];
}

/**
 * RF-02: edición parcial. null/ausente = no tocar el campo;
 * string vacío = borrarlo (logo y descripción).
 */
export interface EditarEquipoRequest {
  nombre?: string | null;
  logo?: string | null;
  descripcion?: string | null;
  juegoId?: number | null;
  redesSociales?: string[] | null;
  estadoPrivacidad?: string | null;
  /** Versión leída en el GET; el backend responde 409 si alguien guardó entre medio. */
  version?: number;
}

export interface DisolverEquipoRequest {
  confirmacion: boolean;
  motivo: string | null;
}
