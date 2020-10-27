-- FIXME: NOT NULL etter migrering
ALTER TABLE SAKSOPPLYSNING ADD (
    DOKUMENT CLOB,
    CONSTRAINT dokument_json_constraint CHECK (DOKUMENT IS JSON) ENABLE
);

-- FIXME: Fjern dokument_xml (+ kilde?) fra saksopplysning
-- FIXME: Endre saksopplysning_kilde til f.eks. saksopplysning_kildesystem
CREATE TABLE saksopplysning_dokument_kilde (
    id                  NUMBER(19)      GENERATED ALWAYS AS IDENTITY,
    saksopplysning_id   NUMBER(19)      NOT NULL,
    kilde               VARCHAR2(99)    NOT NULL,
    mottatt_dokument    CLOB            NOT NULL,
    CONSTRAINT pk_saksopplysning_dokument_kilde PRIMARY KEY (id),
    CONSTRAINT fk_saksopplysning_dokument_kilde_saksopplysning FOREIGN KEY (saksopplysning_id) REFERENCES saksopplysning,
    CONSTRAINT fk_saksopplysning_dokument_kilde_kilde FOREIGN KEY (kilde) REFERENCES saksopplysning_kilde
);
