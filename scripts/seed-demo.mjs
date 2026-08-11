#!/usr/bin/env node
/**
 * Seed de datos de DEMO para la presentación (dev-only).
 *
 * Crea, VÍA API (para pasar por todas las reglas de negocio reales):
 *   - 10 cuentas demo (admin, organizadora y 8 capitanes) con contraseña Demo2026!
 *   - 2 ligas -> 2 temporadas -> 3 torneos 1v1 con los mismos 8 jugadores
 *     participando en dos juegos distintos:
 *       * Copa Relampago (Demo): ELIMINACION_DIRECTA, FINALIZADA con campeón.
 *       * Masters Horizonte (Demo): ELIMINACION_DIRECTA, FINALIZADA.
 *       * Copa Doble Orbita (Demo): DOBLE_ELIMINACION, EN CURSO con SOLO la
 *         gran final reportada y sin confirmar — alimenta el aviso de
 *         resultados no definitivos de RF-47.
 *   - 1 invitación de equipo pendiente (para que el dashboard muestre acciones).
 *   - 2 patrocinadores demo.
 *   - (best-effort) canal de Twitch validado + transmisión asociada al torneo B.
 *
 * El único paso fuera de la API es otorgar ADMIN a admin.demo@brakket.gg
 * (INSERT en usuario_rol vía docker exec): el bootstrap de ADMIN real solo
 * aplica al login con Google.
 *
 * Uso:
 *   node scripts/seed-demo.mjs               # reset + seed (idempotente)
 *   node scripts/seed-demo.mjs --solo-reset  # solo limpiar los datos demo
 *
 * Guardas: aborta si SPRING_PROFILES_ACTIVE=prod en el .env o si el backend
 * no responde en localhost:8080. Solo borra entidades con nombres/correos
 * demo explícitos: jamás toca datos reales.
 */

import { readFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const API = 'http://localhost:8080/api';
const PASSWORD = 'Demo2026!';

const USUARIOS = {
  admin: { nombre: 'Adriana Admin (Demo)', correo: 'admin.demo@brakket.gg' },
  orga: { nombre: 'Olivia Organizadora (Demo)', correo: 'orga.demo@brakket.gg' },
  cap1: { nombre: 'Ana Fenix (Demo)', correo: 'cap1.demo@brakket.gg' },
  cap2: { nombre: 'Bruno Lobo (Demo)', correo: 'cap2.demo@brakket.gg' },
  cap3: { nombre: 'Carla Cuervo (Demo)', correo: 'cap3.demo@brakket.gg' },
  cap4: { nombre: 'Diego Titan (Demo)', correo: 'cap4.demo@brakket.gg' },
  cap5: { nombre: 'Elena Kraken (Demo)', correo: 'cap5.demo@brakket.gg' },
  cap6: { nombre: 'Fabian Nova (Demo)', correo: 'cap6.demo@brakket.gg' },
  cap7: { nombre: 'Gabriela Raptor (Demo)', correo: 'cap7.demo@brakket.gg' },
  cap8: { nombre: 'Hector Vortex (Demo)', correo: 'cap8.demo@brakket.gg' }
};

const EQUIPOS = [
  { clave: 'cap1', nombre: 'Fenix Demo', gamertag: 'AnaFenix#11' },
  { clave: 'cap2', nombre: 'Lobos Demo', gamertag: 'BrunoLobo#22' },
  { clave: 'cap3', nombre: 'Cuervos Demo', gamertag: 'CarlaCuervo#33' },
  { clave: 'cap4', nombre: 'Titanes Demo', gamertag: 'DiegoTitan#44' },
  { clave: 'cap5', nombre: 'Kraken Demo', gamertag: 'ElenaKraken#55' },
  { clave: 'cap6', nombre: 'Nova Demo', gamertag: 'FabianNova#66' },
  { clave: 'cap7', nombre: 'Raptors Demo', gamertag: 'GabrielaRaptor#77' },
  { clave: 'cap8', nombre: 'Vortex Demo', gamertag: 'HectorVortex#88' }
];

const TORNEO_A = 'Copa Relampago (Demo)';
const TORNEO_B = 'Copa Doble Orbita (Demo)';
const TORNEO_C = 'Masters Horizonte (Demo)';
const LIGA = 'Liga Demo Brakket';
const LIGA_SECUNDARIA = 'Liga Multijuego Demo Brakket';
const PATROCINADORES = ['Cafe Volcan (Demo)', 'Nebula Energy (Demo)'];
const MARCADORES = [[3, 1], [3, 2], [2, 0], [3, 0], [2, 1], [1, 3], [0, 2]];

const raiz = join(dirname(fileURLToPath(import.meta.url)), '..');
let marcadorIdx = 0;

// ---------- utilidades ----------

function leerEnv() {
  try {
    const texto = readFileSync(join(raiz, '.env'), 'utf8');
    const vars = {};
    for (const linea of texto.split(/\r?\n/)) {
      const m = linea.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/);
      if (m && !linea.trim().startsWith('#')) vars[m[1]] = m[2];
    }
    return vars;
  } catch {
    return {};
  }
}

