/**
 * Traduce un código de la base al texto que lee la persona usuaria:
 * "EN_VIVO" → "En vivo", "INSCRIPCION_ABIERTA" → "Inscripción abierta".
 *
 * La regla genérica —guiones bajos a espacios y solo la primera letra en
 * mayúscula— resuelve bien la mayoría de los códigos ("EQUIPO_EDITADO" →
 * "Equipo editado"), así que el mapa de abajo solo lista los que se le
 * escapan: los que llevan tilde, los que necesitan una preposición para
 * leerse como una frase y los que la base guarda en inglés.
 */
const NOMBRES: Record<string, string> = {
  // ----- Estados -----
  INSCRIPCION_ABIERTA: 'Inscripción abierta',
  EN_APELACION: 'En apelación',
  // La base guarda el estado del canal en inglés.
  OFFLINE: 'Fuera de línea',

  // ----- Tipos de notificación -----
  INVITACION: 'Invitación',
  INVITACION_EQUIPO: 'Invitación de equipo',
  INVITACION_ACEPTADA: 'Invitación aceptada',
  INVITACION_RECHAZADA: 'Invitación rechazada',
  SOLICITUD_UNION: 'Solicitud de unión',
  CAMBIO_TORNEO: 'Cambio de torneo',
  TRANSMISION: 'Transmisión',
  EXPULSION_EQUIPO: 'Expulsión de equipo',
  CORRECCION: 'Corrección',

  // ----- Torneo -----
  ELIMINACION: 'Eliminación',
  AVANCE_AUTOMATICO: 'Avance automático',

  // ----- Roles de la plataforma -----
  ARBITRO: 'Árbitro',
  CAPITAN: 'Capitán',

  // ----- Permisos -----
  VER_ESTADISTICAS: 'Ver estadísticas',
  VER_METRICAS_AUDIENCIA: 'Ver métricas de audiencia',
  PARTICIPAR_PARTIDAS: 'Participar en partidas',

  // ----- Visibilidad del perfil -----
  PUBLIC: 'Público',
  PRIVATE: 'Privado'
};

// Un código de la base: solo mayúsculas, dígitos y separadores. Sirve para no
// tocar los campos que ya vienen escritos para leer —la categoría de Twitch,
// un nombre propio, el origen "Sistema" de una notificación—, que si pasaran
// por la regla genérica quedarían en minúscula.
const ES_CODIGO = /^[A-ZÁÉÍÓÚÑ0-9]+([ _][A-ZÁÉÍÓÚÑ0-9]+)*$/;

export function etiquetaDeCodigo(valor: string | null | undefined): string {
  if (!valor) {
    return '';
  }
  const bruto = valor.trim();
  if (!bruto) {
    return '';
  }
  const conocido = NOMBRES[bruto.toUpperCase().replace(/ +/g, '_')];
  if (conocido) {
    return conocido;
  }
  if (!ES_CODIGO.test(bruto)) {
    return bruto;
  }
  const texto = bruto.replace(/_/g, ' ').toLowerCase();
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}
