import { strict as assert } from 'node:assert';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';
import { planlegg } from './it-shard-plan.mjs';

const skript = fileURLToPath(new URL('./it-shard-plan.mjs', import.meta.url));

const alle = (plan) => plan.shards.flatMap((s) => s.klasser).sort();

test('lengste klasse først, til shardet med minst tid', () => {
  const plan = planlegg(['AIT', 'BIT', 'CIT', 'DIT', 'EIT'], { AIT: 120, BIT: 60, CIT: 50, DIT: 40, EIT: 30 }, 2);
  assert.deepEqual(
    plan.shards.map((s) => [s.klasser, s.sekunder]),
    [
      [['AIT', 'EIT'], 150],
      [['BIT', 'CIT', 'DIT'], 150],
    ],
  );
});

test('hver klasse havner i nøyaktig ett shard', () => {
  const klasser = Array.from({ length: 48 }, (_, i) => `K${String(i).padStart(2, '0')}IT`);
  const tider = Object.fromEntries(klasser.map((k, i) => [k, (i * 37) % 61]));
  const plan = planlegg(klasser, tider, 3);
  assert.equal(plan.shards.length, 3);
  assert.deepEqual(alle(plan), [...klasser].sort());
});

test('klasse uten målt tid får medianen av de kjente', () => {
  const plan = planlegg(['AIT', 'BIT', 'CIT', 'NyIT'], { AIT: 10, BIT: 20, CIT: 90 }, 1);
  assert.equal(plan.shards[0].sekunder, 10 + 20 + 90 + 20);
  assert.equal(plan.kjente, 3);
});

test('uten tider får alle samme estimat og fordeles jevnt etter antall', () => {
  const plan = planlegg(['AIT', 'BIT', 'CIT', 'DIT', 'EIT', 'FIT'], {}, 3);
  assert.deepEqual(
    plan.shards.map((s) => s.klasser.length),
    [2, 2, 2],
  );
});

test('aldri flere shards enn klasser, og minst ett', () => {
  assert.equal(planlegg(['AIT', 'BIT'], {}, 3).shards.length, 2);
  assert.equal(planlegg(['AIT'], {}, 0).shards.length, 1);
  assert.equal(planlegg(['AIT'], {}, 'tull').shards.length, 1);
});

test('samme inndata gir samme plan, uavhengig av rekkefølgen klassene kommer i', () => {
  const tider = { AIT: 5, BIT: 5, CIT: 5, DIT: 5 };
  const a = planlegg(['AIT', 'BIT', 'CIT', 'DIT'], tider, 2);
  const b = planlegg(['DIT', 'CIT', 'BIT', 'AIT'], tider, 2);
  assert.deepEqual(a, b);
});

test('tid 0 (en @Disabled-klasse) regnes som målt, ikke som ukjent', () => {
  const plan = planlegg(['AIT', 'DisabledIT'], { AIT: 100, DisabledIT: 0 }, 1);
  assert.equal(plan.shards[0].sekunder, 100);
});

for (const [navn, innhold] of [
  ['ugyldig JSON', '{ikke json'],
  ['null', 'null'],
  ['en liste', '[1, 2]'],
]) {
  test(`tidsfil med ${navn} gir plan med like estimater og en advarsel, ikke feil`, () => {
    const kilde = mkdtempSync(join(tmpdir(), 'it-kilde-'));
    writeFileSync(join(kilde, 'AIT.kt'), 'class AIT\n');
    writeFileSync(join(kilde, 'BIT.kt'), 'class BIT\n');
    const tidsfil = join(kilde, 'it-timings.json');
    writeFileSync(tidsfil, innhold);
    const kjøring = spawnSync(process.execPath, [skript, kilde, tidsfil, '2'], { encoding: 'utf8', env: { PATH: process.env.PATH } });
    assert.equal(kjøring.status, 0, kjøring.stderr);
    assert.match(kjøring.stderr, /::warning::/);
    assert.deepEqual(JSON.parse(kjøring.stdout).shards.map((s) => s.sekunder), [30, 30]);
  });
}
