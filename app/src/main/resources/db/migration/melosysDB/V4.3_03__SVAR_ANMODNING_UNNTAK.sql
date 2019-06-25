CREATE TABLE anmodningsperiode_svar
(
    id                   NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    svar_type            VARCHAR2(20) NOT NULL,
    registrert_dato      DATE         NOT NULL,
    begrunnelse_fritekst VARCHAR2(99) NULL,
    fom_dato             DATE         NULL,
    tom_dato             DATE         NULL
);