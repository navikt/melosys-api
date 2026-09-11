-- MELOSYS-8150: backfill av er_fjernarbeid_twfa fra prosessinstans.data. Kolonnen ble lagt til i V170.
-- Prosessinstans-radene er eneste kilde til historikken. Hvorfor backfillen står for seg, og hva som
-- må gjøres hvis den feiler under utrulling: README.md i service/.../statistikk.
--
-- Joinen p.behandling_id = ap.beh_resultat_id ser ut som en nøkkelforveksling, men er riktig:
-- behandlingsresultat har behandling_id som PK (V1.0_06, @MapsId i Behandlingsresultat.kt), og
-- anmodningsperiode.beh_resultat_id peker på den (fk_anmodning_beh_resultat i V4.3_02).
--
-- De to setningene er bevisst gjensidig utelukkende, slik at resultatet er uavhengig av rekkefølgen og
-- true vinner over false. En behandling kan ha to anmodningsprosesser med ulikt flagg: lagreAnmodningsperioder
-- sletter og gjenoppretter radene og mister anmodet_av underveis, slik at sperren i registrerAnmodning omgås.
--
-- er_fjernarbeid_twfa IS NULL gjør kjøringen strengt additiv: rader ny kode allerede har fylt røres ikke.
-- Vakten filtrerer ingenting ved førstegangskjøringen, men gjør at setningene kan kjøres om igjen senere
-- uten å overskrive en riktig verdi med en eldre prosessinstans-verdi.
UPDATE anmodningsperiode ap
SET er_fjernarbeid_twfa = 0
WHERE ap.er_fjernarbeid_twfa IS NULL
  AND EXISTS (SELECT 1
              FROM prosessinstans p
              WHERE p.behandling_id = ap.beh_resultat_id
                AND p.prosess_type = 'ANMODNING_OM_UNNTAK'
                AND p.data LIKE '%erFjernarbeidTWFA=false%')
  AND NOT EXISTS (SELECT 1
                  FROM prosessinstans p
                  WHERE p.behandling_id = ap.beh_resultat_id
                    AND p.prosess_type = 'ANMODNING_OM_UNNTAK'
                    AND p.data LIKE '%erFjernarbeidTWFA=true%');

UPDATE anmodningsperiode ap
SET er_fjernarbeid_twfa = 1
WHERE ap.er_fjernarbeid_twfa IS NULL
  AND EXISTS (SELECT 1
              FROM prosessinstans p
              WHERE p.behandling_id = ap.beh_resultat_id
                AND p.prosess_type = 'ANMODNING_OM_UNNTAK'
                AND p.data LIKE '%erFjernarbeidTWFA=true%');
