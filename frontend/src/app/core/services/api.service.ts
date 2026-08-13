import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

/**
 * Servicio generico de acceso a la API REST de Brakket.
 * Centraliza la URL base. La autenticacion se resuelve mediante el
 * jwtInterceptor, que agrega la cabecera Authorization en cada peticion.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  get<T>(path: string): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}${path}`);
  }

  post<T>(path: string, body: unknown): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}${path}`, body);
  }

  put<T>(path: string, body: unknown): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}${path}`, body);
  }

  patch<T>(path: string, body: unknown): Observable<T> {
    return this.http.patch<T>(`${this.baseUrl}${path}`, body);
  }

  delete<T>(path: string): Observable<T> {
    return this.http.delete<T>(`${this.baseUrl}${path}`);
  }

  // RF-50: descarga de archivos binarios (PDF de reportes). Va aparte de
  // get<T> porque responseType: 'blob' no es compatible con el tipado JSON.
  getBlob(path: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}${path}`, { responseType: 'blob' });
  }
}
