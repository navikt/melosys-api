-- MELOSYS-8150: backfill av er_fjernarbeid_twfa fra prosessinstans.data. Kolonnen ble lagt til i V170.
-- Radene i prosessinstans finnes fortsatt fordi ingen slettejobb er implementert (verifisert august 2026),
-- men de er eneste kilde til historikken.
--
-- Skilt fra V170 med vilje. ALTER-en er en dictionary-oppdatering; denne skanner prosessinstans.data.
-- Oracle stoetter ikke DDL i transaksjon, saa Flyway skriver en success=0-rad hvis denne kaster, og
-- appen starter ikke igjen foer noen kjoerer flyway repair. Splitten fjerner altsaa IKKE den fastlaaste
-- utrullingen -- den gjoer opprydningen enkel: kolonnen er allerede paa plass og registrert, saa
-- gjenopprettingen er aa kjoere setningene under manuelt i en kontrollert sesjon og deretter repare.
-- Uten splitten ville en re-kjoering ogsaa truffet ALTER-en paa nytt (ORA-01430).
--
-- Joinen p.behandling_id = ap.beh_resultat_id ser ut som en nokkelforveksling, men er riktig:
-- behandlingsresultat har behandling_id som PK (V1.0_06, @MapsId i Behandlingsresultat.kt), og
-- anmodningsperiode.beh_resultat_id peker paa den (fk_anmodning_beh_resultat i V4.3_02).
--
-- De to setningene er bevisst gjensidig utelukkende slik at resultatet er uavhengig av rekkefoelgen,
-- og slik at true vinner over false. Det defensive vaktet er ikke for gamle rader: flagget kunne foerst
-- settes 2026-02-22 (#3231), saa hver rad backfillen kan treffe er nyere enn anmodet_av (2021). Veien
-- til to anmodningsprosesser med ulikt flagg gaar i stedet gjennom at lagreAnmodningsperioder sletter og
-- gjenoppretter radene og mister anmodet_av underveis, slik at sperren i registrerAnmodning kan omgaas.
--
-- er_fjernarbeid_twfa IS NULL gjoer kjoeringen strengt additiv: rader som ny kode allerede har fylt
-- roeres ikke. Vakten filtrerer ingenting ved foerstegangskjoeringen -- kolonnen er tom da -- men gjoer
-- at setningene kan kjoeres om igjen etter utrullingen (deploy-gapet) og etter en repair, uten aa
-- overskrive en riktig verdi med en eldre prosessinstans-verdi.
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
