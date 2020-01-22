CREATE TABLE utpekingsperiode (
    id                      NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    behandling_id           NUMBER(19)     NOT NULL,
    fom_dato                DATE           NOT NULL,
    tom_dato                DATE           NOT NULL,
    lovvalgsland            VARCHAR2(99)   NULL,
    lovvalgsbestemmelse     VARCHAR2(99)   NULL,
    tilleggsbestemmelse     VARCHAR2(99)   NULL,
    CONSTRAINT pk_utpekingsperiode PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_utpeking_unik_i_beh ON utpekingsperiode(behandling_id, fom_dato, tom_dato);
CREATE INDEX idx_utpeking_resultat ON utpekingsperiode(behandling_id);

ALTER TABLE utpekingsperiode
    ADD CONSTRAINT fk_utpeking_behandling FOREIGN KEY (behandling_id) REFERENCES behandling;

ALTER TABLE utpekingsperiode
    ADD CONSTRAINT utpeking_unik_i_beh UNIQUE(behandling_id, fom_dato, tom_dato) using index idx_utpeking_unik_i_beh;