import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { EspacioFormComponent } from './espacio-form.component';
import { EspaciosPublicitariosService } from '../../services/espacios-publicitarios.service';
import { UploadService } from '../../../../core/services/upload.service';

describe('EspacioFormComponent', () => {
  let component: EspacioFormComponent;
  let fixture: ComponentFixture<EspacioFormComponent>;
  let espaciosServiceSpy: jasmine.SpyObj<EspaciosPublicitariosService>;
  let uploadServiceSpy: jasmine.SpyObj<UploadService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const activatedRouteStub = {
    snapshot: {
      paramMap: {
        get: (key: string) => (key === 'patrocinioId' ? '2' : null)
      }
    }
  };

  beforeEach(async () => {
    espaciosServiceSpy = jasmine.createSpyObj('EspaciosPublicitariosService', ['crear']);
    uploadServiceSpy = jasmine.createSpyObj('UploadService', ['subirImagen']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [EspacioFormComponent, ReactiveFormsModule],
      providers: [
        { provide: EspaciosPublicitariosService, useValue: espaciosServiceSpy },
        { provide: UploadService, useValue: uploadServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: activatedRouteStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EspacioFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function archivoFalso(nombre: string, tamanoBytes: number, tipo = 'image/png'): File {
    const contenido = new Array(tamanoBytes).fill('a').join('');
    return new File([contenido], nombre, { type: tipo });
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('el formulario es invalido cuando esta vacio', () => {
    expect(component.form.invalid).toBeTrue();
  });

  it('rechaza un archivo mayor a 2MB sin llamar al servicio de subida', () => {
    const archivoPesado = archivoFalso('grande.png', 3 * 1024 * 1024);
    const event = { target: { files: [archivoPesado], value: '' } } as unknown as Event;

    component.onArchivoSeleccionado(event);

    expect(uploadServiceSpy.subirImagen).not.toHaveBeenCalled();
    expect(component.error()).toContain('2MB');
  });

  it('sube la imagen y actualiza el formulario cuando el archivo es valido', () => {
    uploadServiceSpy.subirImagen.and.returnValue(of({ url: 'https://ejemplo.com/subida.png' }));
    const archivoValido = archivoFalso('banner.png', 1024);
    const event = { target: { files: [archivoValido], value: '' } } as unknown as Event;

    component.onArchivoSeleccionado(event);

    expect(uploadServiceSpy.subirImagen).toHaveBeenCalledWith(archivoValido);
    expect(component.form.controls.imagenUrl.value).toBe('https://ejemplo.com/subida.png');
    expect(component.previewUrl()).toBe('https://ejemplo.com/subida.png');
    expect(component.subiendoImagen()).toBeFalse();
  });

  it('muestra un error si falla la subida de la imagen', () => {
    uploadServiceSpy.subirImagen.and.returnValue(throwError(() => new Error('fallo')));
    const archivoValido = archivoFalso('banner.png', 1024);
    const event = { target: { files: [archivoValido], value: '' } } as unknown as Event;

    component.onArchivoSeleccionado(event);

    expect(component.error()).toBe('No se pudo subir la imagen. Intenta de nuevo.');
    expect(component.subiendoImagen()).toBeFalse();
  });

  it('no llama al servicio de crear si el formulario es invalido', () => {
    component.guardar();
    expect(espaciosServiceSpy.crear).not.toHaveBeenCalled();
  });

  it('llama al servicio de crear con el patrocinioId de la ruta y navega al guardar', () => {
    espaciosServiceSpy.crear.and.returnValue(of({} as any));

    component.form.patchValue({
      ubicacion: 'TORNEO_CABECERA',
      imagenUrl: 'https://ejemplo.com/banner.png',
      enlaceUrl: 'https://nike.com'
    });

    component.guardar();

    expect(espaciosServiceSpy.crear).toHaveBeenCalledWith({
      patrocinioId: 2,
      ubicacion: 'TORNEO_CABECERA',
      imagenUrl: 'https://ejemplo.com/banner.png',
      enlaceUrl: 'https://nike.com'
    });
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/sponsorships/asociaciones', 2, 'espacios']);
  });

  it('muestra un mensaje de error si el servicio de crear falla', () => {
    espaciosServiceSpy.crear.and.returnValue(throwError(() => ({ error: { message: 'Error de prueba' } })));

    component.form.patchValue({
      ubicacion: 'TORNEO_CABECERA',
      imagenUrl: 'https://ejemplo.com/banner.png'
    });

    component.guardar();

    expect(component.error()).toBe('Error de prueba');
    expect(component.guardando()).toBeFalse();
  });
});
