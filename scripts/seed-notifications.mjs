#!/usr/bin/env node
/**
 * Genera notificaciones de prueba para RF-45 directamente en PostgreSQL.
 *
 * Uso:
 *   node scripts/seed-notifications.mjs
 *   node scripts/seed-notifications.mjs --correo cap2.demo@brakket.gg
 *   node scripts/seed-notifications.mjs --correo usuario@correo.com --limpiar
 *
 * Sin --correo utiliza cap1.demo@brakket.gg, creado por seed-demo.mjs.
 * Es idempotente: antes de insertar elimina únicamente los registros creados
 * por este script para el destinatario seleccionado.
 */

import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ORIGEN_SEED = 'Generador RF-45';
const CORREO_PREDETERMINADO = 'cap1.demo@brakket.gg';
const raiz = join(dirname(fileURLToPath(import.meta.url)), '..');

function leerEnv() {
  try {
    const vars = {};
    for (const linea of readFileSync(join(raiz, '.env'), 'utf8').split(/\r?\n/)) {
      const match = linea.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/);
      if (match && !linea.trim().startsWith('#')) vars[match[1]] = match[2];
    }
    return vars;
  } catch {
    return {};
  }
}

function argumento(nombre) {
  const indice = process.argv.indexOf(nombre);
  return indice >= 0 ? process.argv[indice + 1] : undefined;
}

function sqlLiteral(valor) {
  return `'${valor.replaceAll("'", "''")}'`;
}

function ejecutarSql(sql) {
  const env = leerEnv();
  const usuario = env.POSTGRES_USER || 'brakket';
  const base = env.POSTGRES_DB || 'brakket';
  return execFileSync(
    'docker',
    ['exec', 'brakket-db', 'psql', '-U', usuario, '-d', base, '-v', 'ON_ERROR_STOP=1', '-c', sql],
    { encoding: 'utf8' }
  );
}

const env = leerEnv();
if ((env.SPRING_PROFILES_ACTIVE || 'dev').toLowerCase().includes('prod')) {
  console.error('ABORTADO: este generador solo se puede ejecutar en desarrollo.');
  process.exit(1);
}

const correo = argumento('--correo') || CORREO_PREDETERMINADO;
if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) {
  console.error(`Correo inválido: ${correo}`);
  process.exit(1);
}

const correoSql = sqlLiteral(correo);
const origenSql = sqlLiteral(ORIGEN_SEED);
const soloLimpiar = process.argv.includes('--limpiar');

try {
  const existe = ejecutarSql(
    `SELECT count(*) FROM usuario WHERE correo = ${correoSql};`
  );
  if (!/\n\s*1\s*\n/.test(existe)) {
    console.error(`No existe el usuario ${correo}. Crea la cuenta o ejecuta primero: node scripts/seed-demo.mjs`);
    process.exit(1);
  }

  ejecutarSql(`
    DELETE FROM notificacion
    WHERE usuario_id = (SELECT id FROM usuario WHERE correo = ${correoSql})
      AND origen = ${origenSql};
  `);

  if (soloLimpiar) {
    console.log(`Notificaciones RF-45 de ${correo} eliminadas.`);
    process.exit(0);
  }

  ejecutarSql(`
    INSERT INTO notificacion
      (usuario_id, tipo, mensaje, entidad, entidad_id, leida, fecha,
       estado_entrega, origen, eliminada_bandeja)
    SELECT u.id, datos.tipo, datos.mensaje, datos.entidad, datos.entidad_id,
           datos.leida, now() - datos.antiguedad, datos.estado_entrega,
           ${origenSql}, false
    FROM usuario u
    CROSS JOIN (VALUES
      ('INVITACION_EQUIPO',
       'Fénix Esports te invitó a formar parte de su plantilla como titular.',
       'InvitacionEquipo', 45001::bigint, false, interval '4 minutes', 'DISPONIBLE'),
      ('TRANSFERENCIA_SOLICITADA',
       'Se solicitó tu transferencia de Lobos Demo a Titanes Demo.',
       'solicitud_transferencia', 45002::bigint, false, interval '18 minutes', 'DISPONIBLE'),
      ('RESULTADO',
       'El resultado de la partida Fénix Demo 3–1 Cuervos Demo fue confirmado.',
       'partida', 45003::bigint, false, interval '2 hours', 'ENTREGADA'),
      ('DISPUTA',
       'Se abrió una disputa sobre la partida #45003. Tienes 24 horas para aportar evidencia.',
       'disputa', 45004::bigint, false, interval '5 hours', 'PENDIENTE'),
      ('CAMBIO_TORNEO',
       'La final de Copa Doble Órbita cambió al viernes a las 20:00.',
       'torneo', 45005::bigint, true, interval '1 day', 'ENTREGADA'),
      ('TRANSMISION',
       'La transmisión oficial de Copa Relámpago está en vivo.',
       'transmision', 45006::bigint, false, interval '2 days', 'DISPONIBLE'),
      ('ADMINISTRATIVA',
       'Actualizamos las reglas de conducta competitiva de la plataforma.',
       'accion_administrativa', 45007::bigint, true, interval '4 days', 'ENTREGADA'),
      ('EXPULSION_EQUIPO',
       'Fuiste removido del equipo Lobos Demo. Causa: inactividad prolongada.',
       'MiembroEquipo', 45008::bigint, true, interval '7 days', 'ENTREGADA'),
      ('CORRECCION',
       'Corrección: la final anunciada anteriormente fue reprogramada, no cancelada.',
       'torneo', 45009::bigint, false, interval '8 days', 'CORREGIDA')
    ) AS datos(tipo, mensaje, entidad, entidad_id, leida, antiguedad, estado_entrega)
    WHERE u.correo = ${correoSql};
  `);

  console.log(`9 notificaciones de prueba creadas para ${correo}.`);
  console.log('Abre http://localhost:4200/notifications con ese usuario.');
  console.log(`Para retirarlas: node scripts/seed-notifications.mjs --correo ${correo} --limpiar`);
} catch (error) {
  console.error('No fue posible generar las notificaciones.');
  console.error('Verifica que PostgreSQL esté activo con: docker compose up -d db');
  console.error(error.message);
  process.exit(1);
}
