-- FIXME: NOT NULL etter migrering
ALTER TABLE SAKSOPPLYSNING ADD DOKUMENT CLOB;

-- FIXME: Fjern dokument_xml (+ kilde?) fra saksopplysning
CREATE TABLE saksopplysning_dokument_kilde (
    id                  NUMBER(19)      GENERATED ALWAYS AS IDENTITY,
    saksopplysning_id   NUMBER(19)      NOT NULL,
    kilde               VARCHAR2(99)    NOT NULL,
    dokument_xml        XMLTYPE         NOT NULL,
    CONSTRAINT pk_personopplysning_kilde PRIMARY KEY (id),
    CONSTRAINT fk_saksopplysning_dokument_kilde_saksopplysning FOREIGN KEY (saksopplysning_id) REFERENCES saksopplysning,
    CONSTRAINT fk_saksopplysning_dokument_kilde_kilde FOREIGN KEY (kilde) REFERENCES saksopplysning_kilde
);
