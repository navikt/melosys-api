CREATE TABLE fagsak (
    saksnummer      VARCHAR2(99)  NOT NULL,
    gsak_saksnummer NUMBER(19)    NULL,
    rina_saksnummer VARCHAR2(99)  NULL,
    fagsak_type     VARCHAR2(99)  NULL,
    status          VARCHAR2(99)  NOT NULL,
    registrert_dato TIMESTAMP     NOT NULL,
    endret_dato     TIMESTAMP     NOT NULL,
    registrert_av   VARCHAR2(99)  NULL,
    endret_av       VARCHAR2(99)  NULL,
    CONSTRAINT pk_fagsak PRIMARY KEY (saksnummer)
);

CREATE INDEX idx_fagsak_type ON fagsak(fagsak_type);
CREATE INDEX idx_fagsak_status ON fagsak(status);

CREATE TABLE fagsak_type (
    kode        VARCHAR2(99)  NOT NULL,
    navn        VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_fagsak_type PRIMARY KEY (kode)
);
INSERT INTO fagsak_type (kode, navn) VALUES ('EU_EOS', 'Saken skal behandles etter trygdeforordningene i EØS-avtalen');
INSERT INTO fagsak_type (kode, navn) VALUES ('TRYGDEAVTALE', 'Saken skal behandles etter en trygdeavtale');
INSERT INTO fagsak_type (kode, navn) VALUES ('FTRL', '	Saken skal behandles etter folketrygdloven');

CREATE TABLE fagsak_status (
    kode        VARCHAR2(99)  NOT NULL,
    navn        VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_fagsak_status PRIMARY KEY (kode)
);
INSERT INTO fagsak_status (kode, navn) VALUES ('OPPRETTET', 'Saken har blitt opprettet men behandlingen har ikke startet eller er ikke ferdigstilt ennå.');
INSERT INTO fagsak_status (kode, navn) VALUES ('LOVVALG_AVKLART', '	Avklart hvilket landstrygdeloving bruker skal omfattes av.');
INSERT INTO fagsak_status (kode, navn) VALUES ('AVSLUTTET', 'Saken er avsluttet');
INSERT INTO fagsak_status (kode, navn) VALUES ('HENLAGT', 'Saken har blitt henlagt');

ALTER TABLE fagsak ADD CONSTRAINT fk_fagsak_type FOREIGN KEY (fagsak_type) REFERENCES fagsak_type;
ALTER TABLE fagsak ADD CONSTRAINT fk_fagsak_satus FOREIGN KEY (status) REFERENCES fagsak_status;

CREATE SEQUENCE saksnummer_seq
MINVALUE 1
NOMAXVALUE
INCREMENT BY 1;
