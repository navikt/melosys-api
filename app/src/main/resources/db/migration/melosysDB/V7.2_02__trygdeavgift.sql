CREATE TABLE fastsatt_trygdeavgift
(
    id                                    NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    medlem_av_folketrygden_id             NUMBER(19) NOT NULL,
    type                                  VARCHAR2(99),
    betales_av                            VARCHAR2(99),
    representant_nr                       VARCHAR2(99),
    avgiftspliktig_norsk_inntekt_mnd      NUMBER(19),
    avgiftspliktig_utenlandsk_inntekt_mnd NUMBER(19),
    CONSTRAINT pk_fastsatt_trygdeavgift PRIMARY KEY (id)
);

ALTER TABLE fastsatt_trygdeavgift
    ADD CONSTRAINT fk_fastsatt_trygdeavgift_medlem_folketrygd FOREIGN KEY (medlem_av_folketrygden_id) REFERENCES medlem_av_folketrygden;
CREATE UNIQUE INDEX idx_fastsatt_trygdeavgift_medlem_folketrygd_unik ON fastsatt_trygdeavgift (medlem_av_folketrygden_id);

CREATE TABLE trygdeavgift
(
    id                          NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    medlemskapsperiode_id       NUMBER(19) NOT NULL,
    type                        VARCHAR2(99) NOT NULL,
    trygdesats                  NUMBER(5,1) NOT NULL,
    avgiftskode                 VARCHAR2(20) NOT NULL,
    CONSTRAINT pk_trygdeavgift PRIMARY KEY (id)
);

ALTER TABLE trygdeavgift
    ADD CONSTRAINT fk_trygdeavgift_medlemskapsperiode FOREIGN KEY (medlemskapsperiode_id) REFERENCES medlemskapsperiode;
CREATE INDEX idx_trygdeavgift_periode ON trygdeavgift (medlemskapsperiode_id);
