-- MELOSYS-7588: Indekser FK-kolonnene i trygdeavgiftsperiode_grunnlag.
--
-- Oracle indekserer ikke FK-kolonner automatisk. Uten indeks tar DELETE/UPDATE på
-- parent-tabellene share-lock på hele grunnlag-tabellen og gjør full table scan
-- for constraint-validering. V154 la kun til indeks for trygdeavgiftsperiode_id;
-- her dekker vi de fem resterende FK-ene. Konvensjon speiler V62 for
-- trygdeavgiftsperiode-tabellen.

CREATE INDEX idx_tag_medlemskapsperiode ON trygdeavgiftsperiode_grunnlag (medlemskapsperiode_id);
CREATE INDEX idx_tag_lovvalgsperiode    ON trygdeavgiftsperiode_grunnlag (lovvalgsperiode_id);
CREATE INDEX idx_tag_helseutgift        ON trygdeavgiftsperiode_grunnlag (helseutgift_dekkes_periode_id);
CREATE INDEX idx_tag_inntektsperiode    ON trygdeavgiftsperiode_grunnlag (inntektsperiode_id);
CREATE INDEX idx_tag_skatteforhold      ON trygdeavgiftsperiode_grunnlag (skatteforhold_id);
