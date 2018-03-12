CREATE TABLE oppgave_tilbakkelegging (
    id                 NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    oppgave_id         VARCHAR2(99)  NOT NULL,
    saksbehandler_id   VARCHAR2(99)  NOT NULL,
    begrunnelse        VARCHAR2(4000)  NOT NULL,
    registrert_dato    TIMESTAMP     NOT NULL,
    CONSTRAINT pk_tilbakkelegging PRIMARY KEY (id)
);
