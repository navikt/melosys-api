// Kontrollerer failsafe-rapportene fra alle IT-shards og lager it-timings.json, som
// fordelingen av klasser på shards leser i neste kjøring.
//
// Bruk: node it-rapport.mjs <kildemappe> <rapportmappe> <tider.json>
//   kildemappe:   integrasjonstest/src/test
//   rapportmappe: én undermappe per shard (it-reports-shard-N), med failsafe-rapportene (TEST-*.xml)
//                 og kompilerte-it-klasser.txt, IT-klassene shardet kompilerte
//
// Feiler når en IT-klasse mangler i rapportene eller finnes i mer enn ett shard: da har
// fordelingen hoppet over eller doblet en klasse, og en grønn sjekk ville ikke vært sann.
// Feiler også når et shard kompilerte en IT-klasse som klassesøket i kildekoden ikke fant:
// shardene kjører bare klassene i planen, så en slik klasse ville aldri kjørt.

import { appendFileSync, readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { basename, join } from 'node:path';
import { pathToFileURL } from 'node:url';

const filer = (mappe) =>
  readdirSync(mappe, { withFileTypes: true }).flatMap((oppføring) => {
    const sti = join(mappe, oppføring.name);
    return oppføring.isDirectory() ? filer(sti) : [sti];
  });

// Klassen, ikke filen, avgjør hva failsafe kjører: en Kotlin-fil kan hete noe annet enn klassen.
// Failsafe tar med klasser som heter IT*, *IT eller *ITCase, men ikke abstrakte eller nøstede.
const toppnivåklasse = /^((?:[a-z]+\s+)*)class\s+([\p{L}\p{N}_]+)/gmu;
const erItKlasse = (navn) => /^IT|IT$|ITCase$/u.test(navn);

export function finnItKlasser(kildemappe) {
  const klasser = filer(kildemappe)
    .filter((sti) => /\.(kt|java)$/.test(sti))
    .flatMap((sti) => [...readFileSync(sti, 'utf8').normalize('NFC').matchAll(toppnivåklasse)])
    .filter(([, modifikatorer, navn]) => erItKlasse(navn) && !/\babstract\b/.test(modifikatorer))
    .map(([, , navn]) => navn);
  return [...new Set(klasser)].sort();
}

const attributt = (tagg, navn) => tagg.match(new RegExp(`\\s${navn}="([^"]*)"`))?.[1];

// Surefire skriver én testsuite per fil. Nøstede klasser (Ytre$Indre) regnes til ytre klasse.
export function lesRapport(xml) {
  const tagg = xml.match(/<testsuite\b[^>]*>/)?.[0];
  const navn = tagg && attributt(tagg, 'name');
  if (!navn) return null;
  const klasse = navn.normalize('NFC').split('.').pop().split('$')[0];
  const sekunder = Number.parseFloat((attributt(tagg, 'time') ?? '0').replaceAll(',', ''));
  return { klasse, sekunder: Number.isFinite(sekunder) ? sekunder : 0 };
}

const KOMPILERTE = 'kompilerte-it-klasser.txt';

export function lesKompilerte(rapportmappe) {
  const klasser = new Set();
  for (const sti of filer(rapportmappe)) {
    if (basename(sti) !== KOMPILERTE) continue;
    for (const linje of readFileSync(sti, 'utf8').split('\n')) {
      const klasse = linje.trim().normalize('NFC');
      if (klasse) klasser.add(klasse);
    }
  }
  return [...klasser].sort();
}

export function lesShards(rapportmappe) {
  const løse = readdirSync(rapportmappe).filter((navn) => /^TEST-.*\.xml$/.test(navn));
  if (løse.length) {
    throw new Error(`${løse.length} rapporter ligger rett i ${rapportmappe}, ikke i en shard-mappe.`);
  }
  return readdirSync(rapportmappe, { withFileTypes: true })
    .filter((oppføring) => oppføring.isDirectory())
    .map((oppføring) => {
      const tider = new Map();
      for (const sti of filer(join(rapportmappe, oppføring.name))) {
        if (!/^TEST-.*\.xml$/.test(basename(sti))) continue;
        const rapport = lesRapport(readFileSync(sti, 'utf8'));
        if (rapport) tider.set(rapport.klasse, (tider.get(rapport.klasse) ?? 0) + rapport.sekunder);
      }
      return { shard: oppføring.name, tider };
    })
    .sort((a, b) => a.shard.localeCompare(b.shard));
}

export function vurder(klasser, shards) {
  const funnet = new Map();
  for (const { shard, tider } of shards) {
    for (const klasse of tider.keys()) funnet.set(klasse, [...(funnet.get(klasse) ?? []), shard]);
  }
  const tider = {};
  for (const klasse of klasser) {
    const sekunder = shards.reduce((sum, { tider: t }) => sum + (t.get(klasse) ?? 0), 0);
    if (funnet.has(klasse)) tider[klasse] = Math.round(sekunder * 10) / 10;
  }
  return {
    tider,
    mangler: klasser.filter((klasse) => !funnet.has(klasse)),
    doble: klasser
      .filter((klasse) => (funnet.get(klasse) ?? []).length > 1)
      .map((klasse) => ({ klasse, shards: funnet.get(klasse) })),
  };
}

function oppsummering(klasser, shards, { tider, mangler, doble }) {
  const linjer = [
    '## IT-klasser per shard',
    '',
    '| Shard | Klasser | Sekunder |',
    '|---|---|---|',
    ...shards.map(({ shard, tider: t }) => {
      const sum = [...t.values()].reduce((a, b) => a + b, 0);
      return `| ${shard} | ${t.size} | ${Math.round(sum)} |`;
    }),
    '',
    `${Object.keys(tider).length} av ${klasser.length} IT-klasser har rapport.`,
  ];
  if (mangler.length) linjer.push('', `**Mangler rapport:** ${mangler.join(', ')}`);
  if (doble.length) {
    linjer.push('', `**Kjørt i flere shards:** ${doble.map((d) => `${d.klasse} (${d.shards.join(', ')})`).join(', ')}`);
  }
  return `${linjer.join('\n')}\n`;
}

function main([kildemappe, rapportmappe, utfil]) {
  if (!kildemappe || !rapportmappe || !utfil) {
    console.error('Bruk: node it-rapport.mjs <kildemappe> <rapportmappe> <tider.json>');
    return 2;
  }
  const klasser = finnItKlasser(kildemappe);
  let shards;
  try {
    shards = lesShards(rapportmappe);
  } catch (feil) {
    console.error(`::error::${feil.message}`);
    return 1;
  }
  const resultat = vurder(klasser, shards);
  const tekst = oppsummering(klasser, shards, resultat);
  console.log(tekst);
  if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY, tekst);
  writeFileSync(utfil, `${JSON.stringify(resultat.tider, null, 2)}\n`);

  if (klasser.length === 0) {
    console.error(`::error::Fant ingen IT-klasser i ${kildemappe}.`);
    return 1;
  }
  if (resultat.mangler.length) console.error(`::error::IT-klasser uten rapport: ${resultat.mangler.join(', ')}`);
  if (resultat.doble.length) console.error(`::error::IT-klasser kjørt i flere shards: ${resultat.doble.map((d) => d.klasse).join(', ')}`);
  const kjent = new Set(klasser);
  const ukjente = lesKompilerte(rapportmappe).filter((klasse) => !kjent.has(klasse));
  if (ukjente.length) {
    console.error(
      `::error::Kompilerte IT-klasser som klassesøket i kildekoden ikke fant, og som derfor ikke kjørte: ${ukjente.join(', ')}. ` +
        'Skriv klassedeklarasjonen på en egen linje, eller gi en abstrakt klasse et navn som ikke ender på IT.',
    );
  }
  return resultat.mangler.length || resultat.doble.length || ukjente.length ? 1 : 0;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  process.exitCode = main(process.argv.slice(2));
}
