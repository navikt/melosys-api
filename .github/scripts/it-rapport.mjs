// Kontrollerer failsafe-rapportene fra alle IT-shards og lager it-timings.json, som
// fordelingen av klasser på shards leser i neste kjøring.
//
// Bruk: node it-rapport.mjs <kildemappe> <rapportmappe> <tider.json>
//   kildemappe:   integrasjonstest/src/test; hver fil som heter *IT.kt eller *IT.java er en IT-klasse
//   rapportmappe: én undermappe per shard-artefakt, med failsafe-rapportene (TEST-*.xml)
//
// Feiler når en IT-klasse mangler i rapportene eller finnes i mer enn ett shard: da har
// fordelingen hoppet over eller doblet en klasse, og en grønn sjekk ville ikke vært sann.

import { appendFileSync, readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { basename, join } from 'node:path';
import { pathToFileURL } from 'node:url';

const filer = (mappe) =>
  readdirSync(mappe, { withFileTypes: true }).flatMap((oppføring) => {
    const sti = join(mappe, oppføring.name);
    return oppføring.isDirectory() ? filer(sti) : [sti];
  });

export function finnItKlasser(kildemappe) {
  return filer(kildemappe)
    .map((sti) => basename(sti).normalize('NFC').match(/^([\p{L}\p{N}_]+IT)\.(kt|java)$/u)?.[1])
    .filter(Boolean)
    .sort();
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

export function lesShards(rapportmappe) {
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
  const shards = lesShards(rapportmappe);
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
  return resultat.mangler.length || resultat.doble.length ? 1 : 0;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  process.exitCode = main(process.argv.slice(2));
}
