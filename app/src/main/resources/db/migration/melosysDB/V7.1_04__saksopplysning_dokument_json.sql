ALTER TABLE saksopplysning DROP CONSTRAINT fk_saksopplysning_kilde;
ALTER TABLE saksopplysning_kilde DROP CONSTRAINT pk_saksopplysning_kilde;
ALTER TABLE saksopplysning_kilde RENAME TO saksopplysning_kildesystem;
ALTER TABLE saksopplysning_kildesystem ADD CONSTRAINT pk_saksopplysning_kildesystem PRIMARY KEY (kode);

ALTER TABLE saksopplysning ADD (
    dokument    CLOB    NULL,
    CONSTRAINT dokument_json_constraint CHECK (dokument IS JSON) ENABLE
);

CREATE TABLE saksopplysning_kilde (
    id                  NUMBER(19)      GENERATED ALWAYS AS IDENTITY,
    saksopplysning_id   NUMBER(19)      NOT NULL,
    kildesystem         VARCHAR2(99)    NOT NULL,
    mottatt_dokument    CLOB            NOT NULL,
    CONSTRAINT pk_saksopplysning_kilde PRIMARY KEY (id),
    CONSTRAINT fk_saksopplysning_kilde_saksopplysning FOREIGN KEY (saksopplysning_id) REFERENCES saksopplysning,
    CONSTRAINT fk_saksopplysning_kilde_kildesystem FOREIGN KEY (kildesystem) REFERENCES saksopplysning_kildesystem
);
