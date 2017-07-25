CREATE TABLE arbeidsforhold (
    id              NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    arbeidsgiver_id NUMBER(19) NOT NULL,
    arbeidstaker_id NUMBER(19) NOT NULL,
    ansettelse_fra  DATE,
    ansettelse_til  DATE,
    sist_bekreftet  DATE,
    type            VARCHAR2(50 CHAR),
    CONSTRAINT pk_arbeidsforhold PRIMARY KEY (id)
);

ALTER TABLE arbeidsforhold ADD CONSTRAINT fk_arbeidsforhold_arbeidsgiver FOREIGN KEY (arbeidsgiver_id) REFERENCES arbeidsgiver;
ALTER TABLE arbeidsforhold ADD CONSTRAINT fk_arbeidsforhold_arbeidstaker FOREIGN KEY (arbeidstaker_id) REFERENCES bruker;

CREATE TABLE arbeidsforhold_type (
    kode        VARCHAR2(50 CHAR) NOT NULL,
    navn        VARCHAR2(100 CHAR) NOT NULL,
    beskrivelse VARCHAR2(2000 CHAR),
    CONSTRAINT pk_arbeidsforhold_type PRIMARY KEY (kode)
);

ALTER TABLE arbeidsforhold ADD CONSTRAINT fk_arbeidsforhold_type FOREIGN KEY (type) REFERENCES arbeidsforhold_type;

INSERT INTO arbeidsforhold_type (kode, navn) VALUES ('ordinaertArbeidsforhold', 'ordinaertArbeidsforhold');
INSERT INTO arbeidsforhold_type (kode, navn) VALUES ('maritimtArbeidsforhold', 'maritimtArbeidsforhold');
INSERT INTO arbeidsforhold_type (kode, navn) VALUES ('forenkletOppgjoersordning', 'forenkletOppgjoersordning');
INSERT INTO arbeidsforhold_type (kode, navn) VALUES ('frilanserOppdragstakerHonorarPersonerMm', 'frilanserOppdragstakerHonorarPersonerMm');
INSERT INTO arbeidsforhold_type (kode, navn) VALUES ('pensjonOgAndreTyperYtelserUtenAnsettelsesforhold', 'pensjonOgAndreTyperYtelserUtenAnsettelsesforhold');

CREATE TABLE arbeidsavtale (
    id                NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    arbeidsforhold_id NUMBER(19)         NOT NULL,
    fartsomraade      VARCHAR2(20 CHAR),
    skipsregister     VARCHAR2(20 CHAR),
    skipstype         VARCHAR2(20 CHAR),
    stillingsprosent  NUMBER,
    timer_per_uke     NUMBER(19),
    yrke              VARCHAR2(100 CHAR) NOT NULL,
    CONSTRAINT pk_arbeidsavtale PRIMARY KEY (id)
);

ALTER TABLE arbeidsavtale ADD CONSTRAINT fk_arbeidsavtale_forhold FOREIGN KEY (arbeidsforhold_id) REFERENCES arbeidsforhold;

CREATE TABLE permisjon (
    id                NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    arbeidsforhold_id NUMBER(19) NOT NULL,
    permisjon_id      VARCHAR2(20 CHAR),
    startdato         DATE,
    sluttdato         DATE,
    prosent           NUMBER,
    endret            TIMESTAMP(0),
    CONSTRAINT pk_permisjon PRIMARY KEY (id)
);

ALTER TABLE permisjon ADD CONSTRAINT fk_permisjon_forhold FOREIGN KEY (arbeidsforhold_id) REFERENCES arbeidsforhold;

CREATE TABLE utenlandsopphold (
    id                NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    arbeidsforhold_id NUMBER(19) NOT NULL,
    land              VARCHAR2(50 CHAR),
    startdato         DATE,
    sluttdato         DATE,
    CONSTRAINT pk_utenlandsopphold PRIMARY KEY (id)
);

ALTER TABLE utenlandsopphold ADD CONSTRAINT fk_opphold_forhold FOREIGN KEY (arbeidsforhold_id) REFERENCES arbeidsforhold;