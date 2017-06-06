CREATE TABLE behandling_resultat (
    id               NUMBER(19)        NOT NULL,
    behandling_maate VARCHAR2(20 CHAR) NOT NULL,
    rettighet_id     NUMBER(19)        NOT NULL,
    vedtak_id     NUMBER(19)        NOT NULL,
    CONSTRAINT pk_behandling_resultat PRIMARY KEY (id)
);

ALTER TABLE behandling
    ADD CONSTRAINT fk_behandling_resultat FOREIGN KEY (behandling_resultat_id) REFERENCES behandling_resultat;

CREATE TABLE behandling_maate (
    kode        VARCHAR2(20 CHAR) NOT NULL,
    navn        VARCHAR2(50 CHAR) NOT NULL,
    beskrivelse VARCHAR2(2000 CHAR),
    CONSTRAINT pk_behandling_maate PRIMARY KEY (kode)
);

ALTER TABLE behandling_resultat
    ADD CONSTRAINT fk_behandling_resultat_maate FOREIGN KEY (behandling_maate) REFERENCES behandling_maate;

INSERT INTO behandling_maate (kode, navn) VALUES ('AUTO', 'hel autmatisert');
INSERT INTO behandling_maate (kode, navn) VALUES ('DELVIS_AUTO', 'delvis automatisert');
INSERT INTO behandling_maate (kode, navn) VALUES ('MANUELT', 'hel manuelt');

CREATE SEQUENCE seq_behandling_resultat MINVALUE 1 START WITH 1 INCREMENT BY 50 NOCACHE NOCYCLE;

CREATE TABLE vedtak (
    id          NUMBER(19)        NOT NULL,
    vedtak_id   NUMBER(19)        NOT NULL,
    resultat    VARCHAR2(20 CHAR) NOT NULL,
    dato_fattet TIMESTAMP(0),
    CONSTRAINT pk_vedtak PRIMARY KEY (id)
);

CREATE TABLE vedtak_resultat_type (
    kode        VARCHAR2(20 CHAR) NOT NULL,
    navn        VARCHAR2(50 CHAR) NOT NULL,
    beskrivelse VARCHAR2(2000 CHAR),
    CONSTRAINT pk_vedtak_resultat_type PRIMARY KEY (kode)
);

ALTER TABLE vedtak ADD CONSTRAINT fk_vedtak_resultat_type FOREIGN KEY (resultat) REFERENCES vedtak_resultat_type;

INSERT INTO vedtak_resultat_type (kode, navn) VALUES ('INNVILGET', 'Innvilget');
INSERT INTO vedtak_resultat_type (kode, navn) VALUES ('DELVIS_INNVILGET', 'Delvis innvilget');
INSERT INTO vedtak_resultat_type (kode, navn) VALUES ('AVSLAG', 'Avslag');

ALTER TABLE behandling_resultat
    ADD CONSTRAINT fk_beh_resultat_vedtak FOREIGN KEY (vedtak_id) REFERENCES vedtak;

CREATE SEQUENCE seq_vedtak MINVALUE 1 START WITH 1 INCREMENT BY 50 NOCACHE NOCYCLE;