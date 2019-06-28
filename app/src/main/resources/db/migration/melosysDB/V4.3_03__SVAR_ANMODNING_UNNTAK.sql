CREATE TABLE anmodningsperiode_svar
(
    anmodningsperiode_id NUMBER(19)   NOT NULL,
    svar_type            VARCHAR2(20) NOT NULL,
    registrert_dato      DATE         NOT NULL,
    begrunnelse_fritekst VARCHAR2(99) NULL,
    innvilget_fom_dato   DATE         NULL,
    innvilget_tom_dato   DATE         NULL,
    CONSTRAINT pk_anmodningsperiode_svar PRIMARY KEY(anmodningsperiode_id)
);

CREATE INDEX idx_anmodningssvar_type ON anmodningsperiode_svar(svar_type);