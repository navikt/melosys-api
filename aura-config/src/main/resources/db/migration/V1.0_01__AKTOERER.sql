CREATE TABLE arbeidsgiver (
    id         NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    aktoer_id  NUMBER(19),
    org_nummer NUMBER(19),
    CONSTRAINT pk_arbeidsgiver PRIMARY KEY (id)
);


CREATE TABLE bruker (
    id           NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    aktoer_id    NUMBER(19),
    org_nummer   NUMBER(19),
    fnr          VARCHAR2(20),
    navn         VARCHAR2(200),
    foedsel_dato DATE,
    CONSTRAINT pk_bruker PRIMARY KEY (id)
);

CREATE TABLE fullmektig (
    id         NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    aktoer_id  NUMBER(19),
    org_nummer NUMBER(19),
    CONSTRAINT pk_fullmektig PRIMARY KEY (id)
);
