CREATE TABLE aarsavregning
(
    behandlingsresultat_id     NUMBER(19) NOT NULL,
    tidligere_behandling_id    NUMBER(19) NOT NULL,
    aar                        NUMBER(4),
    tidligere_fakturert_beloep DECIMAL(12, 2),
    fastsatt_totalbeloep       DECIMAL(12, 2),
    til_fakturering_beloep     DECIMAL(12, 2) DEFAULT 0,
    CONSTRAINT pk_aarsavregning PRIMARY KEY (behandlingsresultat_id)
);

ALTER TABLE aarsavregning
    ADD CONSTRAINT fk_aarsavregning_behandlingsresultat FOREIGN KEY (behandlingsresultat_id) REFERENCES behandlingsresultat;
CREATE INDEX idx_beh_resultat_id ON aarsavregning (behandlingsresultat_id);

ALTER TABLE aarsavregning
    ADD CONSTRAINT fk_aarsavregning_tidl_beh FOREIGN KEY (tidligere_behandling_id) REFERENCES behandling;
CREATE INDEX idx_tidl_beh_id ON aarsavregning (tidligere_behandling_id);
