import { EtiquetaPipe } from './etiqueta.pipe';

describe('EtiquetaPipe', () => {
  const pipe = new EtiquetaPipe();

  it('quita los guiones bajos y deja solo la primera en mayúscula', () => {
    expect(pipe.transform('EN_VIVO')).toBe('En vivo');
    expect(pipe.transform('EQUIPO_ELIMINADO')).toBe('Equipo eliminado');
    expect(pipe.transform('SIN_DATOS_EN_VIVO')).toBe('Sin datos en vivo');
    expect(pipe.transform('ACTIVO')).toBe('Activo');
  });

  it('usa el diccionario cuando la regla genérica perdería la tilde', () => {
    expect(pipe.transform('INSCRIPCION_ABIERTA')).toBe('Inscripción abierta');
    expect(pipe.transform('EN_APELACION')).toBe('En apelación');
    expect(pipe.transform('CORRECCION')).toBe('Corrección');
    expect(pipe.transform('VER_METRICAS_AUDIENCIA')).toBe('Ver métricas de audiencia');
  });

  it('traduce los códigos que la base guarda en inglés', () => {
    expect(pipe.transform('OFFLINE')).toBe('Fuera de línea');
    expect(pipe.transform('PUBLIC')).toBe('Público');
    expect(pipe.transform('PRIVATE')).toBe('Privado');
  });

  it('agrega la preposición donde la frase la necesita', () => {
    expect(pipe.transform('SOLICITUD_UNION')).toBe('Solicitud de unión');
    expect(pipe.transform('CAMBIO_TORNEO')).toBe('Cambio de torneo');
    expect(pipe.transform('PARTICIPAR_PARTIDAS')).toBe('Participar en partidas');
  });

  it('no toca el texto que ya viene escrito para leer', () => {
    // La categoría de Twitch, un nombre propio o el origen "Sistema" de una
    // notificación no son códigos: pasarlos por la regla genérica los dejaría
    // en minúscula.
    expect(pipe.transform('Just Chatting')).toBe('Just Chatting');
    expect(pipe.transform('Sistema')).toBe('Sistema');
    expect(pipe.transform('Tigres UCR')).toBe('Tigres UCR');
  });

  it('acepta el código con espacios en vez de guiones bajos', () => {
    expect(pipe.transform('EN CURSO')).toBe('En curso');
    expect(pipe.transform('INSCRIPCION ABIERTA')).toBe('Inscripción abierta');
  });

  it('sin valor devuelve cadena vacía', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
    expect(pipe.transform('')).toBe('');
    expect(pipe.transform('   ')).toBe('');
  });
});
