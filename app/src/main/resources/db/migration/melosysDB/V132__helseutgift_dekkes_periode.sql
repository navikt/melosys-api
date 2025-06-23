CREATE TABLE helseutgift_dekkes_periode (
    id                      NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    beh_resultat_id         NUMBER(19)     NOT NULL,
    fom_dato                DATE           NOT NULL,
    tom_dato                DATE           NOT NULL,
    bostedsland             VARCHAR2(99)   NULL,
    CONSTRAINT pk_helseutgift_dekkes_periode PRIMARY KEY (id),
    CONSTRAINT fk_helseutgift_dekkes_periode_beh_resultat FOREIGN KEY (beh_resultat_id) REFERENCES behandlingsresultat
);
