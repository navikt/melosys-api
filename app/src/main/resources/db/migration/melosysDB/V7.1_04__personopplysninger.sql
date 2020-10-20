CREATE TABLE personopplysning (
    id              NUMBER(19)  GENERATED ALWAYS AS IDENTITY,
    behandling_id   NUMBER(19)  NOT NULL,
    person          CLOB        NOT NULL,
    CONSTRAINT pk_personopplysning PRIMARY KEY (id),
    CONSTRAINT fk_personopplysning_behandling FOREIGN KEY (behandling_id) REFERENCES behandling
);

CREATE TABLE personopplysning_kilde (
    id                  NUMBER(19)  GENERATED ALWAYS AS IDENTITY,
    personopplysning_id NUMBER(19)  NOT NULL,
    dokument_xml        XMLTYPE     NOT NULL,
    CONSTRAINT pk_personopplysning_kilde PRIMARY KEY (id),
    CONSTRAINT fk_personopplysning_kilde_personopplysning FOREIGN KEY (personopplysning_id) REFERENCES personopplysning
);
