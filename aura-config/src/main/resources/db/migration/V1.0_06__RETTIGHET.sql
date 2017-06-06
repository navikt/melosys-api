CREATE TABLE fastsatt_rettighet (
    id                   NUMBER(19)        NOT NULL,
    type                 VARCHAR2(20 CHAR) NOT NULL,
    lovvalgsland         VARCHAR2(20 CHAR) NOT NULL,
    startdato            DATE              NOT NULL,
    sluttdato            DATE,
    standard_begrunnelse VARCHAR2(59 CHAR),
    fritekst_begrunnelse VARCHAR2(4000 CHAR),
    CONSTRAINT pk_fastsatt_rettighet PRIMARY KEY (id)
);

ALTER TABLE behandling_resultat
    ADD CONSTRAINT fk_beh_resultat_rettighet FOREIGN KEY (rettighet_id) REFERENCES fastsatt_rettighet;

CREATE TABLE rettighet_type (
    kode        VARCHAR2(20 CHAR) NOT NULL,
    navn        VARCHAR2(50 CHAR) NOT NULL,
    beskrivelse VARCHAR2(2000 CHAR),
    CONSTRAINT pk_rettighet_type PRIMARY KEY (kode)
);

ALTER TABLE fastsatt_rettighet ADD CONSTRAINT fk_fastsatt_rettighet_type FOREIGN KEY (type) REFERENCES rettighet_type;

INSERT INTO rettighet_type (kode, navn) VALUES ('LOVVALGSLAND', 'Lovvalgsland');
INSERT INTO rettighet_type (kode, navn) VALUES ('FRIVILIG_MEDL', 'Frivilig medlemskap');
INSERT INTO rettighet_type (kode, navn) VALUES ('UNNTAK_MEDL', 'Unntak medlemskap');

CREATE TABLE vilkaar_resultat (
    id           NUMBER(19) NOT NULL,
    rettighet_id NUMBER(19) NOT NULL,
    utfall       VARCHAR2(20),
    startdato    DATE       NOT NULL,
    sluttdato    DATE,
    CONSTRAINT pk_vilkaar_resultat PRIMARY KEY (id)
);

ALTER TABLE vilkaar_resultat
    ADD CONSTRAINT fk_vilkaar_rettighet FOREIGN KEY (rettighet_id) REFERENCES fastsatt_rettighet;

CREATE TABLE vilkaar_resultat_utfall_type (
    kode        VARCHAR2(20 CHAR) NOT NULL,
    navn        VARCHAR2(50 CHAR) NOT NULL,
    beskrivelse VARCHAR2(2000 CHAR),
    CONSTRAINT pk_vilkaar_resultat_utfall PRIMARY KEY (kode)
);

ALTER TABLE vilkaar_resultat
    ADD CONSTRAINT fk_vilkaar_resultat_utfall FOREIGN KEY (utfall) REFERENCES vilkaar_resultat_utfall_type;

INSERT INTO vilkaar_resultat_utfall_type (kode, navn) VALUES ('OPPFYLT', 'Oppfylt');
INSERT INTO vilkaar_resultat_utfall_type (kode, navn) VALUES ('IKKE_OPPFYLT', 'Ikke oppfylt');

CREATE TABLE behandling_grunnlag (
    vilkaar_resultat_id NUMBER(19) NOT NULL,
    saksopplysning_id NUMBER(19) NOT NULL
);

ALTER TABLE behandling_grunnlag
    ADD CONSTRAINT fk_vilkaar_grunnlag FOREIGN KEY (vilkaar_resultat_id) REFERENCES vilkaar_resultat;
ALTER TABLE behandling_grunnlag
    ADD CONSTRAINT fk_saksopplysning_grunnlag FOREIGN KEY (saksopplysning_id) REFERENCES saksopplysning;

CREATE SEQUENCE seq_rettighet MINVALUE 1 START WITH 1 INCREMENT BY 50 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_vilkaar_resultat MINVALUE 1 START WITH 1 INCREMENT BY 50 NOCACHE NOCYCLE;