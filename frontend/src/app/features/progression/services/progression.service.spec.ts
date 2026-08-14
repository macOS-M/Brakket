import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { ProgressionService, Progresion } from './progression.service';

describe('ProgressionService', () => {
  let service:ProgressionService;
  let http:HttpTestingController;
  const respuesta:Progresion={puntos:100,logros:[],elementos:[]};
  beforeEach(() => {
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});
    service=TestBed.inject(ProgressionService); http=TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('consulta la progresión actual', () => {
    service.get().subscribe(r => expect(r).toEqual(respuesta));
    const req=http.expectOne(`${environment.apiUrl}/progression`);
    expect(req.request.method).toBe('GET'); req.flush(respuesta);
  });

  it('aplica un cosmético canjeado', () => {
    service.apply(12).subscribe();
    const req=http.expectOne(`${environment.apiUrl}/progression/apply/12`);
    expect(req.request.method).toBe('PUT'); expect(req.request.body).toEqual({}); req.flush(respuesta);
  });

  it('quita el cosmético sin eliminar su propiedad', () => {
    service.remove(12).subscribe(r => expect(r).toEqual(respuesta));
    const req=http.expectOne(`${environment.apiUrl}/progression/apply/12`);
    expect(req.request.method).toBe('DELETE'); req.flush(respuesta);
  });
});
