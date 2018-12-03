CREATE TABLE aktoer (
    id              NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    saksnummer      VARCHAR2(99) NOT NULL,
    aktoer_id       VARCHAR2(99) NULL,
    orgnr           VARCHAR2(99) NULL,
    utenlandsk_id   VARCHAR2(99) NULL,
    rolle           VARCHAR2(99) NOT NULL,
    CONSTRAINT pk_bruker PRIMARY KEY (id)
);

CREATE INDEX idx_aktoer_sak ON aktoer(saksnummer);

CREATE TABLE rolle_type (
    kode        VARCHAR2(99)  NOT NULL,
    navn        VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_rolle PRIMARY KEY (kode)
);

INSERT INTO rolle_type (kode, navn) VALUES ('BRUKER', 'Personen som avklaringen lovvalg eller medlemskap gjelder for.');
INSERT INTO rolle_type (kode, navn) VALUES ('ARBEIDSGIVER', 'Arbeidsgiver som sender bruker for arbeid eller oppdrag i utlandet.');
INSERT INTO rolle_type (kode, navn) VALUES ('REPRESENTANT', 'Aktøren representerer bruker og/eller arbeidsgiver i saken.');
INSERT INTO rolle_type (kode, navn) VALUES ('MYNDIGHET', 'Myndigheten det sendes til og/eller mottas dokumentasjon fra i saken.');

ALTER TABLE aktoer ADD CONSTRAINT fk_aktoer_rolle FOREIGN KEY (rolle) REFERENCES rolle_type;
