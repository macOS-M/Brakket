import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { ReportsService } from './reports.service';
import { ReporteResponse } from '../../../models/reporte.model';

describe('ReportsService', () => {
  let service: ReportsService;
  let apiSpy: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    apiSpy = jasmine.createSpyObj('ApiService', ['get', 'getBlob']);
    TestBed.configureTestingModule({
      providers: [ReportsService, { provide: ApiService, useValue: apiSpy }]
    });
    service = TestBed.inject(ReportsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('generar() arma la query con los filtros presentes', () => {
    apiSpy.get.and.returnValue(of({} as ReporteResponse));

    service.generar({ tipo: 'PATROCINIO', torneoId: 5, desde: '2026-01-01' }).subscribe();

    expect(apiSpy.get).toHaveBeenCalledWith('/reports?tipo=PATROCINIO&torneoId=5&desde=2026-01-01');
  });

  it('generar() omite filtros nulos o vacíos', () => {
    apiSpy.get.and.returnValue(of({} as ReporteResponse));

    service.generar({ tipo: 'ESTADISTICA' }).subscribe();

    expect(apiSpy.get).toHaveBeenCalledWith('/reports?tipo=ESTADISTICA');
  });

  it('generarPdf() llama a getBlob con la misma query que generar()', () => {
    apiSpy.getBlob.and.returnValue(of(new Blob(['pdf'])));

    service.generarPdf({ tipo: 'AUDIENCIA', patrocinadorId: 3 }).subscribe();

    expect(apiSpy.getBlob).toHaveBeenCalledWith('/reports/pdf?tipo=AUDIENCIA&patrocinadorId=3');
  });
});
