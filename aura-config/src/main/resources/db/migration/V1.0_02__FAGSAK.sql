CREATE TABLE fagsak (
    id           NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    saksnummer   NUMBER(19, 0),
    status       VARCHAR2(20 CHAR) NOT NULL,
    virkemiddel  VARCHAR2(20 CHAR),
    bruker       NUMBER(19),
    arbeidsgiver NUMBER(19),
    fullmektig   NUMBER(19),
    CONSTRAINT pk_fagsak PRIMARY KEY (id)
);

CREATE TABLE fagsak_status (
    kode        VARCHAR2(7 CHAR)  NOT NULL,
    navn        VARCHAR2(40 CHAR) NOT NULL,
    beskrivelse VARCHAR2(4000 CHAR),
    CONSTRAINT pk_fagsak_status PRIMARY KEY (kode)
);

INSERT INTO fagsak_status (kode, navn) VALUES ('OPPR', 'Opprettet');
INSERT INTO fagsak_status (kode, navn) VALUES ('UBEH', 'Under behandling');
INSERT INTO fagsak_status (kode, navn) VALUES ('LOP', 'Løpende');
INSERT INTO fagsak_status (kode, navn) VALUES ('AVSLU', 'Avsluttet');

ALTER TABLE fagsak ADD CONSTRAINT fk_fagsak_satus FOREIGN KEY (status) REFERENCES fagsak_status;
ALTER TABLE fagsak ADD CONSTRAINT fk_fagsak_bruker FOREIGN KEY (bruker) REFERENCES bruker;
ALTER TABLE fagsak ADD CONSTRAINT fk_fagsak_arbeidsgiver FOREIGN KEY (arbeidsgiver) REFERENCES arbeidsgiver;
ALTER TABLE fagsak ADD CONSTRAINT fk_fagsak_fullmektig FOREIGN KEY (fullmektig) REFERENCES fullmektig;
