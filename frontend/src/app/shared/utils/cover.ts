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
