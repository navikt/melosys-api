CREATE TABLE personopplysning (
    id              NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    behandling_id   NUMBER(19)  NOT NULL,
    person          CLOB        NOT NULL,
    CONSTRAINT pk_personopplysning PRIMARY KEY (id)
);

ALTER TABLE personopplysning ADD CONSTRAINT fk_personopplysning_behandling FOREIGN KEY (behandling_id) REFERENCES behandling;
