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
  imagenUrl: string | null;
}

/** Resultado del buscador externo (RAWG) para precargar el formulario. */
export interface JuegoExterno {
  nombre: string;
  genero: string;
  imagenUrl: string | null;
}
