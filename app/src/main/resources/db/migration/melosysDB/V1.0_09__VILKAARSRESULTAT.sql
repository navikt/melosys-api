CREATE TABLE vilkaarsresultat (
    id                    NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    beh_resultat_id       NUMBER(19) NOT NULL,
    vilkaar               VARCHAR2(99) NOT NULL,
    oppfylt               NUMBER(1) NOT NULL,
    begrunnelse_fritekst  VARCHAR2(4000) NULL,
    registrert_dato       TIMESTAMP NOT NULL,
    registrert_av         VARCHAR2(99) NULL,
    endret_dato           TIMESTAMP NOT NULL,
    endret_av             VARCHAR2(99) NULL,
    CONSTRAINT pk_vilkaarsresultat PRIMARY KEY (id)
);

ALTER TABLE vilkaarsresultat
    ADD CONSTRAINT fk_vilkaar_beh_resultat FOREIGN KEY (beh_resultat_id) REFERENCES behandlingsresultat;

ALTER TABLE vilkaarsresultat
    ADD CONSTRAINT uq_vilkaarsresultat UNIQUE (beh_resultat_id, vilkaar);

CREATE TABLE vilkaar_begrunnelse (
    id                    NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    vilkaar_resultat_id   NUMBER(19) NOT NULL,
    kode                  VARCHAR2(99) NOT NULL,
    registrert_dato       TIMESTAMP NOT NULL,
    registrert_av         VARCHAR2(99) NULL,
    endret_dato           TIMESTAMP NOT NULL,
    endret_av             VARCHAR2(99) NULL,
    CONSTRAINT pk_vilkaar_begrunnelse PRIMARY KEY (id)
);

ALTER TABLE vilkaar_begrunnelse
    ADD CONSTRAINT fk_vilkaar_res_grunn FOREIGN KEY (vilkaar_resultat_id) REFERENCES vilkaarsresultat ON DELETE CASCADE;
ALTER TABLE vilkaar_begrunnelse
    ADD CONSTRAINT uq_vilkaar_begrunnelse UNIQUE (vilkaar_resultat_id, kode);