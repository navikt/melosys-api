-- Arbeidstabell for saksflyt
CREATE TABLE prosessinstans (
    id              NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    prosess_type    VARCHAR2(99)   NOT NULL,
    behandling_id   NUMBER(19)     NULL,
    data            VARCHAR2(4000) NULL,
    steg            VARCHAR2(99)   NULL,
    registrert_dato TIMESTAMP      NOT NULL,
    sist_endret     TIMESTAMP      NOT NULL,
    CONSTRAINT pk_prosessinstans PRIMARY KEY (id)
);

CREATE TABLE prosess_type (
    kode    VARCHAR2(99)  NOT NULL,
    navn    VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_prosess_type PRIMARY KEY (kode)
);

INSERT INTO prosess_type (kode, navn) VALUES ('SØKNAD_A1', 'Søknad A1');

CREATE TABLE prosess_steg (
    kode    VARCHAR2(99)  NOT NULL,
    navn    VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_behandling_steg PRIMARY KEY (kode)
);

INSERT INTO prosess_steg (kode, navn) VALUES ('A1_JOURF', 'A1 journalføring');
INSERT INTO prosess_steg (kode, navn) VALUES ('A1_HENT_PERS_OPPL', 'A1 hent personopplysninger');
INSERT INTO prosess_steg (kode, navn) VALUES ('A1_HENT_ARBF_OPPL', 'A1 hent arbeidsforhold');
INSERT INTO prosess_steg (kode, navn) VALUES ('FEILET_MASKINELT', 'Feilet maskinelt');

ALTER TABLE prosessinstans ADD CONSTRAINT fk_prosinst_behandling FOREIGN KEY (behandling_id) REFERENCES behandling;
ALTER TABLE prosessinstans ADD CONSTRAINT fk_prosinst_type FOREIGN KEY (prosess_type) REFERENCES prosess_type;
ALTER TABLE prosessinstans ADD CONSTRAINT fk_prosinst_steg FOREIGN KEY (steg) REFERENCES prosess_steg;
