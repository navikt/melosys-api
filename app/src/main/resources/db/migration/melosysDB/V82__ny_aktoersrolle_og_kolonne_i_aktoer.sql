INSERT INTO rolle_type (kode, navn)
VALUES ('FULLMEKTIG', 'Fullmektig');

CREATE TABLE fullmakt
(
    id        NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    type      VARCHAR2(99) NOT NULL,
    aktoer_id NUMBER(19) NOT NULL,
    CONSTRAINT pk_fullmakt PRIMARY KEY (id)
);
CREATE INDEX idx_fullmakt_aktoer ON fullmakt (aktoer_id);
