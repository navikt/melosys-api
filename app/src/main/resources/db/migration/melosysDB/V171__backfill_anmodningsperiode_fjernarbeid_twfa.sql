-- MELOSYS-8150: backfill av er_fjernarbeid_twfa fra prosessinstans.data. Kolonnen ble lagt til i V170.
-- Radene i prosessinstans finnes fortsatt fordi ingen slettejobb er implementert (verifisert august 2026),
-- men de er eneste kilde til historikken.
--
-- Skilt fra V170 med vilje: ALTER-en er ren metadataendring, mens denne skanner prosessinstans.data.
-- Blir kjoeretiden et problem, kan setningene kjoeres manuelt i en kontrollert SQL-sesjon uten
-- oppstartstimeout -- IS NULL-vakten under gjoer at Flyway-kjoeringen etterpaa finner null rader.
--
-- Joinen p.behandling_id = ap.beh_resultat_id ser ut som en nokkelforveksling, men er riktig:
-- behandlingsresultat har behandling_id som PK (V1.0_06, @MapsId i Behandlingsresultat.kt), og
-- anmodningsperiode.beh_resultat_id peker paa den (fk_anmodning_beh_resultat i V4.3_02).
--
-- De to setningene er bevisst gjensidig utelukkende slik at resultatet er uavhengig av rekkefoelgen,
-- og slik at true vinner over false. I praksis kan en behandling knapt ha to anmodningsprosesser med
-- ulikt flagg -- registrerAnmodning kaster naar anmodet_av allerede er satt -- saa vaktet er defensivt
-- og treffer i hovedsak rader fra foer V7.6_07 innfoerte anmodet_av.
--
-- er_fjernarbeid_twfa IS NULL gjoer kjoeringen strengt additiv: rader som ny kode allerede har fylt
-- roeres ikke, saa setningene kan kjoeres om igjen uten aa overskrive noe med en eldre prosessinstans-verdi.
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
