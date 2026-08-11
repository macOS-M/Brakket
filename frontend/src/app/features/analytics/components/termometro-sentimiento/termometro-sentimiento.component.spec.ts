import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TermometroSentimientoComponent } from './termometro-sentimiento.component';
import { Termometro } from '../../../../models/sentiment.model';

describe('TermometroSentimientoComponent', () => {
  let component: TermometroSentimientoComponent;
  let fixture: ComponentFixture<TermometroSentimientoComponent>;

  const base: Termometro = {
    transmisionId: 7,
    estado: 'DISPONIBLE',
    resumen: 'El ambiente del chat es mayormente positivo.',
    desde: null,
    hasta: null,
    intervaloMinutos: 5,
    puntajeGeneral: 40,
    clasificacion: 'POSITIVO',
    totalMuestras: 6,
    minimoMuestras: 3,
    distribucion: {
      positivo: 4,
      neutro: 1,
      negativo: 1,
      porcentajePositivo: 66.67,
      porcentajeNeutro: 16.67,
      porcentajeNegativo: 16.67
    },
    intervalos: [
      { inicio: '2026-08-10T20:00:00', fin: '2026-08-10T20:05:00', muestras: 3, puntajePromedio: 20, clasificacion: 'POSITIVO' },
      { inicio: '2026-08-10T20:05:00', fin: '2026-08-10T20:10:00', muestras: 3, puntajePromedio: 60, clasificacion: 'POSITIVO' }
    ]
  };

  const montar = (termometro: Termometro | null) => {
    component.termometro = termometro;
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TermometroSentimientoComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TermometroSentimientoComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    montar(base);
    expect(component).toBeTruthy();
  });

  it('muestra el indicador cuando el análisis está disponible', () => {
    const html = montar(base);

    expect(component.hayIndicador()).toBeTrue();
    expect(html.querySelector('.puntaje')?.textContent).toContain('40');
    expect(html.querySelector('.badge')?.textContent).toContain('POSITIVO');
  });

  it('oculta el indicador cuando los datos son insuficientes', () => {
    const html = montar({
      ...base,
      estado: 'INSUFICIENTE',
      puntajeGeneral: null,
      clasificacion: null,
      totalMuestras: 2,
      resumen: 'Hay 2 muestras y se necesitan al menos 3.'
    });

    // La ERS pide ocultar el termómetro definitivo, no mostrar un cero.
    expect(component.hayIndicador()).toBeFalse();
    expect(html.querySelector('.puntaje')).toBeNull();
    expect(html.querySelector('.barra')).toBeNull();
  });

  it('oculta el indicador cuando el análisis está pendiente', () => {
    const html = montar({
      ...base,
      estado: 'PENDIENTE',
      puntajeGeneral: null,
      clasificacion: null,
      totalMuestras: 0,
      distribucion: {
        positivo: 0, neutro: 0, negativo: 0,
        porcentajePositivo: 0, porcentajeNeutro: 0, porcentajeNegativo: 0
      },
      intervalos: [],
      resumen: 'Todavía no hay análisis para esta transmisión.'
    });

    expect(html.querySelector('.puntaje')).toBeNull();
    expect(html.querySelector('.distribucion')).toBeNull();
    expect(html.querySelector('.estado')?.textContent).toContain('pendiente');
  });

  it('muestra el resumen en todos los estados', () => {
    const html = montar({ ...base, estado: 'PENDIENTE', puntajeGeneral: null, clasificacion: null });
    expect(html.querySelector('.resumen')?.textContent).toContain('mayormente positivo');
  });

  it('siempre muestra el aviso de que no evalúa usuarios', () => {
    const html = montar(base);
    // Criterio explícito de la ERS: apoyo informativo, no decisión automática.
    expect(html.querySelector('.aviso')?.textContent).toContain('No evalúa ni sanciona');
  });

  it('dibuja la distribución con una columna por clasificación', () => {
    const html = montar(base);

    expect(html.querySelectorAll('.reparto .tramo').length).toBe(3);
    expect(html.querySelector('.leyenda')?.textContent).toContain('66.7');
  });

  it('ubica la aguja convirtiendo la escala de -100..100 a 0..100', () => {
    montar({ ...base, puntajeGeneral: 0 });
    expect(component.posicionAguja()).toBe(50);

    montar({ ...base, puntajeGeneral: -100 });
    expect(component.posicionAguja()).toBe(0);

    montar({ ...base, puntajeGeneral: 100 });
    expect(component.posicionAguja()).toBe(100);
  });

  it('no dibuja la evolución con un solo intervalo', () => {
    const html = montar({ ...base, intervalos: [base.intervalos[0]] });

    // Una sola columna no es una evolución.
    expect(component.hayEvolucion()).toBeFalse();
    expect(html.querySelector('.evolucion')).toBeNull();
  });

  it('dibuja una columna por intervalo cuando hay evolución', () => {
    const html = montar(base);
    expect(html.querySelectorAll('.grafico .columna').length).toBe(2);
  });

  it('da alto mínimo a un intervalo de puntaje cero para que se vea', () => {
    expect(component.altura(0)).toBe(4);
    expect(component.altura(100)).toBe(50);
  });

  it('no renderiza nada sin lectura', () => {
    const html = montar(null);
    expect(html.querySelector('.termometro')).toBeNull();
  });
});
