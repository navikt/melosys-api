-- MELOSYS-7588: Legg til ON DELETE CASCADE på alle FK-er i grunnlag-tabellen.
-- Grunnlag-rader er rene referanser — de skal slettes automatisk når noen av
-- parent-radene slettes. Uten dette oppstår FK-feil (ORA-01407/ORA-02292) fordi
-- Hibernate ikke garanterer riktig sletterekkefølge når Inntektsperiode og
-- Trygdeavgiftsperiode cascade-slettes samtidig.

ALTER TABLE trygdeavgiftsperiode_grunnlag DROP CONSTRAINT fk_tag_trygdeavgiftsperiode;
ALTER TABLE trygdeavgiftsperiode_grunnlag ADD CONSTRAINT fk_tag_trygdeavgiftsperiode
    FOREIGN KEY (trygdeavgiftsperiode_id) REFERENCES trygdeavgiftsperiode (id) ON DELETE CASCADE;

ALTER TABLE trygdeavgiftsperiode_grunnlag DROP CONSTRAINT fk_tag_inntektsperiode;
ALTER TABLE trygdeavgiftsperiode_grunnlag ADD CONSTRAINT fk_tag_inntektsperiode
    FOREIGN KEY (inntektsperiode_id) REFERENCES inntektsperiode (id) ON DELETE CASCADE;

ALTER TABLE trygdeavgiftsperiode_grunnlag DROP CONSTRAINT fk_tag_skatteforhold;
ALTER TABLE trygdeavgiftsperiode_grunnlag ADD CONSTRAINT fk_tag_skatteforhold
    FOREIGN KEY (skatteforhold_id) REFERENCES skatteforhold_til_norge (id) ON DELETE CASCADE;
