// Fordeler IT-klassene i integrasjonstest på shards etter målt tid, for plan-jobben i
// pull-request.yml. Fordelingen er LPT, som shard-plan.mjs i melosys-e2e-tests: lengste
// klasse først, til shardet med minst estimert tid.
//
// Bruk: node it-shard-plan.mjs <kildemappe> <tider.json eller -> <antall shards>
// Planen skrives som JSON på stdout: { shards: [{ shard, klasser, sekunder }], ... }
//
// Tidene er it-timings.json fra samlejobben i en tidligere kjøring. En klasse uten tid får
// medianen av de kjente. Den første klassen i hver JVM bærer oppstarten av Oracle og den
// første Spring-konteksten (ca. 100 s). Med tider fra en kjøring med like mange shards har
// hvert shard én slik klasse, og LPT legger de største i hvert sitt shard.

import { appendFileSync, readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';
import { finnItKlasser } from './it-rapport.mjs';

const STANDARD_SEKUNDER = 30;

function median(verdier) {
  if (verdier.length === 0) return STANDARD_SEKUNDER;
  const sortert = [...verdier].sort((a, b) => a - b);
  const midt = Math.floor(sortert.length / 2);
  return sortert.length % 2 ? sortert[midt] : (sortert[midt - 1] + sortert[midt]) / 2;
}

const etterNavn = (a, b) => (a < b ? -1 : a > b ? 1 : 0);

// Samme inndata gir samme plan: like lange klasser sorteres på navn.
export function planlegg(klasser, tider, ønsketAntall) {
  const kjente = klasser.filter((klasse) => Number.isFinite(tider[klasse]));
  const reserve = median(kjente.map((klasse) => tider[klasse]));
  const antall = Math.max(1, Math.min(Math.floor(Number(ønsketAntall)) || 1, klasser.length));
  const shards = Array.from({ length: antall }, (_, i) => ({ shard: i + 1, klasser: [], sekunder: 0 }));

  const ordnet = klasser
    .map((klasse) => ({ klasse, sekunder: Number.isFinite(tider[klasse]) ? tider[klasse] : reserve }))
    .sort((a, b) => b.sekunder - a.sekunder || etterNavn(a.klasse, b.klasse));
  for (const { klasse, sekunder } of ordnet) {
    const mål = shards.reduce((min, s) => (s.sekunder < min.sekunder ? s : min));
    mål.klasser.push(klasse);
    mål.sekunder += sekunder;
  }

  return {
    klasser: klasser.length,
    kjente: kjente.length,
    shards: shards.map((s) => ({ ...s, klasser: s.klasser.sort(etterNavn), sekunder: Math.round(s.sekunder) })),
  };
}

function oppsummering(plan) {
  return [
    '## Fordeling av IT-klasser',
    '',
    `${plan.klasser} klasser, ${plan.kjente} med målt tid. Estimert testtid per shard:`,
    '',
    '| Shard | Klasser | Sekunder |',
    '|---|---|---|',
    ...plan.shards.map((s) => `| ${s.shard} | ${s.klasser.length} | ${s.sekunder} |`),
    '',
  ].join('\n');
}

function main([kildemappe, tidsfil, antall]) {
  if (!kildemappe || !tidsfil || !antall) {
    console.error('Bruk: node it-shard-plan.mjs <kildemappe> <tider.json eller -> <antall shards>');
    return 2;
  }
  const klasser = finnItKlasser(kildemappe);
  if (klasser.length === 0) {
    console.error(`::error::Fant ingen IT-klasser i ${kildemappe}.`);
    return 1;
  }
  const tider = tidsfil === '-' ? {} : JSON.parse(readFileSync(tidsfil, 'utf8'));
  if (tidsfil === '-') console.error('::warning::Fant ingen tider fra en tidligere kjøring; alle klasser får samme estimat.');
  const plan = planlegg(klasser, tider, antall);
  if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY, oppsummering(plan));
  console.log(JSON.stringify(plan));
  return 0;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  process.exitCode = main(process.argv.slice(2));
}
