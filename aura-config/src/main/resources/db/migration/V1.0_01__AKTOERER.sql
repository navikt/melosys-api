CREATE TABLE arbeidsgiver (
    id         NUMBER(19) NOT NULL,
    aktoer_id  NUMBER(19),
    org_nummer NUMBER(19),
    CONSTRAINT pk_arbeidsgiver PRIMARY KEY (id)
);


CREATE TABLE bruker (
    id           NUMBER(19) NOT NULL,
    aktoer_id    NUMBER(19),
    org_nummer   NUMBER(19),
    fnr          NUMBER(19),
    navn         VARCHAR2(200),
    foedsel_dato DATE,
    CONSTRAINT pk_bruker PRIMARY KEY (id)
);

CREATE TABLE fullmektig (
    id         NUMBER(19) NOT NULL,
    aktoer_id  NUMBER(19),
    org_nummer NUMBER(19),
    CONSTRAINT pk_fullmektig PRIMARY KEY (id)
);

CREATE SEQUENCE seq_arbeidsgiver MINVALUE 1 START WITH 1 INCREMENT BY 50 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_bruker MINVALUE 1 START WITH 1 INCREMENT BY 50 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_fullmektig MINVALUE 1 START WITH 1 INCREMENT BY 50 NOCACHE NOCYCLE;
