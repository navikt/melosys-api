CREATE TABLE behandling (
    id                     NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    behandling_id          NUMBER(19)        NOT NULL,
    fagsak_id              NUMBER(19)        NOT NULL,
    status                 VARCHAR2(20 CHAR) NOT NULL,
    type                   VARCHAR2(20 CHAR) NOT NULL,
    behandling_resultat_id NUMBER(19),
    CONSTRAINT pk_behandling PRIMARY KEY (id)
);

CREATE TABLE behandling_status (
    kode            VARCHAR2(20 CHAR) NOT NULL,
    navn            VARCHAR2(50 CHAR) NOT NULL,
    beskrivelse     VARCHAR2(2000 CHAR),
    CONSTRAINT pk_behandling_status PRIMARY KEY (kode)
);

CREATE TABLE behandling_type (
    kode            VARCHAR2(20 CHAR) NOT NULL,
    navn            VARCHAR2(50 CHAR) NOT NULL,
    beskrivelse     VARCHAR2(2000 CHAR),
    CONSTRAINT pk_behandling_type PRIMARY KEY (kode)
);

INSERT INTO behandling_status (kode, navn) VALUES ('OPPR', 'Opprettet');
INSERT INTO behandling_status (kode, navn) VALUES ('UTRED', 'Utredes');
INSERT INTO behandling_status (kode, navn) VALUES ('F_VED', 'Fatter vedtak');
INSERT INTO behandling_status (kode, navn) VALUES ('I_VED', 'Iverksetter vedtak');
INSERT INTO behandling_status (kode, navn) VALUES ('AVSLU', 'Avsluttet');

ALTER TABLE behandling ADD CONSTRAINT fk_behandling_fagsak_1 FOREIGN KEY (fagsak_id) REFERENCES fagsak;
ALTER TABLE behandling ADD CONSTRAINT fk_behandling_status_1 FOREIGN KEY (status) REFERENCES behandling_status;
ALTER TABLE behandling ADD CONSTRAINT fk_behandling_type_1 FOREIGN KEY (status) REFERENCES behandling_type;