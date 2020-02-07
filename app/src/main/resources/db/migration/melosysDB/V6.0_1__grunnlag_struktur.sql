CREATE TABLE BEHANDLINGSGRUNNLAG_TYPE(
    kode            VARCHAR2(20) NOT NULL,
    beskrivelse     VARCHAR2(100) NOT NULL,
    CONSTRAINT pk_behgrunnlag_type PRIMARY KEY (kode)
);

INSERT INTO BEHANDLINGSGRUNNLAG_TYPE(kode, beskrivelse) VALUES ('BEH_GRUNNLAG', 'Generelt behandlingsgrunnlag');
INSERT INTO BEHANDLINGSGRUNNLAG_TYPE(kode, beskrivelse) VALUES ('SOEKNAD_GRUNNLAG', 'Søknadsgrunnlag');

CREATE TABLE BEHANDLINGSGRUNNLAG(
    behandling_id       NUMBER(19)      NOT NULL,
    versjon             VARCHAR2(10)    NOT NULL,
    type                VARCHAR2(20)    REFERENCES BEHANDLINGSGRUNNLAG_TYPE(kode),
    original_data       CLOB            NULL,
    data                CLOB            NOT NULL,
    CONSTRAINT json_constraint CHECK (data IS JSON) ENABLE,
    CONSTRAINT fk_grunnlag_behandling FOREIGN KEY (behandling_id) REFERENCES BEHANDLING
);