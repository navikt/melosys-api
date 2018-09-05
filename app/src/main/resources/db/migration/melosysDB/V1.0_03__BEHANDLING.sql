CREATE TABLE behandling (
    id              NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    saksnummer      VARCHAR2(99) NOT NULL,
    status          VARCHAR2(99) NOT NULL,
    beh_type        VARCHAR2(99) NOT NULL,
    registrert_dato TIMESTAMP    NOT NULL,
    endret_dato     TIMESTAMP    NOT NULL,
    sisteopplysningerhentet_dato  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_behandling PRIMARY KEY (id)
);

CREATE TABLE behandling_status (
    kode    VARCHAR2(99)  NOT NULL,
    navn    VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_behandling_status PRIMARY KEY (kode)
);

INSERT INTO behandling_status (kode, navn) VALUES ('OPPR', 'Opprettet');
INSERT INTO behandling_status (kode, navn) VALUES ('UBEH', 'Under behandling');
INSERT INTO behandling_status (kode, navn) VALUES ('FORL', 'Foreløpig lovvalg');
INSERT INTO behandling_status (kode, navn) VALUES ('AVSLU', 'Avsluttet');

CREATE TABLE behandling_type (
    kode    VARCHAR2(99)  NOT NULL,
    navn    VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_behandling_type PRIMARY KEY (kode)
);

INSERT INTO behandling_type (kode, navn) VALUES ('SKND', 'Søknad');
INSERT INTO behandling_type (kode, navn) VALUES ('UFM', 'Unntak medlemskap');
INSERT INTO behandling_type (kode, navn) VALUES ('KLG', 'Klage');
INSERT INTO behandling_type (kode, navn) VALUES ('REV', 'Revurdering');
INSERT INTO behandling_type (kode, navn) VALUES ('ML_U', 'Melding fra utenlandsk myndighet');
INSERT INTO behandling_type (kode, navn) VALUES ('PS_U', 'Påstand fra utenlandsk myndighet');

ALTER TABLE behandling ADD CONSTRAINT fk_behandling_fagsak FOREIGN KEY (saksnummer) REFERENCES fagsak;
ALTER TABLE behandling ADD CONSTRAINT fk_behandling_status FOREIGN KEY (status) REFERENCES behandling_status;
ALTER TABLE behandling ADD CONSTRAINT fk_behandling_type FOREIGN KEY (beh_type) REFERENCES behandling_type;

CREATE SEQUENCE sob_behandling_seq
MINVALUE 1
NOMAXVALUE
INCREMENT BY 1;