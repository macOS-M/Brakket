import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FechaInputComponent } from './fecha-input.component';

describe('FechaInputComponent', () => {
  let fixture: ComponentFixture<FechaInputComponent>;
  let componente: FechaInputComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FechaInputComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(FechaInputComponent);
    componente = fixture.componentInstance;
    fixture.detectChanges();
  });

  /** Abre el panel y devuelve el botón del día indicado del mes en vista. */
  function celdaDelMes(dia: number): HTMLButtonElement {
    const celdas = fixture.nativeElement.querySelectorAll('.dia:not(.otro-mes)');
    return Array.from(celdas).find(
      (b) => (b as HTMLElement).textContent?.trim() === String(dia)
    ) as HTMLButtonElement;
  }

  it('se crea vacío y sin panel abierto', () => {
    expect(componente).toBeTruthy();
    expect(componente.valor()).toBe('');
    expect(componente.abierto()).toBeFalse();
  });

  it('muestra el marcador mientras no hay valor', () => {
    const texto = fixture.nativeElement.querySelector('.texto') as HTMLElement;
    expect(texto.textContent?.trim()).toBe('Seleccionar fecha');
  });

  it('formatea el valor recibido sin correrse de día por zona horaria', () => {
    // new Date('2026-08-11') se interpreta como UTC y en CR (-06:00) caería
    // en el día 10: esta prueba es la que protege ese caso.
    componente.writeValue('2026-08-11');
    fixture.detectChanges();

    expect(componente.textoValor()).toBe('11 ago 2026');
  });

  it('emite YYYY-MM-DD al elegir un día', () => {
    const alCambiar = jasmine.createSpy('alCambiar');
    componente.registerOnChange(alCambiar);

    componente.writeValue('2026-08-01');
    componente.alternar();
    fixture.detectChanges();

    celdaDelMes(20).click();

    expect(alCambiar).toHaveBeenCalledWith('2026-08-20');
    expect(componente.abierto()).withContext('se cierra al elegir').toBeFalse();
  });

  it('con hora conserva la parte horaria y no cierra el panel', () => {
    fixture.componentRef.setInput('conHora', true);
    componente.writeValue('2026-08-01T09:30');
    componente.alternar();
    fixture.detectChanges();

    celdaDelMes(20).click();

    expect(componente.valor()).toBe('2026-08-20T09:30');
    expect(componente.abierto()).withContext('sigue abierto para la hora').toBeTrue();
  });

  it('con hora y sin valor previo aplica la hora que muestran los selectores', () => {
    fixture.componentRef.setInput('conHora', true);
    componente.alternar();
    fixture.detectChanges();

    const horaMostrada = `${componente.horaActual()}:${componente.minutoActual()}`;
    celdaDelMes(15).click();

    expect(componente.valor().slice(11)).toBe(horaMostrada);
  });

  it('los selectores de hora marcan la opción correcta', () => {
    fixture.componentRef.setInput('conHora', true);
    componente.writeValue('2026-08-11T18:45');
    componente.alternar();
    fixture.detectChanges();

    const selects = fixture.nativeElement.querySelectorAll('.hora-select');
    expect((selects[0] as HTMLSelectElement).value).toBe('18');
    expect((selects[1] as HTMLSelectElement).value).toBe('45');
  });

  it('cambiar la hora reescribe solo la parte horaria', () => {
    fixture.componentRef.setInput('conHora', true);
    componente.writeValue('2026-08-11T09:30');

    componente.cambiarHora('18');

    expect(componente.valor()).toBe('2026-08-11T18:30');
  });

  it('deshabilita los días fuera del rango de max', () => {
    fixture.componentRef.setInput('max', '2026-08-15');
    componente.writeValue('2026-08-01');
    componente.alternar();
    fixture.detectChanges();

    expect(celdaDelMes(10).disabled).toBeFalse();
    expect(celdaDelMes(20).disabled).toBeTrue();
  });

  it('no cambia el valor al hacer clic en un día deshabilitado', () => {
    fixture.componentRef.setInput('min', '2026-08-10');
    componente.writeValue('2026-08-12');
    componente.alternar();
    fixture.detectChanges();

    celdaDelMes(5).click();

    expect(componente.valor()).toBe('2026-08-12');
  });

  it('navega entre meses sin tocar el valor', () => {
    componente.writeValue('2026-08-11');
    componente.alternar();
    fixture.detectChanges();

    componente.mesSiguiente();
    fixture.detectChanges();
    expect(componente.tituloVista()).toBe('septiembre 2026');

    componente.mesAnterior();
    componente.mesAnterior();
    fixture.detectChanges();
    expect(componente.tituloVista()).toBe('julio 2026');
    expect(componente.valor()).toBe('2026-08-11');
  });

  it('salta de año al retroceder desde enero', () => {
    componente.writeValue('2026-01-15');
    componente.alternar();
    componente.mesAnterior();

    expect(componente.tituloVista()).toBe('diciembre 2025');
  });

  it('limpiar deja el valor vacío y cierra', () => {
    componente.writeValue('2026-08-11');
    componente.alternar();

    componente.limpiar();

    expect(componente.valor()).toBe('');
    expect(componente.abierto()).toBeFalse();
  });

  it('la cuadrícula siempre trae 42 celdas', () => {
    componente.writeValue('2026-02-01');
    componente.alternar();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.dia').length).toBe(42);
  });

  it('incrustado muestra el calendario sin disparador', () => {
    fixture.componentRef.setInput('incrustado', true);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.disparador')).toBeNull();
    expect(fixture.nativeElement.querySelector('.panel')).not.toBeNull();
  });

  it('incrustado no se cierra al elegir día', () => {
    fixture.componentRef.setInput('incrustado', true);
    componente.writeValue('2026-08-01');
    fixture.detectChanges();

    celdaDelMes(20).click();

    expect(componente.valor()).toBe('2026-08-20');
    expect(componente.panelVisible()).toBeTrue();
  });

  it('deshabilitado no abre el panel', () => {
    componente.setDisabledState(true);
    componente.alternar();

    expect(componente.abierto()).toBeFalse();
  });
});
