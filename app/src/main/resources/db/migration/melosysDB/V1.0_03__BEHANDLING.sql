CREATE TABLE behandling (
    id              NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    saksnummer      VARCHAR2(99) NOT NULL,
    status          VARCHAR2(99) NOT NULL,
    beh_type        VARCHAR2(99) NOT NULL,
    registrert_dato TIMESTAMP    NOT NULL,
    endret_dato     TIMESTAMP    NOT NULL,
    siste_opplysninger_hentet_dato  TIMESTAMP    NULL,
    CONSTRAINT pk_behandling PRIMARY KEY (id)
);

CREATE TABLE behandling_status (
    kode    VARCHAR2(99)  NOT NULL,
    navn    VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_behandling_status PRIMARY KEY (kode)
);

INSERT INTO behandling_status (kode, navn) VALUES ('OPPRETTET', 'Opprettet');
INSERT INTO behandling_status (kode, navn) VALUES ('UNDER_BEHANDLING', 'Under behandling');
INSERT INTO behandling_status (kode, navn) VALUES ('AVVENT_DOK_UTL', 'Avventer dokumentasjon fra utlandet');
INSERT INTO behandling_status (kode, navn) VALUES ('AVVENT_DOK_PART', 'Avventer dokumentasjon fra en part');
INSERT INTO behandling_status (kode, navn) VALUES ('AVSLUTTET', 'Avsluttet');

CREATE TABLE behandling_type (
    kode    VARCHAR2(99)  NOT NULL,
    navn    VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_behandling_type PRIMARY KEY (kode)
);

INSERT INTO behandling_type (kode, navn) VALUES ('SOEKNAD', 'Behandling av søknad');
INSERT INTO behandling_type (kode, navn) VALUES ('KLAGE', 'Behandling av klage eller anke');
INSERT INTO behandling_type (kode, navn) VALUES ('POSTING_UTL', 'Behandling av melding om posting fra utenlandske myndigheter');
INSERT INTO behandling_type (kode, navn) VALUES ('NORGE_UTPEKT', 'Behandling av at Norge er utpekt fra utenlandske myndigheter');
INSERT INTO behandling_type (kode, navn) VALUES ('PAASTAND_UTL', 'Behandling av påstand fra utenlandske myndigheter');
INSERT INTO behandling_type (kode, navn) VALUES ('REVURDERING ', 'Behandling av revurdering av et tidligere vedtak');

ALTER TABLE behandling ADD CONSTRAINT fk_behandling_fagsak FOREIGN KEY (saksnummer) REFERENCES fagsak;
ALTER TABLE behandling ADD CONSTRAINT fk_behandling_status FOREIGN KEY (status) REFERENCES behandling_status;
ALTER TABLE behandling ADD CONSTRAINT fk_behandling_type FOREIGN KEY (beh_type) REFERENCES behandling_type;

CREATE SEQUENCE sob_behandling_seq
MINVALUE 1
NOMAXVALUE
INCREMENT BY 1;