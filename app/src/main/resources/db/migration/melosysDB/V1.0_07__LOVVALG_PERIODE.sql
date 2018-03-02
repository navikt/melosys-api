CREATE TABLE lovvalg_periode (
    id             NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    vedtak_id      NUMBER(19)     NOT NULL,
    fom_dato       DATE           NOT NULL,
    tom_dato       DATE           NOT NULL,
    bestemmelse    VARCHAR2(99)   NOT NULL,
    lovvalgsland   VARCHAR2(99)   NOT NULL,
    dekning        VARCHAR2(99)   NOT NULL,
    versjon        VARCHAR2(99)   NOT NULL,
    grunnlag_xml   XMLTYPE        NOT NULL,
    resultat_xml   XMLTYPE        NOT NULL,
    CONSTRAINT pk_fastsatt_rettighet PRIMARY KEY (id)
);

ALTER TABLE lovvalg_periode
    ADD CONSTRAINT fk_lovvalg_periode_vedtak FOREIGN KEY (vedtak_id) REFERENCES vedtak;

CREATE TABLE lovvalg_bestemmelse (
    kode        VARCHAR2(99) NOT NULL,
    navn        VARCHAR2(99) NOT NULL,
    CONSTRAINT pk_lovvalg_bestemmelse PRIMARY KEY (kode)
);

INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_11_1', 'ART_11_1');
INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_11_2', 'ART_11_2');
INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_11_3A', 'ART_11_3A');
INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_11_3B', 'ART_11_3B');
INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_11_3C', 'ART_11_3C');
INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_11_3D', 'ART_11_3D');
INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_11_3E', 'ART_11_3E');
INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_12_1', 'ART_12_1');
INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_12_2', 'ART_12_2');
INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_13_1A', 'ART_13_1A');
INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_13_1B1', 'ART_13_1B1');
INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_13_1B2', 'ART_13_1B2');
INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_13_1B3', 'ART_13_1B3');
INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_13_1B4', 'ART_13_1B4');
INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_13_2A', 'ART_13_2A');
INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_13_2B', 'ART_13_2B');
INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_16_1', 'ART_16_1');
INSERT INTO lovvalg_bestemmelse (kode, navn) VALUES ('ART_16_2', 'ART_16_2');

CREATE TABLE lovvalg_dekning (
    kode        VARCHAR2(99) NOT NULL,
    navn        VARCHAR2(99) NOT NULL,
    CONSTRAINT pk_lovvalg_dekning PRIMARY KEY (kode)
);

INSERT INTO lovvalg_dekning (kode, navn) VALUES ('OMFATTET', 'OMFATTET');
INSERT INTO lovvalg_dekning (kode, navn) VALUES ('IKKE_DEKKET', 'IKKE_DEKKET');
INSERT INTO lovvalg_dekning (kode, navn) VALUES ('UNTATT', 'UNTATT');

ALTER TABLE lovvalg_periode ADD CONSTRAINT fk_lovvalg_periode_bestemmelse FOREIGN KEY (bestemmelse) REFERENCES lovvalg_bestemmelse;
ALTER TABLE lovvalg_periode ADD CONSTRAINT fk_lovvalg_periode_dekning FOREIGN KEY (dekning) REFERENCES lovvalg_dekning;
