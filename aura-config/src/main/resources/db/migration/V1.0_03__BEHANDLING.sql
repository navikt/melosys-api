CREATE TABLE behandling (
    id                     NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    behandling_id          NUMBER(19)        NOT NULL,
    fagsak_id              NUMBER(19)        NOT NULL,
    status                 VARCHAR2(20 CHAR) NOT NULL,
    type                   VARCHAR2(20 CHAR) NOT NULL,
    frist                  DATE,
    behandling_resultat_id NUMBER(19),
    CONSTRAINT pk_behandling PRIMARY KEY (id)
);

CREATE TABLE behandling_status (
    kode        VARCHAR2(20 CHAR) NOT NULL,
    navn        VARCHAR2(50 CHAR) NOT NULL,
    beskrivelse VARCHAR2(2000 CHAR),
    CONSTRAINT pk_behandling_status PRIMARY KEY (kode)
);

INSERT INTO behandling_status (kode, navn) VALUES ('OPPR', 'Opprettet');
INSERT INTO behandling_status (kode, navn) VALUES ('KLAR', 'Klargjort');
INSERT INTO behandling_status (kode, navn) VALUES ('UTRED', 'Utredes');
INSERT INTO behandling_status (kode, navn) VALUES ('F_VED', 'Fatter vedtak');
INSERT INTO behandling_status (kode, navn) VALUES ('I_VED', 'Iverksetter vedtak');
INSERT INTO behandling_status (kode, navn) VALUES ('AVSLU', 'Avsluttet');

CREATE TABLE behandling_type (
    kode        VARCHAR2(20 CHAR) NOT NULL,
    navn        VARCHAR2(50 CHAR) NOT NULL,
    beskrivelse VARCHAR2(2000 CHAR),
    CONSTRAINT pk_behandling_type PRIMARY KEY (kode)
);

INSERT INTO behandling_type (kode, navn) VALUES ('NY', 'Ny');
INSERT INTO behandling_type (kode, navn) VALUES ('ENDRING', 'Endring');
INSERT INTO behandling_type (kode, navn) VALUES ('KLAGE', 'Klage');

ALTER TABLE behandling
    ADD CONSTRAINT fk_behandling_fagsak_1 FOREIGN KEY (fagsak_id) REFERENCES fagsak;
ALTER TABLE behandling
    ADD CONSTRAINT fk_behandling_status_1 FOREIGN KEY (status) REFERENCES behandling_status;
ALTER TABLE behandling
    ADD CONSTRAINT fk_behandling_type_1 FOREIGN KEY (type) REFERENCES behandling_type;

/*Midlertidig. Til å generere behandlingsId*/
CREATE SEQUENCE seq_behandling MINVALUE 1 START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE OR REPLACE TRIGGER trigger_behandling_id
BEFORE INSERT ON behandling
FOR EACH ROW
WHEN (new.behandling_id IS NULL)
    BEGIN
        SELECT seq_behandling.nextval
        INTO :new.behandling_id
        FROM dual;
    END;