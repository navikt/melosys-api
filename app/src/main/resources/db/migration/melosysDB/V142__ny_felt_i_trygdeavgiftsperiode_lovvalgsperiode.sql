ALTER TABLE trygdeavgiftsperiode
ADD lovvalg_periode_id NUMBER(19);

ALTER TABLE trygdeavgiftsperiode
ADD CONSTRAINT fk_trygdeavgiftsperiode_lovvalg_periode FOREIGN KEY (lovvalg_periode_id) REFERENCES lovvalg_periode;
