CREATE TABLE BEHANDLINGSAARSAK_TYPE
(
    kode        VARCHAR2(20)  NOT NULL,
    beskrivelse VARCHAR2(100) NOT NULL,
    CONSTRAINT pk_behandlingsaarsak_type PRIMARY KEY (kode)
);

INSERT INTO BEHANDLINGSAARSAK_TYPE(kode, beskrivelse) VALUES ('SØKNAD', 'Søknad');
INSERT INTO BEHANDLINGSAARSAK_TYPE(kode, beskrivelse) VALUES ('SED', 'SED');
INSERT INTO BEHANDLINGSAARSAK_TYPE(kode, beskrivelse) VALUES ('HENVENDELSE', 'Henvendelse');
INSERT INTO BEHANDLINGSAARSAK_TYPE(kode, beskrivelse) VALUES ('FRITEKST', 'Fritekst');
INSERT INTO BEHANDLINGSAARSAK_TYPE(kode, beskrivelse) VALUES ('ANNET', 'Annet');

CREATE TABLE BEHANDLINGSAARSAK
(
    behandling_id   NUMBER(19)   NOT NULL PRIMARY KEY,
    aarsak_type     VARCHAR2(20) NOT NULL,
    aarsak_fritekst VARCHAR2(50) NULL,
    mottak_dato     DATE NOT NULL,
    CONSTRAINT fk_behandlingsaarsak_behandling FOREIGN KEY (behandling_id) REFERENCES BEHANDLING (ID),
    CONSTRAINT fk_behandlingsaarsak_type FOREIGN KEY (aarsak_type) REFERENCES BEHANDLINGSAARSAK_TYPE (kode)
);