function psql(sql) {
  const env = leerEnv();
  const user = env.POSTGRES_USER || 'brakket';
  const db = env.POSTGRES_DB || 'brakket';
  return execFileSync('docker', ['exec', 'brakket-db', 'psql', '-U', user, '-d', db,
    '-v', 'ON_ERROR_STOP=1', '-c', sql], { encoding: 'utf8' });
}

async function api(metodo, path, { token, body } = {}) {
  const res = await fetch(`${API}${path}`, {
    method: metodo,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const texto = await res.text();
  if (!res.ok) {
    throw new Error(`${metodo} ${path} -> ${res.status}: ${texto.slice(0, 300)}`);
  }
  return texto ? JSON.parse(texto) : null;
}

/** Fecha local (la JVM del backend corre America/Costa_Rica, igual que esta máquina). */
function fechaLocal(horasFuturo, soloFecha = false) {
  const d = new Date(Date.now() + horasFuturo * 3_600_000);
  const p = (n) => String(n).padStart(2, '0');
  const fecha = `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
  return soloFecha ? fecha : `${fecha}T${p(d.getHours())}:${p(d.getMinutes())}:00`;
}

function marcador() {
  const [a, b] = MARCADORES[marcadorIdx++ % MARCADORES.length];
  return { marcadorA: a, marcadorB: b };
}

function log(msg) {
  console.log(`  ${msg}`);
}

// ---------- guardas ----------

async function verificarEntorno() {
  const env = leerEnv();
  if ((env.SPRING_PROFILES_ACTIVE || 'dev').toLowerCase().includes('prod')) {
    console.error('ABORTADO: el .env tiene SPRING_PROFILES_ACTIVE=prod. Este seed es SOLO para desarrollo.');
    process.exit(1);
  }
  try {
    const salud = await fetch('http://localhost:8080/actuator/health').then((r) => r.json());
    if (salud.status !== 'UP') throw new Error(salud.status);
  } catch (e) {
    console.error(`ABORTADO: el backend no responde en localhost:8080 (${e.message}). Levantalo con: docker compose up -d`);
    process.exit(1);
  }
}

// ---------- reset ----------

function reset() {
  console.log('\n== Reset de datos demo ==');
  // Solo entidades con nombres/correos demo explícitos. Los ON DELETE CASCADE
  // del esquema (V1/V34) arrastran partidas, inscripciones, membresías,
  // invitaciones, métricas y transmisiones asociadas.
  psql(`
    DELETE FROM torneo WHERE nombre IN ('${TORNEO_A}', '${TORNEO_B}', '${TORNEO_C}');
    DELETE FROM temporada WHERE liga_id IN (SELECT id FROM liga WHERE nombre IN ('${LIGA}', '${LIGA_SECUNDARIA}'));
    DELETE FROM liga WHERE nombre IN ('${LIGA}', '${LIGA_SECUNDARIA}');
    DELETE FROM equipo WHERE nombre IN (${EQUIPOS.map((e) => `'${e.nombre}'`).join(', ')});
    -- Limpia también equipos de versiones anteriores del seed cuyos nombres
    -- cambiaron, pero únicamente si pertenecen a las cuentas demo.
    DELETE FROM equipo
    WHERE capitan_id IN (SELECT id FROM usuario WHERE correo LIKE '%.demo@brakket.gg');
    DELETE FROM patrocinador WHERE nombre IN (${PATROCINADORES.map((p) => `'${p}'`).join(', ')});
    DELETE FROM usuario WHERE correo LIKE '%.demo@brakket.gg';
  `);
  log('Datos demo anteriores eliminados (los datos reales no se tocan).');
}

// ---------- seed ----------

async function registrar(usuario) {
  try {
    const r = await api('POST', '/auth/registro', {
      body: { nombre: usuario.nombre, correo: usuario.correo, password: PASSWORD }
    });
    return r.token;
  } catch {
    // Ya existía (reset parcial): entrar con la contraseña demo.
    const r = await api('POST', '/auth/login', {
      body: { correo: usuario.correo, password: PASSWORD }
    });
    return r.token;
  }
}

/**
 * Resuelve (como organizador) partidas pendientes con ambos rivales definidos,
 * hasta dejar exactamente `dejarPendientes` partidas sin cerrar en el torneo.
 * El avance de llave y la finalización del torneo los hace el motor real.
 */
async function resolverHastaDejar(torneoId, tokenOrganizador, dejarPendientes) {
  for (let paso = 0; paso < 64; paso++) {
    const bracket = await api('GET', `/tournaments/${torneoId}/bracket`, { token: tokenOrganizador });
    const abiertas = bracket.filter((p) => p.estado !== 'FINALIZADA' && p.estado !== 'CANCELADA');
    if (abiertas.length <= dejarPendientes) return abiertas;
    const jugable = abiertas.find((p) => p.estado === 'PENDIENTE' && p.equipoAId && p.equipoBId && !p.bye);
    if (!jugable) {
      throw new Error(`Torneo ${torneoId}: quedan ${abiertas.length} partidas abiertas pero ninguna jugable.`);
    }
    await api('POST', `/tournaments/partidas/${jugable.id}/resolucion`, {
      token: tokenOrganizador, body: marcador()
    });
    log(`Partida ${jugable.id} (${jugable.equipoANombre} vs ${jugable.equipoBNombre}) resuelta.`);
  }
  throw new Error('Demasiadas iteraciones resolviendo partidas: revisar el bracket.');
}

async function seed() {
  console.log('\n== Usuarios demo ==');
  const tokens = {};
  const ids = {};
  for (const [clave, usuario] of Object.entries(USUARIOS)) {
    tokens[clave] = await registrar(usuario);
    ids[clave] = (await api('GET', '/me', { token: tokens[clave] })).id;
    log(`${usuario.correo} (id ${ids[clave]})`);
  }

  // ADMIN por SQL: el bootstrap real de ADMIN solo aplica al login con Google.
  psql(`INSERT INTO usuario_rol (usuario_id, rol_id)
        SELECT u.id, r.id FROM usuario u, rol r
        WHERE u.correo = '${USUARIOS.admin.correo}' AND r.nombre_rol = 'ADMIN'
        ON CONFLICT DO NOTHING;`);
  log('Rol ADMIN otorgado a admin.demo@brakket.gg');

  console.log('\n== Juegos, ligas y temporadas ==');
  const juegos = await api('GET', '/games');
  if (juegos.length < 2) throw new Error('Se necesitan al menos dos juegos activos para el seed multijuego.');
  const juego = juegos.find((j) => j.nombre.includes('Rocket League')) ?? juegos[0];
  const juegoSecundario = juegos.find((j) => j.id !== juego.id && /Valorant|Counter|Fortnite/i.test(j.nombre))
    ?? juegos.find((j) => j.id !== juego.id);
  log(`Juego principal: ${juego.nombre} (id ${juego.id})`);
  log(`Juego secundario: ${juegoSecundario.nombre} (id ${juegoSecundario.id})`);

  const liga = await api('POST', '/leagues', {
    token: tokens.orga,
    body: { nombre: LIGA, juegoId: juego.id, descripcion: 'Liga oficial de demostracion de Brakket.' }
  });
  log(`Liga: ${liga.nombre} (id ${liga.id})`);

  const formatos = await api('GET', `/leagues/${liga.id}/season-formats`, { token: tokens.orga });
  const formato = formatos.find((f) => String(f.nombre ?? f.codigo ?? '').toUpperCase().includes('ELIMINACION')) ?? formatos[0];
  const temporada = await api('POST', `/leagues/${liga.id}/seasons`, {
    token: tokens.orga,
    body: {
      nombre: 'Temporada Demo 2026',
      fechaInicio: fechaLocal(0, true),
      fechaFin: fechaLocal(24 * 90, true),
      reglas: 'Temporada de demostracion: torneos 1v1 con resultados reportados por capitanes.',
      estado: 'ACTIVA',
      cupoEquipos: 16,
      formatoId: formato.id
    }
  });
  log(`Temporada: ${temporada.nombre} (id ${temporada.id})`);

  const ligaSecundaria = await api('POST', '/leagues', {
    token: tokens.orga,
    body: { nombre: LIGA_SECUNDARIA, juegoId: juegoSecundario.id, descripcion: 'Liga demo para validar historiales multijuego.' }
  });
  const formatosSecundarios = await api('GET', `/leagues/${ligaSecundaria.id}/season-formats`, { token: tokens.orga });
  const formatoSecundario = formatosSecundarios.find((f) => String(f.nombre ?? f.codigo ?? '').toUpperCase().includes('ELIMINACION'))
    ?? formatosSecundarios[0];
  const temporadaSecundaria = await api('POST', `/leagues/${ligaSecundaria.id}/seasons`, {
    token: tokens.orga,
    body: {
      nombre: 'Temporada Multijuego Demo 2026', fechaInicio: fechaLocal(0, true),
      fechaFin: fechaLocal(24 * 90, true), reglas: 'Temporada secundaria para historial multijuego.',
      estado: 'ACTIVA', cupoEquipos: 16, formatoId: formatoSecundario.id
    }
  });
  log(`Liga secundaria: ${ligaSecundaria.nombre} (id ${ligaSecundaria.id})`);

  console.log('\n== Equipos (8 equipos de 1 jugador) ==');
  const equipoIds = {};
  for (const e of EQUIPOS) {
    const equipo = await api('POST', '/teams', {
      token: tokens[e.clave],
      body: {
        nombre: e.nombre, juegoId: juego.id, juegoIds: [juego.id, juegoSecundario.id],
        descripcion: `Equipo multijuego demo capitaneado por ${USUARIOS[e.clave].nombre}.`
      }
    });
    equipoIds[e.clave] = equipo.id;
    log(`${e.nombre} (id ${equipo.id})`);
  }

  // Invitación pendiente: alimenta la sección "pendientes" del dashboard de la organizadora.
  try {
    await api('POST', `/teams/${equipoIds.cap1}/invitaciones`, {
      token: tokens.cap1,
      body: { jugadorId: ids.orga, rolPropuesto: 'SUPLENTE', mensaje: 'Sumate como suplente para la demo.' }
    });
    log('Invitación pendiente: Fenix Demo -> Olivia Organizadora');
  } catch (e) {
    log(`Invitación auxiliar omitida: ${e.message.slice(0, 120)}`);
  }

  async function crearTorneo(nombre, formatoTorneo, horas, premio, contexto = {}) {
    const juegoTorneo = contexto.juego ?? juego;
    const temporadaTorneo = contexto.temporada ?? temporada;
    const equiposTorneo = contexto.equipos ?? EQUIPOS;
    const idsTorneo = contexto.equipoIds ?? equipoIds;
    const t = await api('POST', '/tournaments', {
      token: tokens.orga,
      body: {
        nombre, juegoId: juegoTorneo.id, temporadaId: temporadaTorneo.id, formato: formatoTorneo,
        tamanoEquipo: 1, maxEquipos: 8, fechaInicio: fechaLocal(horas), publico: true,
        descripcion: 'Torneo de demostracion 1v1.', premio,
        ajustesPartida: [{ clave: 'Modalidad', valor: 'Duelo 1v1' }, { clave: 'Mejor de', valor: '5' }]
      }
    });
    for (const e of equiposTorneo) {
      await api('POST', `/tournaments/${t.id}/inscripciones`, {
        token: tokens[e.clave], body: { equipoId: idsTorneo[e.clave], usuarioEnJuego: e.gamertag }
      });
    }
    await api('POST', `/tournaments/${t.id}/iniciar`, { token: tokens.orga });
    log(`${nombre} (id ${t.id}, ${juegoTorneo.nombre}): ${equiposTorneo.length} equipos inscritos, bracket generado.`);
    return t;
  }

  console.log('\n== Torneo A: finalizado ==');
  const torneoA = await crearTorneo(TORNEO_A, 'ELIMINACION_DIRECTA', 2, 'Trofeo Relampago');
  await resolverHastaDejar(torneoA.id, tokens.orga, 0);
  const detalleA = await api('GET', `/tournaments/${torneoA.id}`, { token: tokens.orga });
  log(`Estado final: ${detalleA.torneo?.estado ?? detalleA.estado} — campeón listo para mostrarse.`);

  console.log('\n== Torneo C: segundo historial finalizado ==');
  const torneoC = await crearTorneo(TORNEO_C, 'ELIMINACION_DIRECTA', 3, 'Trofeo Horizonte', {
    juego: juegoSecundario, temporada: temporadaSecundaria,
    equipos: EQUIPOS, equipoIds
  });
  await resolverHastaDejar(torneoC.id, tokens.orga, 0);
  const detalleC = await api('GET', `/tournaments/${torneoC.id}`, { token: tokens.orga });
  log(`Estado final: ${detalleC.torneo?.estado ?? detalleC.estado} — segundo punto de progresión listo.`);

  console.log('\n== Torneo B: en curso, gran final reportada ==');
  const torneoB = await crearTorneo(TORNEO_B, 'DOBLE_ELIMINACION', 4, 'Trofeo Orbita');
  const abiertas = await resolverHastaDejar(torneoB.id, tokens.orga, 1);
  const granFinal = abiertas[0];
  const entradaCapitan = Object.entries(equipoIds)
    .find(([, equipoId]) => equipoId === granFinal.equipoAId);
  if (!entradaCapitan) throw new Error('No se encontró el capitán del equipo A de la gran final.');
  await api('POST', `/tournaments/partidas/${granFinal.id}/reporte`, {
    token: tokens[entradaCapitan[0]], body: marcador()
  });
  log(`Gran final reportada y sin confirmar: ${granFinal.equipoANombre} vs ${granFinal.equipoBNombre} (partida ${granFinal.id}).`);

  console.log('\n== Verificación RF-47 ==');
  const statsEquipo = await api('GET', `/statistics?equipoId=${granFinal.equipoAId}`, {
    token: tokens[entradaCapitan[0]]
  });
  const resumen = statsEquipo.juegos?.[0];
  log(`${granFinal.equipoANombre}: ${resumen?.partidas ?? 0} partidas oficiales, ${resumen?.torneos ?? 0} torneos.`);
  log(`${statsEquipo.resultadosNoDefinitivos ?? 0} resultado(s) no definitivo(s) identificado(s).`);

  console.log('\n== Patrocinadores demo ==');
  for (const nombre of PATROCINADORES) {
    await api('POST', '/sponsors', {
      token: tokens.admin,
      body: {
        nombre, logo: null, contacto: `contacto@${nombre.split(' ')[0].toLowerCase()}.cr`,
        descripcion: 'Patrocinador de demostracion.', enlaces: [], confirmarDuplicado: false
      }
    });
    log(nombre);
  }

  console.log('\n== Twitch (best-effort) ==');
  try {
    const canal = await api('GET', '/twitch', { token: tokens.admin });
    if (!canal.activo) {
      const env = leerEnv();
      await api('POST', '/twitch/canal', {
        token: tokens.admin, body: { canal: env.TWITCH_CHANNEL || 'brakketcenfotec' }
      });
      await api('POST', '/twitch/validar', { token: tokens.admin });
    }
    const transmision = await api('POST', '/twitch/transmisiones', {
      token: tokens.admin, body: { torneoId: torneoB.id, partidaId: null }
    });
    log(`Transmisión ${transmision.id} asociada al torneo B (métricas RF-36 muestrearán si el canal sale en vivo).`);
  } catch (e) {
    log(`Omitido (no bloquea la demo): ${e.message.slice(0, 160)}`);
  }

  console.log(`
============================================================
 SEED COMPLETO. Credenciales (contraseña única: ${PASSWORD})
------------------------------------------------------------
 ADMIN         admin.demo@brakket.gg   (panel /admin, /twitch, patrocinios)
 Organizadora  orga.demo@brakket.gg    (liga, temporada, torneos, resolver)
 Capitanes     cap1..cap8.demo@brakket.gg (8 equipos disponibles)
------------------------------------------------------------
 Torneo A FINALIZADO:  ${TORNEO_A} (id ${torneoA.id})
 Torneo C FINALIZADO:  ${TORNEO_C} (id ${torneoC.id})
 Torneo B EN CURSO:    ${TORNEO_B} (id ${torneoB.id}) — gran final reportada
 Historial multijuego: ${juego.nombre} + ${juegoSecundario.nombre}
 Guía completa: docs/GUIA-DEMO-E2E.md
============================================================`);
}

// ---------- main ----------

const soloReset = process.argv.includes('--solo-reset');
await verificarEntorno();
reset();
if (!soloReset) {
  await seed();
} else {
  console.log('\nListo: solo reset.');
}
