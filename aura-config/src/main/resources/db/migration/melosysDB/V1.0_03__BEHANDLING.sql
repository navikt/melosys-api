CREATE TABLE behandling (
    id              NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    behandling_id   NUMBER(19)   NOT NULL,
    fagsak_id       NUMBER(19)   NOT NULL,
    status          VARCHAR2(99) NOT NULL,
    steg            VARCHAR2(99) NULL,
    behandling_type VARCHAR2(99) NOT NULL,
    registrert_dato TIMESTAMP    NOT NULL,
    CONSTRAINT pk_behandling PRIMARY KEY (id)
);

CREATE TABLE behandling_status (
    kode    VARCHAR2(99)  NOT NULL,
    navn    VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_behandling_status PRIMARY KEY (kode)
);

INSERT INTO behandling_status (kode, navn) VALUES ('OPPR', 'Opprettet');
INSERT INTO behandling_status (kode, navn) VALUES ('KLAR', 'Klargjort');
INSERT INTO behandling_status (kode, navn) VALUES ('UTRED', 'Utredes');
INSERT INTO behandling_status (kode, navn) VALUES ('F_VED', 'Fatter vedtak');
INSERT INTO behandling_status (kode, navn) VALUES ('I_VED', 'Iverksetter vedtak');
INSERT INTO behandling_status (kode, navn) VALUES ('AVSLU', 'Avsluttet');

CREATE TABLE behandling_type (
    kode    VARCHAR2(99)  NOT NULL,
    navn    VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_behandling_type PRIMARY KEY (kode)
);

INSERT INTO behandling_type (kode, navn) VALUES ('NY', 'Ny');
INSERT INTO behandling_type (kode, navn) VALUES ('ENDRING', 'Endring');
INSERT INTO behandling_type (kode, navn) VALUES ('KLAGE', 'Klage');

ALTER TABLE behandling ADD CONSTRAINT fk_behandling_fagsak FOREIGN KEY (fagsak_id) REFERENCES fagsak;
ALTER TABLE behandling ADD CONSTRAINT fk_behandling_status FOREIGN KEY (status) REFERENCES behandling_status;
ALTER TABLE behandling ADD CONSTRAINT fk_behandling_type FOREIGN KEY (behandling_type) REFERENCES behandling_type;
