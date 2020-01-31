CREATE TABLE utpekingsperiode (
    id                      NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    beh_resultat_id         NUMBER(19)     NOT NULL,
    fom_dato                DATE           NOT NULL,
    tom_dato                DATE           NOT NULL,
    lovvalgsland            VARCHAR2(99)   NULL,
    lovvalgsbestemmelse     VARCHAR2(99)   NULL,
    tilleggsbestemmelse     VARCHAR2(99)   NULL,
    medlperiode_id          NUMBER(19)     NULL,
    sendt_utland            DATE           NULL,
    CONSTRAINT pk_utpekingsperiode PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_utpeking_unik_i_beh_resultat ON utpekingsperiode(beh_resultat_id, fom_dato, tom_dato);
CREATE INDEX idx_utpeking_beh_resultat ON utpekingsperiode(beh_resultat_id);

ALTER TABLE utpekingsperiode
    ADD CONSTRAINT fk_utpeking_beh_resultat FOREIGN KEY (beh_resultat_id) REFERENCES behandlingsresultat;

ALTER TABLE utpekingsperiode
    ADD CONSTRAINT utpeking_unik_i_beh_resultat UNIQUE(beh_resultat_id, fom_dato, tom_dato) using index idx_utpeking_unik_i_beh_resultat;