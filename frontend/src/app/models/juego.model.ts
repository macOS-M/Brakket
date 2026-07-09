export interface Juego {
  id: number;
  nombre: string;
  genero: string;
  descripcion: string | null;
  imagenUrl: string | null;
  activo: boolean;
}

export interface JuegoRequest {
  nombre: string;
  genero: string;
  descripcion: string;
}
