-- MELOSYS-8150: flytt TWFA-flagget (rammeavtale om fjernarbeid) fra prosessinstans.data til en durabel kolonne.
-- prosessinstans er en arbeidstabell som er ment aa slettes loepende (V161__prosessinstans_prioritet.sql),
-- og var frem til naa eneste lagringssted for saksbehandlerens avhuking. Presedens for kolonnen:
-- V7.6_07__saksbehandler_anmodet_om_unntak.sql la til anmodet_av paa samme tabell for tilsvarende formaal.
--
-- Tri-state bevares: NULL = ikke besvart (flagget kunne foerst settes 2026-02-22), 0 = nei, 1 = ja.
ALTER TABLE anmodningsperiode ADD er_fjernarbeid_twfa NUMBER(1);

-- Backfill fra prosessinstans.data. Radene finnes fortsatt fordi ingen slettejobb er implementert
-- (verifisert august 2026), men de er eneste kilde til historikken.
--
-- De to setningene er bevisst gjensidig utelukkende slik at resultatet er uavhengig av rekkefoelgen,
-- og slik at true vinner over false dersom en behandling har flere anmodningsprosesser med ulikt flagg.
UPDATE anmodningsperiode ap
SET er_fjernarbeid_twfa = 0
WHERE EXISTS (SELECT 1
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
WHERE EXISTS (SELECT 1
              FROM prosessinstans p
              WHERE p.behandling_id = ap.beh_resultat_id
                AND p.prosess_type = 'ANMODNING_OM_UNNTAK'
                AND p.data LIKE '%erFjernarbeidTWFA=true%');
