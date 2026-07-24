export interface Patrocinador {
  id: number;
  nombre: string;
  logo: string | null;
  contacto: string;
  descripcion: string | null;
  estado: string;
  enlaces: string[];
}

export interface CrearPatrocinadorRequest {
  nombre: string;
  logo: string | null;
  contacto: string;
  descripcion: string | null;
  enlaces: string[];
  confirmarDuplicado: boolean;
}

export interface EditarPatrocinadorRequest {
  nombre: string;
  logo: string | null;
  contacto: string;
  descripcion: string | null;
  enlaces: string[];
}
