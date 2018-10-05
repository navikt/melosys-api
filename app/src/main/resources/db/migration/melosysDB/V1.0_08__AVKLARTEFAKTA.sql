CREATE TABLE avklartefakta (
    id                            NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    beh_resultat_id               NUMBER(19) NOT NULL,
    referanse                     VARCHAR2(99) NULL,
    type                          VARCHAR2(99) NULL,
    subjekt                       VARCHAR2(99) NULL,
    fakta                         VARCHAR2(99) NULL,
    CONSTRAINT pk_avklartefakta PRIMARY KEY (id),
    CONSTRAINT unique_avklartefakta UNIQUE(beh_resultat_id, type),
    CONSTRAINT unique_referanse UNIQUE(beh_resultat_id, referanse)
);

ALTER TABLE avklartefakta
    ADD CONSTRAINT fk_avklartefakta_beh_resultat FOREIGN KEY (beh_resultat_id) REFERENCES behandlingsresultat;

CREATE TABLE avklartefakta_registrering (
    id                    NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    avklartefakta_id      NUMBER(19) NOT NULL,
    begrunnelse           VARCHAR2(99) NULL,
    begrunnelse_fritekst  VARCHAR2(4000) NULL,
    registrert_dato       TIMESTAMP NOT NULL,
    registrert_av         VARCHAR2(99) NULL,
    endret_dato           TIMESTAMP NOT NULL,
    endret_av             VARCHAR2(99) NULL,
    CONSTRAINT pk_avklartefakta_reg PRIMARY KEY (id)
);