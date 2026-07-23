/**
 * Portadas determinísticas para tarjetas de juegos, ligas y torneos.
 *
 * Las referencias visuales usan arte oficial de cada juego; nosotros no
 * podemos embeber key-art con copyright, así que cada entidad recibe un
 * gradiente propio derivado de su nombre: mismo nombre → misma portada,
 * siempre. Con esto las grillas dejan de ser tarjetas idénticas sin
 * inventar imágenes que no tenemos.
 */

/** Hash simple y estable (djb2) para derivar el matiz del nombre. */
function hash(texto: string): number {
  let h = 5381;
  for (let i = 0; i < texto.length; i++) {
    h = (h * 33) ^ texto.charCodeAt(i);
  }
  return Math.abs(h);
}

/**
 * Fotografías de stock libre (licencia Pexels, uso libre) para los juegos
 * del catálogo semilla. La referencia visual usa key-art oficial de cada
 * juego, que no podemos embeber por copyright; estas fotos temáticas dan
 * el mismo efecto de tarjeta fotográfica. Verificadas en el navegador.
 */
const FOTOS_STOCK: Record<string, string> = {
  'league of legends':
    'https://images.pexels.com/photos/7848987/pexels-photo-7848987.jpeg?auto=compress&cs=tinysrgb&w=800',
  valorant:
    'https://images.pexels.com/photos/9072394/pexels-photo-9072394.jpeg?auto=compress&cs=tinysrgb&w=800',
  'counter-strike 2':
    'https://images.pexels.com/photos/6125330/pexels-photo-6125330.jpeg?auto=compress&cs=tinysrgb&w=800',
  'rocket league':
    'https://images.pexels.com/photos/13930769/pexels-photo-13930769.jpeg?auto=compress&cs=tinysrgb&w=800',
  'ea sports fc 25':
    'https://images.pexels.com/photos/1657324/pexels-photo-1657324.jpeg?auto=compress&cs=tinysrgb&w=800'
};

/**
 * Fotografía de portada para un nombre conocido, o null si no hay.
 * La cadena de prioridad completa la resuelve cada componente:
 * imagenUrl real → foto de stock → gradiente determinístico.
 */
export function portadaFoto(nombre: string): string | null {
  return FOTOS_STOCK[(nombre || '').trim().toLowerCase()] ?? null;
}

/**
 * Gradiente de portada. Dos tonos del mismo matiz en diagonal, oscurecidos
 * para que el texto claro y el monograma mantengan contraste encima.
 */
export function portadaGradiente(nombre: string): string {
  const matiz = hash(nombre || '?') % 360;
  const matizB = (matiz + 35) % 360;
  return `linear-gradient(135deg, hsl(${matiz} 65% 32%), hsl(${matizB} 75% 18%))`;
}

/** Color sólido derivado del nombre, para acentos menores (avatares, chips). */
export function colorDeNombre(nombre: string): string {
  const matiz = hash(nombre || '?') % 360;
  return `hsl(${matiz} 60% 45%)`;
}
