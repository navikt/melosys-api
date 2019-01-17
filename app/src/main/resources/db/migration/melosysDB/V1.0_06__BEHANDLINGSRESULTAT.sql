CREATE TABLE behandlingsresultat (
    behandling_id         NUMBER(19) NOT NULL,
    behandlingsmaate      VARCHAR2(99) NOT NULL,
    resultat_type         VARCHAR2(99) NULL,
    fastsatt_av_land      VARCHAR2(99) NULL,
    henleggelse_grunn     VARCHAR2(99) NULL,
    henleggelse_fritekst  VARCHAR2(4000)  NULL,
    vedtak_dato           TIMESTAMP NULL,
    vedtak_klagefrist     DATE NULL,
    registrert_dato       TIMESTAMP NOT NULL,
    registrert_av         VARCHAR2(99) NULL,
    endret_dato           TIMESTAMP NOT NULL,
    endret_av             VARCHAR2(99) NULL,
    CONSTRAINT pk_resultat PRIMARY KEY (behandling_id)
);

CREATE INDEX idx_behandlingsmaate ON behandlingsresultat(behandlingsmaate);
CREATE INDEX idx_behandlingsresultat_type ON behandlingsresultat(resultat_type);

ALTER TABLE behandlingsresultat
    ADD CONSTRAINT fk_resultat_behandling FOREIGN KEY (behandling_id) REFERENCES behandling;

CREATE TABLE behandlingsmaate (
    kode        VARCHAR2(99)  NOT NULL,
    navn        VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_behandlingsmaate PRIMARY KEY (kode)
);
INSERT INTO behandlingsmaate (kode, navn) VALUES ('AUTOMATISERT', 'Automatisert');
INSERT INTO behandlingsmaate (kode, navn) VALUES ('MANUELT', 'Manuelt');
INSERT INTO behandlingsmaate (kode, navn) VALUES ('DELVIS_AUTOMATISERT', 'Delvis automatisert');
INSERT INTO behandlingsmaate (kode, navn) VALUES ('UDEFINERT', 'Udefinert');

ALTER TABLE behandlingsresultat ADD CONSTRAINT fk_behandlingsmaate FOREIGN KEY (behandlingsmaate) REFERENCES behandlingsmaate;

CREATE TABLE behandlingsresultat_type (
    kode        VARCHAR2(99)  NOT NULL,
    navn        VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_behandlingsresultat_type PRIMARY KEY (kode)
);
INSERT INTO behandlingsresultat_type (kode, navn) VALUES ('ANMODNING_OM_UNNTAK', 'Anmodning om unntak');
INSERT INTO behandlingsresultat_type (kode, navn) VALUES ('HENLEGGELSE', 'Saken er henlagt');
INSERT INTO behandlingsresultat_type (kode, navn) VALUES ('FASTSATT_LOVVALGSLAND', 'Lovvalgsland er fastsatt');
INSERT INTO behandlingsresultat_type (kode, navn) VALUES ('FORELOEPIG_FASTSATT_LOVVALGSLAND', 'Lovvalgsland er foreløpig fastsatt');
INSERT INTO behandlingsresultat_type (kode, navn) VALUES ('IKKE_FASTSATT', 'Ikke fastsatt');

ALTER TABLE behandlingsresultat ADD CONSTRAINT fk_behandlingsresultat_type FOREIGN KEY (resultat_type) REFERENCES behandlingsresultat_type;