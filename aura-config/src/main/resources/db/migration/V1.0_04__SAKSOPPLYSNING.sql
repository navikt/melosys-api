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