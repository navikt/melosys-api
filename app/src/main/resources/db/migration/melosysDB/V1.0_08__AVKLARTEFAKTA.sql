CREATE TABLE avklartefakta (
    id                            NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    beh_resultat_id               NUMBER(19) NOT NULL,
    arbeidsgiver_forretningsland  VARCHAR2(99) NULL,
    mottar_kontantytelse          NUMBER(1) NULL,
    kontantytelse_type            VARCHAR2(99) NULL,
    offentlig_tjenestemann        NUMBER(1) NULL,
    bostedsland                   VARCHAR2(99) NULL,
    sokkel_skip                   VARCHAR2(99) NULL,
    CONSTRAINT pk_avklartefakta PRIMARY KEY (id)
);

ALTER TABLE avklartefakta
    ADD CONSTRAINT fk_avklartefakta_beh_resultat FOREIGN KEY (beh_resultat_id) REFERENCES behandlingsresultat;