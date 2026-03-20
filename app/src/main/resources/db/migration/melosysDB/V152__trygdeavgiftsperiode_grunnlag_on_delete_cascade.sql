-- MELOSYS-7588: ON DELETE CASCADE på trygdeavgiftsperiode_id.
-- Når trygdeavgiftsperiode slettes (orphan removal), sletter databasen
-- automatisk tilhørende grunnlag-rader.
--
-- NB: inntektsperiode_id og skatteforhold_id har IKKE ON DELETE CASCADE.
-- Disse radene eies av Trygdeavgiftsperiode (cascade=ALL) og slettes etter
-- at grunnlag allerede er borte (via ON DELETE CASCADE ovenfor).

ALTER TABLE trygdeavgiftsperiode_grunnlag DROP CONSTRAINT fk_tag_trygdeavgiftsperiode;
ALTER TABLE trygdeavgiftsperiode_grunnlag ADD CONSTRAINT fk_tag_trygdeavgiftsperiode
    FOREIGN KEY (trygdeavgiftsperiode_id) REFERENCES trygdeavgiftsperiode (id) ON DELETE CASCADE;
