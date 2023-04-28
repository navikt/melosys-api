CREATE TABLE trygdeavgiftsgrunnlag
(
    id                                  NUMBER(19)  GENERATED ALWAYS AS IDENTITY,
    fastsatt_trygdeavgift_id            NUMBER(19)  NOT NULL,
    CONSTRAINT pk_trygdeavgiftsgrunnlag PRIMARY KEY (id)
);

ALTER TABLE trygdeavgiftsgrunnlag ADD CONSTRAINT fk_trygdeavgiftsgrunnlag_fastsatt_trygdeavgift FOREIGN KEY (fastsatt_trygdeavgift_id) REFERENCES fastsatt_trygdeavgift;
CREATE UNIQUE INDEX idx_trygdeavgiftsgrunnlag_fastsatt_trygdeavgift_unik ON trygdeavgiftsgrunnlag (fastsatt_trygdeavgift_id);


CREATE TABLE inntektsperiode
(
    id                                  NUMBER(19)       GENERATED ALWAYS AS IDENTITY,
    trygdeavgiftsgrunnlag_id            NUMBER(19)       NOT NULL,
    fom_dato                            DATE             NOT NULL,
    tom_dato                            DATE             NOT NULL,
    inntektskilde_type                  VARCHAR2(99)     NOT NULL,
    avgiftspliktig_inntekt_mnd_verdi    DECIMAL(12,2),
    avgiftspliktig_inntekt_mnd_valuta   VARCHAR2(3),
    aga_betales_til_skatt               NUMBER(1),
    trygdeavgift_betales_til_skatt      NUMBER(1),
    CONSTRAINT pk_inntektsperiode       PRIMARY KEY (id)
);

ALTER TABLE inntektsperiode ADD CONSTRAINT fk_inntektsperiode_trygdeavgiftsgrunnlag FOREIGN KEY (trygdeavgiftsgrunnlag_id) REFERENCES trygdeavgiftsgrunnlag;
CREATE INDEX idx_inntektsperiode_trygdeavgiftsgrunnlag ON inntektsperiode (trygdeavgiftsgrunnlag_id);


CREATE TABLE skatteforhold_til_norge
(
    id                                      NUMBER(19)      GENERATED ALWAYS AS IDENTITY,
    trygdeavgiftsgrunnlag_id                NUMBER(19)      NOT NULL,
    fom_dato                                DATE            NOT NULL,
    tom_dato                                DATE            NOT NULL,
    skatteplikt_type                        VARCHAR2(99)    NOT NULL,
    CONSTRAINT pk_skatteforhold_til_norge   PRIMARY KEY (id)
);

ALTER TABLE skatteforhold_til_norge ADD CONSTRAINT fk_skatteforhold_til_norge_trygdeavgiftsgrunnlag FOREIGN KEY (trygdeavgiftsgrunnlag_id) REFERENCES trygdeavgiftsgrunnlag;
CREATE INDEX idx_skatteforhold_til_norge_trygdeavgiftsgrunnlag ON skatteforhold_til_norge (trygdeavgiftsgrunnlag_id);


ALTER TABLE medlem_av_folketrygden ADD bestemmelse VARCHAR2(99);
UPDATE medlem_av_folketrygden SET bestemmelse = 'FTRL_KAP2_2_8_FØRSTE_LEDD_A';
ALTER TABLE medlem_av_folketrygden MODIFY bestemmelse VARCHAR2(99) NOT NULL;


ALTER TABLE trygdeavgift RENAME TO trygdeavgift_deprecated;
ALTER TABLE trygdeavgift_deprecated RENAME CONSTRAINT pk_trygdeavgift to pk_trygdeavgift_deprecated;

CREATE TABLE trygdeavgiftsperiode
(
    id                                  NUMBER(19)      GENERATED ALWAYS AS IDENTITY,
    fastsatt_trygdeavgift_id            NUMBER(19)      NOT NULL,
    periode_fra                         DATE            NOT NULL,
    periode_til                         DATE            NOT NULL,
    trygdeavgift_beloep_mnd_verdi       DECIMAL(12,2)   NOT NULL,
    trygdeavgift_beloep_mnd_valuta      VARCHAR2(3)     NOT NULL,
    trygdesats                          DECIMAL(4,2)    NOT NULL,
    CONSTRAINT pk_trygdeavgiftsperiode  PRIMARY KEY (id)
);

ALTER TABLE trygdeavgiftsperiode
    ADD CONSTRAINT fk_trygdeavgiftsperiode_fastsatt_trygdeavgift FOREIGN KEY (fastsatt_trygdeavgift_id) REFERENCES fastsatt_trygdeavgift;
CREATE INDEX idx_trygdeavgiftsperiode_fastsatt_trygdeavgift ON trygdeavgiftsperiode (fastsatt_trygdeavgift_id);
