export interface Juego {
  id: number;
  nombre: string;
  genero: string;
  descripcion: string | null;
  imagenUrl: string | null;
  activo: boolean;
  /** Ficha enriquecida desde RAWG (V28); todo opcional. */
  fechaLanzamiento?: string | null;
  rating?: number | null;
  metacritic?: number | null;
  plataformas?: string | null;
  etiquetas?: string | null;
  sitioWeb?: string | null;
  capturas?: string[];
  trailerId?: string | null;
}

export interface JuegoRequest {
  nombre: string;
  genero: string;
  descripcion: string;
  imagenUrl: string | null;
}

/** Resultado del buscador externo (RAWG) para precargar el formulario. */
export interface JuegoExterno {
  slug?: string;
  nombre: string;
  genero: string;
  imagenUrl: string | null;
}
