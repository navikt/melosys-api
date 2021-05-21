ALTER TABLE fastsatt_trygdeavgift MODIFY betales_av NUMBER(19);
ALTER TABLE fastsatt_trygdeavgift
    ADD CONSTRAINT fk_fastsatt_trygdeavgift_betales_av_aktoer FOREIGN KEY (betales_av) REFERENCES aktoer;