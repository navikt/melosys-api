CREATE TABLE fagsak (
    id              NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    saksnummer      NUMBER(19, 0) NULL,
    fagsak_type     VARCHAR2(99)  NOT NULL,
    versjon         INTEGER       NOT NULL,
    status          VARCHAR2(99)  NOT NULL,
    registrert_dato TIMESTAMP     NOT NULL,
    CONSTRAINT pk_fagsak PRIMARY KEY (id)
);

CREATE TABLE fagsak_type (
    kode        VARCHAR2(99)  NOT NULL,
    navn        VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_fagsak_type PRIMARY KEY (kode)
);
INSERT INTO fagsak_type (kode, navn) VALUES ('A1', 'Søknad A1');

CREATE TABLE fagsak_status (
    kode        VARCHAR2(99)  NOT NULL,
    navn        VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_fagsak_status PRIMARY KEY (kode)
);
INSERT INTO fagsak_status (kode, navn) VALUES ('OPPR', 'Opprettet');
INSERT INTO fagsak_status (kode, navn) VALUES ('UBEH', 'Under behandling');
INSERT INTO fagsak_status (kode, navn) VALUES ('LOP', 'Løpende');
INSERT INTO fagsak_status (kode, navn) VALUES ('AVSLU', 'Avsluttet');

ALTER TABLE fagsak ADD CONSTRAINT fk_fagsak_type FOREIGN KEY (fagsak_type) REFERENCES fagsak_type;
ALTER TABLE fagsak ADD CONSTRAINT fk_fagsak_satus FOREIGN KEY (status) REFERENCES fagsak_status;
