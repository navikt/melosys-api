CREATE TABLE aarsavregning
(
    behandlingsresultat_id     NUMBER(19) NOT NULL,
    aar                        NUMBER(4) NOT NULL,
    tidligere_resultat_id      NUMBER(19),
    tidligere_fakturert_beloep DECIMAL(12, 2),
    nytt_totalbeloep           DECIMAL(12, 2),
    til_fakturering_beloep     DECIMAL(12, 2) DEFAULT 0,
    CONSTRAINT pk_aarsavregning PRIMARY KEY (behandlingsresultat_id)
);

ALTER TABLE aarsavregning
    ADD CONSTRAINT fk_aarsavregning_behandlingsresultat FOREIGN KEY (behandlingsresultat_id) REFERENCES behandlingsresultat;

ALTER TABLE aarsavregning
    ADD CONSTRAINT fk_aarsavregning_tidl_reultat FOREIGN KEY (tidligere_resultat_id) REFERENCES behandlingsresultat;
CREATE INDEX idx_tidl_resultat_id ON aarsavregning (tidligere_resultat_id);
