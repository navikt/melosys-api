CREATE TABLE personopplysning (
    id              NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    behandling_id   NUMBER(19)      NOT NULL,
    fnr             VARCHAR2(99)    NOT NULL,
    sivilstand      VARCHAR2(99)    NOT NULL,
    kjoenn          VARCHAR2(99)    NOT NULL,
    fornavn         VARCHAR2(99)    NULL,
    mellomnavn      VARCHAR2(99)    NULL,
    etternavn       VARCHAR2(99)    NULL,
    sammensatt_navn VARCHAR2(99)    NOT NULL,
    foedselsdato    TIMESTAMP       NOT NULL,
    doedsdato       TIMESTAMP       NULL,
    diskresjonskode VARCHAR2(99)    NULL,
    personstatus    VARCHAR2(99)    NOT NULL,
    CONSTRAINT pk_personopplysning PRIMARY KEY (id)
);

CREATE TABLE familiemedlem (
    id                  NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    personopplysning_id NUMBER(19)      NOT NULL,
    fnr                 VARCHAR2(99)    NOT NULL,
    navn                VARCHAR2(99)    NOT NULL,
    familierelasjon     VARCHAR2(99)    NOT NULL,
    foedselsdato        TIMESTAMP       NULL, -- lagrer bare for barn
    bor_med_bruker      NUMBER(1)       NOT NULL,
    sivilstand          VARCHAR2(99)    NULL,
    sivilstand_fom      TIMESTAMP       NULL,
    fnr_annen_forelder  VARCHAR2(99)    NULL,
    CONSTRAINT pk_familiemedlem PRIMARY KEY (id)
);

CREATE TABLE personstatus_type (
    kode        VARCHAR2(99)  NOT NULL,
    navn        VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_personstatus_type PRIMARY KEY (kode)
);

ALTER TABLE personopplysning ADD CONSTRAINT fk_personstatus_type FOREIGN KEY (personstatus) REFERENCES personstatus_type;
ALTER TABLE personopplysning ADD CONSTRAINT fk_personopplysning_behandling FOREIGN KEY (behandling_id) REFERENCES behandling;
ALTER TABLE familiemedlem ADD CONSTRAINT fk_familiemedlem_personopplysning FOREIGN KEY (personopplysning_id) REFERENCES personopplysning;

INSERT INTO personstatus_type VALUES ('ADNR', 'ADNR');
INSERT INTO personstatus_type VALUES ('UTPE', 'UTPE');
INSERT INTO personstatus_type VALUES ('BOSA', 'BOSA');
INSERT INTO personstatus_type VALUES ('UREG', 'UREG');
INSERT INTO personstatus_type VALUES ('ABNR', 'ABNR');
INSERT INTO personstatus_type VALUES ('UFUL', 'UFUL');
INSERT INTO personstatus_type VALUES ('UTVA', 'UTVA');
INSERT INTO personstatus_type VALUES ('FOSV', 'FOSV');
INSERT INTO personstatus_type VALUES ('DØDD', 'DØDD');
INSERT INTO personstatus_type VALUES ('DØD',  'DØD');
INSERT INTO personstatus_type VALUES ('UTAN', 'UTAN');
INSERT INTO personstatus_type VALUES ('FØDR', 'FØDR');

