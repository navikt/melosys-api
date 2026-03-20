-- MELOSYS-7588: Legg til ON DELETE CASCADE på FK til trygdeavgiftsperiode.
-- Når en trygdeavgiftsperiode slettes (orphan removal fra medlemskapsperiode),
-- skal databasen automatisk slette tilhørende grunnlag-rader.
-- Uten dette prøver Hibernate å UPDATE grunnlag-rader med inntektsperiode_id=NULL
-- før sletting, som bryter NOT NULL constraint (ORA-01407).

ALTER TABLE trygdeavgiftsperiode_grunnlag DROP CONSTRAINT fk_tag_trygdeavgiftsperiode;

ALTER TABLE trygdeavgiftsperiode_grunnlag ADD CONSTRAINT fk_tag_trygdeavgiftsperiode
    FOREIGN KEY (trygdeavgiftsperiode_id) REFERENCES trygdeavgiftsperiode (id) ON DELETE CASCADE;
