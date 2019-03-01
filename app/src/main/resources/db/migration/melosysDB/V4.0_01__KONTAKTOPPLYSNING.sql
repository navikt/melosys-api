-- Arbeidstabell for Kontaktopplysning
CREATE TABLE kontaktopplysning (
    saksnummer      VARCHAR2(99)  NOT NULL,
    orgnr           VARCHAR2(99)  NOT NULL,
    kontakt_navn   VARCHAR2(999) NULL,
    kontakt_orgnr    VARCHAR2(99)  NULL,
    CONSTRAINT pk_kontaktopplysning PRIMARY KEY (saksnummer, orgnr)
);

ALTER TABLE kontaktopplysning ADD CONSTRAINT fk_fagsak FOREIGN KEY (saksnummer) REFERENCES fagsak;

