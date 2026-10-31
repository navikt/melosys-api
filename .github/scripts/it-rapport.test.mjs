import { strict as assert } from 'node:assert';
import { mkdirSync, mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';
import { finnItKlasser, lesRapport, lesShards, vurder } from './it-rapport.mjs';

const rapport = (navn, tid) =>
  `<?xml version="1.0" encoding="UTF-8"?>\n<testsuite xmlns:xsi="x" version="3.0.2" name="${navn}" time="${tid}" tests="1">\n</testsuite>\n`;

const skriv = (rot, sti, innhold) => {
  const full = join(rot, sti);
  mkdirSync(join(full, '..'), { recursive: true });
  writeFileSync(full, innhold);
};

test('finner IT-klasser i Kotlin og Java, også med æøå, og ikke baseklasser', () => {
  const rot = mkdtempSync(join(tmpdir(), 'it-kilde-'));
  skriv(rot, 'kotlin/no/nav/ÅrsavregningIT.kt', '');
  skriv(rot, 'java/no/nav/GammelIT.java', '');
  skriv(rot, 'kotlin/no/nav/ComponentTestBase.kt', '');
  skriv(rot, 'kotlin/no/nav/KafkaSerializationTest.kt', '');
  assert.deepEqual(finnItKlasser(rot), ['GammelIT', 'ÅrsavregningIT']);
});

test('klassenavn fra filsystem med NFD (macOS) og rapport med NFC blir like', () => {
  const rot = mkdtempSync(join(tmpdir(), 'it-kilde-'));
  skriv(rot, 'ÅpenIT.kt'.normalize('NFD'), '');
  const [klasse] = finnItKlasser(rot);
  assert.equal(klasse, lesRapport(rapport('no.nav.ÅpenIT'.normalize('NFC'), '1')).klasse);
});

test('leser klassenavn og tid, og regner nøstede klasser til ytre klasse', () => {
  assert.deepEqual(lesRapport(rapport('no.nav.melosys.itest.SedMottakTestIT', '18.59')), {
    klasse: 'SedMottakTestIT',
    sekunder: 18.59,
  });
  assert.deepEqual(lesRapport(rapport('no.nav.FooIT$Nøstet', '1,234.5')), { klasse: 'FooIT', sekunder: 1234.5 });
  assert.equal(lesRapport('<testsuite tests="0">'), null);
});

test('summerer tider per shard og vurderer mangler og dobbeltkjøring', () => {
  const rot = mkdtempSync(join(tmpdir(), 'it-rapporter-'));
  skriv(rot, 'it-reports-shard-1/TEST-no.nav.AIT.xml', rapport('no.nav.AIT', '10'));
  skriv(rot, 'it-reports-shard-1/TEST-no.nav.AIT$Indre.xml', rapport('no.nav.AIT$Indre', '2.5'));
  skriv(rot, 'it-reports-shard-1/annet.txt', 'ikke en rapport');
  skriv(rot, 'it-reports-shard-2/TEST-no.nav.BIT.xml', rapport('no.nav.BIT', '5'));
  skriv(rot, 'it-reports-shard-2/TEST-no.nav.AIT.xml', rapport('no.nav.AIT', '11'));

  const shards = lesShards(rot);
  assert.deepEqual(
    shards.map((s) => [s.shard, Object.fromEntries(s.tider)]),
    [
      ['it-reports-shard-1', { AIT: 12.5 }],
      ['it-reports-shard-2', { BIT: 5, AIT: 11 }],
    ],
  );

  const { tider, mangler, doble } = vurder(['AIT', 'BIT', 'CIT'], shards);
  assert.deepEqual(tider, { AIT: 23.5, BIT: 5 });
  assert.deepEqual(mangler, ['CIT']);
  assert.deepEqual(doble, [{ klasse: 'AIT', shards: ['it-reports-shard-1', 'it-reports-shard-2'] }]);
});

test('alle klasser i nøyaktig ett shard gir ingen funn', () => {
  const rot = mkdtempSync(join(tmpdir(), 'it-rapporter-'));
  skriv(rot, 'it-reports-shard-1/TEST-no.nav.AIT.xml', rapport('no.nav.AIT', '3'));
  const { mangler, doble } = vurder(['AIT'], lesShards(rot));
  assert.deepEqual([mangler, doble], [[], []]);
});
