CREATE TABLE BEHANDLINGSGRUNNLAG_TYPE
(
    kode        VARCHAR2(20)  NOT NULL,
    beskrivelse VARCHAR2(100) NOT NULL,
    CONSTRAINT pk_behandlingsgrunnnlag_type PRIMARY KEY (kode)
);

INSERT INTO BEHANDLINGSGRUNNLAG_TYPE(kode, beskrivelse)VALUES ('GENERELT', 'Generelle opplysninger');
INSERT INTO BEHANDLINGSGRUNNLAG_TYPE(kode, beskrivelse)VALUES ('SØKNAD', 'Opplysninger fra søknad');

CREATE TABLE BEHANDLINGSGRUNNLAG
(
    id              NUMBER(19)   GENERATED ALWAYS AS IDENTITY,
    behandling_id   NUMBER(19)   NOT NULL,
    versjon         VARCHAR2(10) NOT NULL,
    registrert_dato TIMESTAMP    NOT NULL,
    endret_dato     TIMESTAMP    NOT NULL,
    type            VARCHAR2(20) REFERENCES BEHANDLINGSGRUNNLAG_TYPE (kode),
    original_data   CLOB         NULL,
    data            CLOB         NOT NULL,
    CONSTRAINT pk_behandlingsgrunnlag PRIMARY KEY (id),
    CONSTRAINT fk_behandlingsgrunnlag_behandling FOREIGN KEY (behandling_id) REFERENCES BEHANDLING,
    CONSTRAINT json_constraint CHECK (data IS JSON) ENABLE
);

CREATE UNIQUE INDEX idx_behandling_unik ON BEHANDLINGSGRUNNLAG (behandling_id);