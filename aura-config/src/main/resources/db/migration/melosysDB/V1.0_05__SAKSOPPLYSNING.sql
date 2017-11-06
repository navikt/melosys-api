CREATE TABLE saksopplysning (
    id              NUMBER(19)     NOT NULL,
    behandling_id   NUMBER(19)     NOT NULL,
    opplysning_type VARCHAR2(99)   NOT NULL,
    versjon         VARCHAR2(99)   NOT NULL,
    kilde           VARCHAR2(99)   NOT NULL,
    registrert_dato TIMESTAMP      NOT NULL,
    dokument_xml    XMLTYPE        NOT NULL,
    CONSTRAINT pk_saksopplysning PRIMARY KEY (id)
);

CREATE TABLE saksopplysning_type (
    kode        VARCHAR2(99)  NOT NULL,
    navn        VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_saksopplysning_type PRIMARY KEY (kode)
);

INSERT INTO saksopplysning_type (kode, navn) VALUES ('ARBFORH', 'Arbeidsforhold');
INSERT INTO saksopplysning_type (kode, navn) VALUES ('INNTK', 'Inntekt');
INSERT INTO saksopplysning_type (kode, navn) VALUES ('ORG', 'Arbeidsgiver');
INSERT INTO saksopplysning_type (kode, navn) VALUES ('PERSOPL', 'Personopplysninger');
INSERT INTO saksopplysning_type (kode, navn) VALUES ('SØKNAD', 'Søknad');
INSERT INTO saksopplysning_type (kode, navn) VALUES ('MEDL', 'Medlemskap');


CREATE TABLE saksopplysning_kilde (
    kode        VARCHAR2(99)  NOT NULL,
    navn        VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_saksopplysning_kilde PRIMARY KEY (kode)
);

INSERT INTO saksopplysning_kilde (kode, navn) VALUES ('AAREG', 'Aa-registeret');
INSERT INTO saksopplysning_kilde (kode, navn) VALUES ('EREG', 'Enhetsregisteret');
INSERT INTO saksopplysning_kilde (kode, navn) VALUES ('INNTK', 'Inntektskomponenten');
INSERT INTO saksopplysning_kilde (kode, navn) VALUES ('TPS', 'Folkeregisteret');
INSERT INTO saksopplysning_kilde (kode, navn) VALUES ('MEDL', 'Medlemskapsunntak');

ALTER TABLE saksopplysning ADD CONSTRAINT fk_saksopplysning_behandling FOREIGN KEY (behandling_id) REFERENCES behandling;
ALTER TABLE saksopplysning ADD CONSTRAINT fk_saksopplysning_type FOREIGN KEY (opplysning_type) REFERENCES saksopplysning_type;
ALTER TABLE saksopplysning ADD CONSTRAINT fk_saksopplysning_kilde FOREIGN KEY (kilde) REFERENCES saksopplysning_kilde;
