CREATE TABLE anmodningsperiode (
    id                      NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    beh_resultat_id         NUMBER(19)     NOT NULL,
    fom_dato                DATE           NOT NULL,
    tom_dato                DATE           NOT NULL,
    lovvalgsland            VARCHAR2(99)   NULL,
    lovvalg_bestemmelse     VARCHAR2(99)   NULL,
    tillegg_bestemmelse     VARCHAR(99)    NULL,
    unntak_fra_lovvalgsland VARCHAR(99)    NULL,
    unntak_fra_bestemmelse  VARCHAR(99)    NULL,
    medlperiode_id          NUMBER(19)     NULL,
    anmodningsperiode_type  VARCHAR(10)    NULL,
    svar_id                 NUMBER(19)     NULL,
    CONSTRAINT pk_anmodningsperiode PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_anmodning_unik_i_beh ON anmodningsperiode(beh_resultat_id, fom_dato, tom_dato);
CREATE INDEX idx_anmodning_resultat ON anmodningsperiode(beh_resultat_id);

ALTER TABLE anmodningsperiode
    ADD CONSTRAINT fk_anmodning_beh_resultat FOREIGN KEY (beh_resultat_id) REFERENCES behandlingsresultat;

ALTER TABLE anmodningsperiode
    ADD CONSTRAINT anmodning_unik_i_beh UNIQUE(beh_resultat_id, fom_dato, tom_dato) using index idx_anmodning_unik_i_beh;