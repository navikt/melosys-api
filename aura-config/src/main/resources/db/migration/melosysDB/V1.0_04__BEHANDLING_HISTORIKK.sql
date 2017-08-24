CREATE TABLE behandling_historikk (
    id             NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    behandling_id  NUMBER(19)  NOT NULL,
    dato           TIMESTAMP   NOT NULL,
    status         VARCHAR2    NOT NULL,
    steg           VARCHAR2    NULL,
    ident          VARCHAR2    NOT NULL,
    kommentar      VARCHAR2    NOT NULL,
    CONSTRAINT pk_behandling_resultat PRIMARY KEY (id)
);

ALTER TABLE behandling_historikk
    ADD CONSTRAINT fk_beh_historikk_status FOREIGN KEY (behandling_id) REFERENCES behandling;

ALTER TABLE behandling_historikk
    ADD CONSTRAINT fk_beh_historikk_behandling FOREIGN KEY (status) REFERENCES behandling_status;
