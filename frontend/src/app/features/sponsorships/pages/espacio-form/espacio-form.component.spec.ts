import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { EspacioFormComponent } from './espacio-form.component';
import { EspaciosPublicitariosService } from '../../services/espacios-publicitarios.service';
import { PatrociniosService } from '../../services/patrocinios.service';
import { UploadService } from '../../../../core/services/upload.service';
import { Patrocinio } from '../../../../models/patrocinio.model';
import { EspacioPublicitario } from '../../../../models/espacio-publicitario.model';

describe('EspacioFormComponent', () => {
  let component: EspacioFormComponent;
  let fixture: ComponentFixture<EspacioFormComponent>;
  let espaciosServiceSpy: jasmine.SpyObj<EspaciosPublicitariosService>;
  let patrociniosServiceSpy: jasmine.SpyObj<PatrociniosService>;
  let uploadServiceSpy: jasmine.SpyObj<UploadService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const patrocinioDeTorneoMock: Patrocinio = {
    id: 2,
    patrocinadorId: 11,
    patrocinadorNombre: 'Nike Demo',
    ligaId: null,
    temporadaId: null,
    torneoId: 3,
    condiciones: null,
    fechaInicio: '2026-08-05',
    fechaFin: '2026-12-31',
    estado: 'ACTIVO'
  };

  const espacioExistenteMock: EspacioPublicitario = {
    id: 9,
    patrocinioId: 2,
    patrocinadorNombre: 'Nike Demo',
    ubicacion: 'TRANSMISION_INFERIOR',
    imagenUrl: 'https://ejemplo.com/viejo.png',
    enlaceUrl: 'https://nike.com',
    estado: 'ACTIVO',
    fechaInicio: '2026-08-05',
    fechaFin: '2026-12-31'
  };

  // key === 'id' devuelve null por defecto (modo creación); se sobreescribe
  // por test para simular el modo edición.
  let idParamValue: string | null = null;
  const activatedRouteStub = {
    snapshot: {
      paramMap: {
        get: (key: string) => (key === 'patrocinioId' ? '2' : key === 'id' ? idParamValue : null)
      }
    }
  };

  beforeEach(async () => {
    idParamValue = null;
    espaciosServiceSpy = jasmine.createSpyObj('EspaciosPublicitariosService', [
      'crear', 'editar', 'listarPorPatrocinio'
    ]);
    patrociniosServiceSpy = jasmine.createSpyObj('PatrociniosService', ['obtener']);
    patrociniosServiceSpy.obtener.and.returnValue(of(patrocinioDeTorneoMock));
    uploadServiceSpy = jasmine.createSpyObj('UploadService', ['subirImagen']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [EspacioFormComponent, ReactiveFormsModule],
      providers: [
        { provide: EspaciosPublicitariosService, useValue: espaciosServiceSpy },
        { provide: PatrociniosService, useValue: patrociniosServiceSpy },
        { provide: UploadService, useValue: uploadServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: activatedRouteStub }
      ]
    }).compileComponents();
  });

  function crearComponente(): void {
    fixture = TestBed.createComponent(EspacioFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function archivoFalso(nombre: string, tamanoBytes: number, tipo = 'image/png'): File {
    const contenido = new Array(tamanoBytes).fill('a').join('');
    return new File([contenido], nombre, { type: tipo });
  }

  it('should create', () => {
    crearComponente();
    expect(component).toBeTruthy();
  });

  it('el formulario es invalido cuando esta vacio', () => {
    crearComponente();
    expect(component.form.invalid).toBeTrue();
  });

  it('pide el patrocinio de la ruta y filtra las ubicaciones a las de un torneo', () => {
    crearComponente();
    expect(patrociniosServiceSpy.obtener).toHaveBeenCalledWith(2);
    expect(component.ubicaciones()).toEqual(['TORNEO_CABECERA', 'TRANSMISION_INFERIOR']);
  });

  it('un patrocinio de liga solo ofrece LIGA_CABECERA y ajusta el default del form', () => {
    patrociniosServiceSpy.obtener.and.returnValue(of({
      ...patrocinioDeTorneoMock,
      torneoId: null,
      ligaId: 8
    }));

    crearComponente();

    expect(component.ubicaciones()).toEqual(['LIGA_CABECERA']);
    expect(component.form.controls.ubicacion.value).toBe('LIGA_CABECERA');
  });

  it('rechaza un archivo mayor a 2MB sin llamar al servicio de subida', () => {
    crearComponente();
    const archivoPesado = archivoFalso('grande.png', 3 * 1024 * 1024);
    const event = { target: { files: [archivoPesado], value: '' } } as unknown as Event;

    component.onArchivoSeleccionado(event);

    expect(uploadServiceSpy.subirImagen).not.toHaveBeenCalled();
    expect(component.error()).toContain('2MB');
  });

  it('sube la imagen y actualiza el formulario cuando el archivo es valido', () => {
    crearComponente();
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
    crearComponente();
    uploadServiceSpy.subirImagen.and.returnValue(throwError(() => new Error('fallo')));
    const archivoValido = archivoFalso('banner.png', 1024);
    const event = { target: { files: [archivoValido], value: '' } } as unknown as Event;

    component.onArchivoSeleccionado(event);

    expect(component.error()).toBe('No se pudo subir la imagen. Intenta de nuevo.');
    expect(component.subiendoImagen()).toBeFalse();
  });

  it('no llama al servicio de crear si el formulario es invalido', () => {
    crearComponente();
    component.guardar();
    expect(espaciosServiceSpy.crear).not.toHaveBeenCalled();
  });

  it('llama al servicio de crear con el patrocinioId de la ruta y navega al guardar', () => {
    crearComponente();
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
    crearComponente();
    espaciosServiceSpy.crear.and.returnValue(throwError(() => ({ error: { message: 'Error de prueba' } })));

    component.form.patchValue({
      ubicacion: 'TORNEO_CABECERA',
      imagenUrl: 'https://ejemplo.com/banner.png'
    });

    component.guardar();

    expect(component.error()).toBe('Error de prueba');
    expect(component.guardando()).toBeFalse();
  });

  // ---------------------------------------------------------------
  // Modo edición
  // ---------------------------------------------------------------

  it('en modo edicion carga el espacio existente y precarga el formulario', () => {
    idParamValue = '9';
    espaciosServiceSpy.listarPorPatrocinio.and.returnValue(of([espacioExistenteMock]));

    crearComponente();

    expect(component.editando()).toBeTrue();
    expect(component.form.controls.ubicacion.value).toBe('TRANSMISION_INFERIOR');
    expect(component.form.controls.imagenUrl.value).toBe('https://ejemplo.com/viejo.png');
    expect(component.form.controls.enlaceUrl.value).toBe('https://nike.com');
    expect(component.previewUrl()).toBe('https://ejemplo.com/viejo.png');
  });

  it('en modo edicion muestra error si el espacio no aparece en el listado', () => {
    idParamValue = '999';
    espaciosServiceSpy.listarPorPatrocinio.and.returnValue(of([espacioExistenteMock]));

    crearComponente();

    expect(component.error()).toBe('No se encontró el espacio publicitario a editar.');
  });

  it('en modo edicion guardar llama a editar() con el id, no a crear()', () => {
    idParamValue = '9';
    espaciosServiceSpy.listarPorPatrocinio.and.returnValue(of([espacioExistenteMock]));
    espaciosServiceSpy.editar.and.returnValue(of({} as any));

    crearComponente();

    component.form.patchValue({ imagenUrl: 'https://ejemplo.com/nuevo.png' });
    component.guardar();

    expect(espaciosServiceSpy.editar).toHaveBeenCalledWith(9, {
      ubicacion: 'TRANSMISION_INFERIOR',
      imagenUrl: 'https://ejemplo.com/nuevo.png',
      enlaceUrl: 'https://nike.com'
    });
    expect(espaciosServiceSpy.crear).not.toHaveBeenCalled();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/sponsorships/asociaciones', 2, 'espacios']);
  });
});
