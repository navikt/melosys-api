-- MELOSYS-8150: flytt TWFA-flagget (rammeavtale om fjernarbeid) fra prosessinstans.data til en durabel kolonne.
-- prosessinstans er en arbeidstabell som er ment aa slettes loepende (V161__prosessinstans_prioritet.sql),
-- og var frem til naa eneste lagringssted for saksbehandlerens avhuking. Presedens for kolonnen:
-- V7.6_07__saksbehandler_anmodet_om_unntak.sql la til anmodet_av paa samme tabell for tilsvarende formaal.
--
-- Tri-state bevares: NULL = ikke besvart (flagget kunne foerst settes 2026-02-22), 0 = nei, 1 = ja.
-- Bevisst UTEN DEFAULT, i motsetning til sendt_utland NUMBER(1) DEFAULT 0 i V4.4_03 paa samme tabell:
-- en DEFAULT 0 ville lest alle historiske rader som et registrert nei og oedelagt tri-staten som baade
-- uttrekket (WHERE = 1) og EessiService sin null-sjekk bygger paa. Presedens for nullbar form: V122.
ALTER TABLE anmodningsperiode ADD er_fjernarbeid_twfa NUMBER(1);

-- Backfill fra prosessinstans.data. Radene finnes fortsatt fordi ingen slettejobb er implementert
-- (verifisert august 2026), men de er eneste kilde til historikken.
--
-- Joinen p.behandling_id = ap.beh_resultat_id ser ut som en nokkelforveksling, men er riktig:
-- behandlingsresultat har behandling_id som PK (V1.0_06, @MapsId i Behandlingsresultat.kt), og
-- anmodningsperiode.beh_resultat_id peker paa den (fk_anmodning_beh_resultat i V4.3_02).
--
-- De to setningene er bevisst gjensidig utelukkende slik at resultatet er uavhengig av rekkefoelgen,
-- og slik at true vinner over false. I praksis kan en behandling knapt ha to anmodningsprosesser med
-- ulikt flagg -- registrerAnmodning kaster naar anmodet_av allerede er satt -- saa vaktet er defensivt
-- og treffer i hovedsak rader fra foer V7.6_07 innfoerte anmodet_av.
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
