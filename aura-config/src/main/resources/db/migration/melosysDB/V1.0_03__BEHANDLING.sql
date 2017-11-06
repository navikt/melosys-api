CREATE TABLE behandling (
    id              NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    gsak_id         NUMBER(19)   NOT NULL,
    fagsak_id       NUMBER(19)   NOT NULL,
    status          VARCHAR2(99) NOT NULL,
    steg            VARCHAR2(99) NULL,
    beh_type        VARCHAR2(99) NOT NULL,
    registrert_dato TIMESTAMP    NOT NULL,
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

CREATE TABLE behandling_steg (
    kode    VARCHAR2(99)  NOT NULL,
    navn    VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_behandling_steg PRIMARY KEY (kode)
);

INSERT INTO behandling_steg (kode, navn) VALUES ('NY', 'Ny');
INSERT INTO behandling_steg (kode, navn) VALUES ('KLARGJORT', 'Klargjort');

CREATE TABLE behandling_type (
    kode    VARCHAR2(99)  NOT NULL,
    navn    VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_behandling_type PRIMARY KEY (kode)
);

INSERT INTO behandling_type (kode, navn) VALUES ('SØKNAD', 'Behandling av søknad');
INSERT INTO behandling_type (kode, navn) VALUES ('KLAGE', 'Behandling av klage');
INSERT INTO behandling_type (kode, navn) VALUES ('MELDING_UTL', 'Behandling av meldinger fra utenlandske myndigheter');
INSERT INTO behandling_type (kode, navn) VALUES ('PÅSTAND_UTL', 'Behandling av påstander fra utenlandske myndigheter');

ALTER TABLE behandling ADD CONSTRAINT fk_behandling_fagsak FOREIGN KEY (fagsak_id) REFERENCES fagsak;
ALTER TABLE behandling ADD CONSTRAINT fk_behandling_status FOREIGN KEY (status) REFERENCES behandling_status;
ALTER TABLE behandling ADD CONSTRAINT fk_behandling_type FOREIGN KEY (beh_type) REFERENCES behandling_type;
