CREATE TABLE behandling_historikk (
    id             NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    behandling_id  NUMBER(19)     NOT NULL,
    dato           TIMESTAMP      NOT NULL,
    status         VARCHAR2(99)   NOT NULL,
    ident          VARCHAR2(99)   NOT NULL,
    kommentar      VARCHAR2(4000) NOT NULL,
    CONSTRAINT pk_behandling_resultat PRIMARY KEY (id)
);

CREATE INDEX idx_historikk_behandling ON behandling_historikk(behandling_id);

ALTER TABLE behandling_historikk
    ADD CONSTRAINT fk_beh_historikk_behandling FOREIGN KEY (status) REFERENCES behandling_status;

ALTER TABLE behandling_historikk
    ADD CONSTRAINT fk_beh_historikk_status FOREIGN KEY (behandling_id) REFERENCES behandling;