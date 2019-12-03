CREATE TABLE vedtak_metadata (
    behandlingsresultat_id NUMBER(19)      NOT NULL,
    vedtak_dato            TIMESTAMP       NULL,
    vedtak_klagefrist      DATE            NULL,
    vedtak_type            VARCHAR2(99)    NOT NULL,
    revurder_begrunnelse   VARCHAR2(4000)  NULL, 
    registrert_dato        TIMESTAMP       NOT NULL,
    endret_dato            TIMESTAMP       NOT NULL,
    registrert_av          VARCHAR2(99)    NULL,
    endret_av              VARCHAR2(99)    NULL,
    CONSTRAINT pk_vedtak_metadata PRIMARY KEY (behandlingsresultat_id)
);

CREATE INDEX idx_vedtaks_type ON vedtak_metadata(vedtak_type);

CREATE TABLE vedtak_type (
    kode        VARCHAR2(99)  NOT NULL,
    navn        VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_vedtak_type PRIMARY KEY (kode)
);
INSERT INTO vedtak_type (kode, navn) VALUES ('FØRSTEGANGSVEDTAK', 'Førstegangsvedtak for en sak');
INSERT INTO vedtak_type (kode, navn) VALUES ('KORRIGERT_VEDTAK', 'Korrigering av vedtak, som ikke har betydning for utfallet av saken');
INSERT INTO vedtak_type (kode, navn) VALUES ('OMGJØRINGSVEDTAK', 'Omgjøring av vedtak for en sak');

ALTER TABLE vedtak_metadata ADD CONSTRAINT fk_vedtak_metadata_type FOREIGN KEY (vedtak_type) REFERENCES vedtak_type;
ALTER TABLE vedtak_metadata ADD CONSTRAINT fk_vedtak_behandlingsresultat_id FOREIGN KEY (behandlingsresultat_id) REFERENCES behandlingsresultat;

INSERT INTO vedtak_metadata (behandlingsresultat_id, vedtak_dato, vedtak_klagefrist, vedtak_type) 
    SELECT br.behandling_id, br.vedtak_dato, br.vedtak_klagefrist, 'FØRSTEGANGSVEDTAK' 
    FROM behandlingsresultat br
    WHERE br.vedtak_dato IS NOT NULL OR br.vedtak_klagefrist IS NOT NULL;

ALTER TABLE behandlingsresultat DROP (vedtak_dato, vedtak_klagefrist);
