export type TipoNotificacion =
  | 'INVITACION'
  | 'INVITACION_EQUIPO'
  | 'INVITACION_ACEPTADA'
  | 'INVITACION_RECHAZADA'
  | 'SOLICITUD_UNION'
  | 'SOLICITUD_ACEPTADA'
  | 'SOLICITUD_RECHAZADA'
  | 'TRANSFERENCIA'
  | 'TRANSFERENCIA_SOLICITADA'
  | 'TRANSFERENCIA_ACTUALIZADA'
  | 'TRANSFERENCIA_APROBADA'
  | 'TRANSFERENCIA_RECHAZADA'
  | 'RESULTADO'
  | 'DISPUTA'
  | 'CAMBIO_TORNEO'
  | 'TRANSMISION'
  | 'ADMINISTRATIVA'
  | 'EXPULSION_EQUIPO'
  | 'CORRECCION';

export interface Notificacion {
  id: number;
  tipo: TipoNotificacion;
  mensaje: string;
  origen: string;
  entidad: string;
  entidadId: number;
  leida: boolean;
  fecha: string;
  estadoEntrega: 'DISPONIBLE' | 'ENTREGADA' | 'PENDIENTE' | 'CORREGIDA';
}
