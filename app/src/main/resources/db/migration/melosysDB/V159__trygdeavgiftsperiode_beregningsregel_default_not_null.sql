-- MELOSYS-7969: Lukk NULL-hullet på trygdeavgiftsperiode.beregningsregel
--
-- V156 la til kolonnen UTEN DB-default og UTEN NOT NULL, og backfillet kun rader
-- som fantes da migrasjonen kjørte. Under rullerende deploy av feature/7588 fortsatte
-- den gamle poden (uten beregningsregel i Hibernate-mappingen) å sette inn
-- trygdeavgiftsperiode-rader; disse INSERT-ene utelot kolonnen og fikk beregningsregel = NULL.
--
-- Kotlin-entiteten deklarerer beregningsregel som non-null. Når en slik NULL-rad lastes,
-- settes feltet til null via Hibernate-feltinjeksjon (omgår Kotlins null-sjekk), og
-- Trygdeavgiftsperiode.copyEntity(...) kaster NullPointerException på default-parameteret.
--
-- 1) Backfill gjenstående NULL-rader.
-- 2) Sett DB-default + NOT NULL slik at fremtidige INSERT som utelater kolonnen
--    (f.eks. gamle poder ved senere rullerende deploy) får 'ORDINÆR' i stedet for NULL,
--    og slik at DB-invarianten matcher den non-null Kotlin-typen.

UPDATE trygdeavgiftsperiode SET beregningsregel = 'ORDINÆR' WHERE beregningsregel IS NULL;

ALTER TABLE trygdeavgiftsperiode MODIFY (beregningsregel DEFAULT 'ORDINÆR' NOT NULL);
