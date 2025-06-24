CREATE TABLE helseutgift_dekkes_periode (
    id                      NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    beh_resultat_id         NUMBER(19)     NOT NULL,
    fom_dato                DATE           NOT NULL,
    tom_dato                DATE           NOT NULL,
    bosted_landkode         VARCHAR2(3)    NOT NULL,
    CONSTRAINT pk_helseutgift_dekkes_periode PRIMARY KEY (id),
    CONSTRAINT fk_helseutgift_dekkes_periode_beh_resultat FOREIGN KEY (beh_resultat_id) REFERENCES behandlingsresultat
);

CREATE INDEX idx_dekkes_periode_resultat ON helseutgift_dekkes_periode(beh_resultat_id);

