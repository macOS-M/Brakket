import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AsistenteIaComponent } from './asistente-ia.component';
import { AuthService } from '../../../../core/services/auth.service';
import { AsistenteRespuesta } from '../../../../models/sentiment.model';

describe('AsistenteIaComponent', () => {
  let component: AsistenteIaComponent;
  let fixture: ComponentFixture<AsistenteIaComponent>;
  let http: HttpTestingController;

  /** Rol del usuario de la prueba; se cambia antes de crear el componente. */
  let esAdmin = true;

  const respuesta = (extra: Partial<AsistenteRespuesta> = {}): AsistenteRespuesta => ({
    respuesta: 'El pico fue a las 14:01.',
    generadaPorIa: true,
    aviso: null,
    ...extra
  });

  /** Atiende la consulta al asistente y devuelve la petición para inspeccionarla. */
  const atender = (cuerpo: AsistenteRespuesta) => {
    const peticion = http.expectOne((r) => r.url.includes('/asistente'));
    peticion.flush(cuerpo);
    fixture.detectChanges();
    return peticion.request;
  };

  const crear = async () => {
    await TestBed.configureTestingModule({
      imports: [AsistenteIaComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { hasRole: () => esAdmin } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AsistenteIaComponent);
    component = fixture.componentInstance;
    component.transmisionId = 15;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    esAdmin = true;
    await crear();
  });

  afterEach(() => http.verify());

  it('no pinta nada cuando el usuario no es admin', async () => {
    esAdmin = false;
    TestBed.resetTestingModule();
    await crear();

    // El backend igual lo exige; esto evita ofrecer un boton que daria 403.
    expect(fixture.nativeElement.querySelector('.fab')).toBeNull();
  });

  it('muestra el boton flotante para un admin', () => {
    expect(fixture.nativeElement.querySelector('.fab')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.panel')).toBeNull();
  });

  it('abre y cierra el panel', () => {
    component.alternar();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.panel')).not.toBeNull();

    component.alternar();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.panel')).toBeNull();
  });

  it('manda la pregunta y el periodo consultado', () => {
    component.desde = '2026-08-12T10:00:00';
    component.pregunta = '  cuando hubo mas chat  ';

    component.enviar();
    const peticion = atender(respuesta());

    expect(peticion.method).toBe('POST');
    expect(peticion.url).toContain('/analytics/transmisiones/15/asistente');
    expect(peticion.url).toContain('desde=2026-08-12T10%3A00%3A00');
    // Se recorta: los espacios de sobra no son parte de la pregunta.
    expect(peticion.body).toEqual({ pregunta: 'cuando hubo mas chat' });
  });

  it('deja la pregunta y la respuesta en el historial', () => {
    component.pregunta = 'cuando hubo mas chat';

    component.enviar();
    atender(respuesta());

    expect(component.turnos()).toEqual([
      { autor: 'usuario', texto: 'cuando hubo mas chat' },
      { autor: 'asistente', texto: 'El pico fue a las 14:01.', generadaPorIa: true, aviso: null }
    ]);
    // El campo queda limpio para la siguiente.
    expect(component.pregunta).toBe('');
  });

  it('muestra el aviso cuando la respuesta no vino de la IA', () => {
    component.alternar();
    component.pregunta = 'cuando hubo mas chat';

    component.enviar();
    atender(respuesta({
      generadaPorIa: false,
      aviso: 'Se alcanzó el límite de consultas del proveedor.'
    }));

    // Presentar una respuesta de plantilla como si la hubiera escrito el modelo
    // seria enganoso: el aviso tiene que verse.
    expect(fixture.nativeElement.querySelector('.aviso').textContent)
      .toContain('límite de consultas');
  });

  it('no consulta cuando no hay transmision elegida', () => {
    component.transmisionId = null;
    component.pregunta = 'cuando hubo mas chat';

    component.enviar();

    http.expectNone((r) => r.url.includes('/asistente'));
    expect(component.error()).toContain('Elegí una transmisión');
  });

  it('no consulta con la pregunta vacia', () => {
    component.pregunta = '   ';

    component.enviar();

    http.expectNone((r) => r.url.includes('/asistente'));
    expect(component.turnos()).toEqual([]);
  });

  it('traduce el 403 a un mensaje de permisos', () => {
    component.pregunta = 'cuando hubo mas chat';

    component.enviar();
    http.expectOne((r) => r.url.includes('/asistente'))
      .flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();

    expect(component.error()).toContain('administrador');
    expect(component.enviando()).toBeFalse();
  });

  it('una sugerencia consulta directamente', () => {
    component.usarSugerencia('¿Cuándo bajó la participación?');
    const peticion = atender(respuesta());

    expect(peticion.body).toEqual({ pregunta: '¿Cuándo bajó la participación?' });
  });

  it('pide clasificar el chat acumulado sin esperar al bloque', () => {
    component.analizarAhora();

    const peticion = http.expectOne((r) => r.url.includes('/analytics/muestreo/sentimiento'));
    expect(peticion.request.method).toBe('POST');
    peticion.flush({ clasificado: true, mensajes: 180, mensaje: 'Se analizó un bloque de 180 mensajes.' });

    expect(component.avisoAnalisis()).toContain('180 mensajes');
    expect(component.analizando()).toBeFalse();
  });

  it('avisa a la pagina cuando dejo analisis nuevo', () => {
    let avisos = 0;
    component.analisisHecho.subscribe(() => avisos++);

    component.analizarAhora();
    http.expectOne((r) => r.url.includes('/analytics/muestreo/sentimiento'))
      .flush({ clasificado: true, mensajes: 180, mensaje: 'Se analizó un bloque.' });

    // Una transmision recien empezada no figura en el selector hasta tener su
    // primer analisis; sin este aviso habria que refrescar a mano.
    expect(avisos).toBe(1);
  });

  it('no avisa cuando no habia chat que clasificar', () => {
    let avisos = 0;
    component.analisisHecho.subscribe(() => avisos++);

    component.analizarAhora();
    http.expectOne((r) => r.url.includes('/analytics/muestreo/sentimiento'))
      .flush({ clasificado: false, mensajes: 0, mensaje: 'Todavía no hay chat acumulado.' });

    expect(avisos).toBe(0);
  });

  it('no dispara dos analisis a la vez', () => {
    component.analizarAhora();
    component.analizarAhora();

    // El segundo click mientras corre el primero no debe gastar otra llamada
    // al proveedor.
    http.expectOne((r) => r.url.includes('/analytics/muestreo/sentimiento'))
      .flush({ clasificado: false, mensajes: 0, mensaje: 'Todavía no hay chat acumulado.' });

    expect(component.avisoAnalisis()).toContain('no hay chat acumulado');
  });

  it('limpiar vacia el historial', () => {
    component.pregunta = 'cuando hubo mas chat';
    component.enviar();
    atender(respuesta());

    component.limpiar();

    expect(component.turnos()).toEqual([]);
    expect(component.error()).toBeNull();
  });
});
