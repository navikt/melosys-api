CREATE TABLE faktagrunnlag (
    id                 NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    lovvalgperiode_id  NUMBER(19)   NOT NULL,
    saksopplysning_id  NUMBER(19)   NOT NULL,
    fakta_type            VARCHAR2(99) NOT NULL,
    CONSTRAINT pk_faktagrunnlag PRIMARY KEY (id)
);

ALTER TABLE faktagrunnlag
    ADD CONSTRAINT fk_faktagrunnlag_periode FOREIGN KEY (lovvalgperiode_id) REFERENCES lovvalg_periode;
ALTER TABLE faktagrunnlag
    ADD CONSTRAINT fk_faktagrunnlag_saksopl FOREIGN KEY (saksopplysning_id) REFERENCES saksopplysning;

CREATE TABLE fakta_type (
    kode        VARCHAR2(99) NOT NULL,
    navn        VARCHAR2(99) NOT NULL,
    CONSTRAINT pk_fakta_type PRIMARY KEY (kode)
);

INSERT INTO fakta_type (kode, navn) VALUES ('PERSONOPPLYSNING', 'Personopplysning');

ALTER TABLE faktagrunnlag ADD CONSTRAINT fk_faktagrunnlag_type FOREIGN KEY (fakta_type) REFERENCES fakta_type;
