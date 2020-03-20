CREATE TABLE BEHANDLINGSNOTAT
(
    id              NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    behandling_id   NUMBER(19)     NOT NULL,
    tekst           VARCHAR2(4000) NOT NULL,
    registrert_dato TIMESTAMP      NOT NULL,
    endret_dato     TIMESTAMP      NOT NULL,
    registrert_av   VARCHAR2(99)   NULL,
    endret_av       VARCHAR2(99)   NULL,
    CONSTRAINT pk_behandlingsnotat PRIMARY KEY (id),
    CONSTRAINT fk_behandlingsnotat_behandling FOREIGN KEY (behandling_id) REFERENCES BEHANDLING
);

CREATE INDEX idx_behnotat_behandling ON BEHANDLINGSNOTAT(behandling_id);
