CREATE TABLE vedtak (
    behandling_id     NUMBER(19) NOT NULL,
    vedtak_dato       TIMESTAMP  NOT NULL,
    CONSTRAINT pk_vedtak PRIMARY KEY (behandling_id)
);

ALTER TABLE vedtak
    ADD CONSTRAINT fk_vedtak_behandling FOREIGN KEY (behandling_id) REFERENCES behandling;
