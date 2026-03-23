-- MELOSYS-7588: ON DELETE CASCADE på trygdeavgiftsperiode_id.
-- Når trygdeavgiftsperiode slettes (orphan removal), sletter databasen
-- automatisk tilhørende grunnlag-rader.
--
-- NB: inntektsperiode_id og skatteforhold_id har IKKE ON DELETE CASCADE.
-- Disse har cascade=PERSIST+MERGE (ikke REMOVE) i JPA for å unngå ORA-02292.
-- Orphaned rader i inntektsperiode/skatteforhold_til_norge ryddes opp manuelt.

ALTER TABLE trygdeavgiftsperiode_grunnlag DROP CONSTRAINT fk_tag_trygdeavgiftsperiode;
ALTER TABLE trygdeavgiftsperiode_grunnlag ADD CONSTRAINT fk_tag_trygdeavgiftsperiode
    FOREIGN KEY (trygdeavgiftsperiode_id) REFERENCES trygdeavgiftsperiode (id) ON DELETE CASCADE;

-- ON DELETE SET NULL på avgiftspliktig-periode FK-er.
-- Hibernate kan slette Medlemskapsperiode/Lovvalgsperiode/HelseutgiftDekkesPeriode
-- før grunnlag-raden. SET NULL lar slettingen gå gjennom; grunnlag-raden
-- ryddes opp etterpå via ON DELETE CASCADE på trygdeavgiftsperiode_id.

ALTER TABLE trygdeavgiftsperiode_grunnlag DROP CONSTRAINT fk_tag_medlemskapsperiode;
ALTER TABLE trygdeavgiftsperiode_grunnlag ADD CONSTRAINT fk_tag_medlemskapsperiode
    FOREIGN KEY (medlemskapsperiode_id) REFERENCES medlemskapsperiode (id) ON DELETE SET NULL;

ALTER TABLE trygdeavgiftsperiode_grunnlag DROP CONSTRAINT fk_tag_lovvalgsperiode;
ALTER TABLE trygdeavgiftsperiode_grunnlag ADD CONSTRAINT fk_tag_lovvalgsperiode
    FOREIGN KEY (lovvalgsperiode_id) REFERENCES lovvalg_periode (id) ON DELETE SET NULL;

ALTER TABLE trygdeavgiftsperiode_grunnlag DROP CONSTRAINT fk_tag_helseutgift;
ALTER TABLE trygdeavgiftsperiode_grunnlag ADD CONSTRAINT fk_tag_helseutgift
    FOREIGN KEY (helseutgift_dekkes_periode_id) REFERENCES helseutgift_dekkes_periode (id) ON DELETE SET NULL;
