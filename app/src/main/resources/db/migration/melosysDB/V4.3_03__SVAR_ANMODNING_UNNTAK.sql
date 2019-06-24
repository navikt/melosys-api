CREATE TABLE innvilget_anmodningsperiode
(
    id                   NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    anmodningsperiode_id NUMBER(19) NOT NULL,
    registrert_dato      DATE       NOT NULL,
    begrunnelse_fritekst VARCHAR2(99) NULL
);

CREATE TABLE delvis_innvilget_anmodningsperiode
(
    id                   NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    anmodningsperiode_id NUMBER(19)   NOT NULL,
    registrert_dato      DATE         NOT NULL,
    begrunnelse_fritekst VARCHAR2(99) NULL,
    fom                  DATE         NULL,
    tom                  DATE         NULL
);

CREATE TABLE avslag_anmodningsperiode
(
    id                   NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    anmodningsperiode_id NUMBER(19)   NOT NULL,
    registrert_dato      DATE         NOT NULL,
    begrunnelse_fritekst VARCHAR2(99) NULL
);