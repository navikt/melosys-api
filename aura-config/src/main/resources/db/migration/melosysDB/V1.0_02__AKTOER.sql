CREATE TABLE aktoer (
    id              NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    fagsak_id       NUMBER(19)  NOT NULL,
    aktoer_id       VARCHAR2    NOT NULL,
    ekstern_id      VARCHAR2    NOT NULL,
    rolle           VARCHAR2    NOT NULL,
    CONSTRAINT pk_bruker PRIMARY KEY (id)
);

CREATE TABLE rolle_type (
    kode        VARCHAR2  NOT NULL,
    navn        VARCHAR2  NOT NULL,
    CONSTRAINT pk_rolle PRIMARY KEY (kode)
);

INSERT INTO rolle_type (kode, navn) VALUES ('ARBTAG', 'Arbeidstager');
INSERT INTO rolle_type (kode, navn) VALUES ('ARBGIV', 'Arbeidsgiver');
INSERT INTO rolle_type (kode, navn) VALUES ('FULLMK', 'Fullmektig');

ALTER TABLE aktoer ADD CONSTRAINT fk_aktoer_rolle FOREIGN KEY (rolle) REFERENCES rolle_type;
