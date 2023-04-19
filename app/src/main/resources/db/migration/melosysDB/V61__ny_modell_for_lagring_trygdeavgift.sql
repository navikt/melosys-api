CREATE TABLE trygdeavgiftsgrunnlaget
(
    id                                      NUMBER(19)  GENERATED ALWAYS AS IDENTITY,
    fastsatt_trygdeavgift_id                NUMBER(19)  NOT NULL,
    CONSTRAINT pk_trygdeavgiftsgrunnlaget   PRIMARY KEY (id)
);

ALTER TABLE trygdeavgiftsgrunnlaget ADD CONSTRAINT fk_trygdeavgiftsgrunnlaget_fastsatt_trygdeavgift FOREIGN KEY (fastsatt_trygdeavgift_id) REFERENCES fastsatt_trygdeavgift;
CREATE UNIQUE INDEX idx_trygdeavgiftsgrunnlaget_fastsatt_trygdeavgift_unik ON trygdeavgiftsgrunnlaget (fastsatt_trygdeavgift_id);


CREATE TABLE inntektskilde
(
    id                                      NUMBER(19)       GENERATED ALWAYS AS IDENTITY,
    trygdeavgiftsgrunnlaget_id              NUMBER(19)       NOT NULL,
    fom_dato                                DATE             NOT NULL,
    tom_dato                                DATE             NOT NULL,
    inntektskilde_type                      VARCHAR2(99)     NOT NULL,
    avgiftspliktig_inntekt_mnd              DECIMAL(12,2),
    arbeidsgiversavgift_betales_til_skatt   NUMBER(1),
    trygdeavgift_betales_til_skatt          NUMBER(1),
    CONSTRAINT pk_inntektskilde             PRIMARY KEY (id)
);

ALTER TABLE inntektskilde ADD CONSTRAINT fk_inntektskilde_trygdeavgiftsgrunnlaget FOREIGN KEY (trygdeavgiftsgrunnlaget_id) REFERENCES trygdeavgiftsgrunnlaget;
CREATE INDEX idx_inntektskilde_trygdeavgiftsgrunnlaget ON inntektskilde (trygdeavgiftsgrunnlaget_id);


CREATE TABLE skatteforhold_til_norge
(
    id                                      NUMBER(19)      GENERATED ALWAYS AS IDENTITY,
    trygdeavgiftsgrunnlaget_id              NUMBER(19)      NOT NULL,
    fom_dato                                DATE            NOT NULL,
    tom_dato                                DATE            NOT NULL,
    skatteplikt_type                        VARCHAR2(99)    NOT NULL,
    CONSTRAINT pk_skatteforhold_til_norge   PRIMARY KEY (id)
);

ALTER TABLE skatteforhold_til_norge ADD CONSTRAINT fk_skatteforhold_til_norge_trygdeavgiftsgrunnlaget FOREIGN KEY (trygdeavgiftsgrunnlaget_id) REFERENCES trygdeavgiftsgrunnlaget;
CREATE INDEX idx_skatteforhold_til_norge_trygdeavgiftsgrunnlaget ON skatteforhold_til_norge (trygdeavgiftsgrunnlaget_id);


ALTER TABLE medlem_av_folketrygden ADD bestemmelse VARCHAR2(99);
UPDATE medlem_av_folketrygden SET bestemmelse = 'FTRL_KAP2_2_8_FØRSTE_LEDD_A';
ALTER TABLE medlem_av_folketrygden MODIFY bestemmelse VARCHAR2(99) NOT NULL;
