ALTER TABLE trygdeavgiftsperiode
ADD lovvalgsperiode_id NUMBER(19);

ALTER TABLE trygdeavgiftsperiode
ADD CONSTRAINT fk_trygdeavgiftsperiode_lovvalgsperiode FOREIGN KEY (lovvalgsperiode_id) REFERENCES lovvalgsperiode;
