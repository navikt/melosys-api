CREATE TABLE saksopplysning (
    id            NUMBER(19)       NOT NULL,
    fagsak_id     NUMBER(19)       NOT NULL,
    behandling_id NUMBER(19)       NOT NULL,
    kilde         VARCHAR2(7 CHAR) NOT NULL,
    gyldighet_fra DATE             NOT NULL,
    gyldighet_til DATE,
    CONSTRAINT pk_saksopplysning PRIMARY KEY (id)
);

CREATE TABLE saksopplysning_kilde (
    kode        VARCHAR2(7 CHAR)  NOT NULL,
    navn        VARCHAR2(50 CHAR) NOT NULL,
    beskrivelse VARCHAR2(2000 CHAR),
    CONSTRAINT pk_saksopplysning_kilde PRIMARY KEY (kode)
);

INSERT INTO saksopplysning_kilde (kode, navn) VALUES ('JOARK', 'JOARK');
INSERT INTO saksopplysning_kilde (kode, navn) VALUES ('TPS', 'TPS');

ALTER TABLE saksopplysning ADD CONSTRAINT fk_saksopplysning_kilde_1 FOREIGN KEY (kilde) REFERENCES saksopplysning_kilde (kode);

CREATE TABLE arbeidsforhold (
    id                NUMBER(19) NOT NULL,
    arbeidsgiver_id      NUMBER(19) NOT NULL,
    arbeidstaker_id      NUMBER(19) NOT NULL,
    ansettelse_fra    DATE,
    ansettelse_til    DATE,
    CONSTRAINT pk_arbeidsforhold PRIMARY KEY (id)
);

ALTER TABLE arbeidsforhold ADD CONSTRAINT fk_arbeidsforhold_arbeidsgiver FOREIGN KEY (arbeidsgiver_id) REFERENCES arbeidsgiver;
ALTER TABLE arbeidsforhold ADD CONSTRAINT fk_arbeidsforhold_arbeidstaker FOREIGN KEY (arbeidstaker_id) REFERENCES bruker;

CREATE SEQUENCE seq_saksopplysning MINVALUE 1 START WITH 1 INCREMENT BY 50 NOCACHE NOCYCLE;