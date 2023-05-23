CREATE TABLE trygdeavgiftsperiode
(
    id                             NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    fastsatt_trygdeavgift_id       NUMBER(19)     NOT NULL,
    periode_fra                    DATE           NOT NULL,
    periode_til                    DATE           NOT NULL,
    trygdeavgift_beloep_mnd_verdi  DECIMAL(12, 2) NOT NULL,
    trygdeavgift_beloep_mnd_valuta VARCHAR2(3)    NOT NULL,
    trygdesats                     DECIMAL(4, 2)  NOT NULL,
    inntektsperiode_id             NUMBER(19),
    medlemskapsperiode_id          NUMBER(19),
    skatteforhold_id               NUMBER(19),
    CONSTRAINT pk_trygdeavgiftsperiode PRIMARY KEY (id)
);

ALTER TABLE trygdeavgiftsperiode
    ADD CONSTRAINT fk_trygdeavgiftsperiode_fastsatt_trygdeavgift FOREIGN KEY (fastsatt_trygdeavgift_id) REFERENCES fastsatt_trygdeavgift;
CREATE INDEX idx_trygdeavgiftsperiode_fastsatt_trygdeavgift ON trygdeavgiftsperiode (fastsatt_trygdeavgift_id);
ALTER TABLE trygdeavgiftsperiode
    ADD CONSTRAINT fk_trygdeavgiftsperiode_inntektsperiode FOREIGN KEY (inntektsperiode_id) REFERENCES inntektsperiode;
CREATE INDEX idx_trygdeavgiftsperiode_inntektsperiode ON trygdeavgiftsperiode (inntektsperiode_id);
ALTER TABLE trygdeavgiftsperiode
    ADD CONSTRAINT fk_trygdeavgiftsperiode_medlemskapsperiode FOREIGN KEY (medlemskapsperiode_id) REFERENCES medlemskapsperiode;
CREATE INDEX idx_trygdeavgiftsperiode_medlemskapsperiode ON trygdeavgiftsperiode (medlemskapsperiode_id);
ALTER TABLE trygdeavgiftsperiode
    ADD CONSTRAINT fk_trygdeavgiftsperiode_skatteforhold_til_norge FOREIGN KEY (skatteforhold_id) REFERENCES skatteforhold_til_norge;
CREATE INDEX idx_trygdeavgiftsperiode_skatteforhold_til_norge ON trygdeavgiftsperiode (skatteforhold_id);


INSERT INTO trygdeavgiftsperiode(fastsatt_trygdeavgift_id, periode_fra, periode_til, trygdeavgift_beloep_mnd_verdi,
                                 trygdeavgift_beloep_mnd_valuta, trygdesats)
SELECT ft.id, t.periode_fra, t.periode_til, t.trygdeavgift_belop_md, 'NOK', t.trygdesats
from trygdeavgift t
         JOIN medlemskapsperiode m on t.medlemskapsperiode_id = m.id
         JOIN medlem_av_folketrygden maf on m.medlem_av_folketrygden_id = maf.id
         JOIN fastsatt_trygdeavgift ft on maf.id = ft.medlem_av_folketrygden_id
where t.periode_fra is not null
  and t.periode_til is not null;

DROP TABLE trygdeavgift CASCADE CONSTRAINTS;


ALTER TABLE fastsatt_trygdeavgift
    DROP COLUMN betales_av CASCADE CONSTRAINTS;
ALTER TABLE fastsatt_trygdeavgift
    DROP COLUMN representant_nr;
ALTER TABLE fastsatt_trygdeavgift
    DROP COLUMN avgiftspliktig_norsk_inntekt_md;
ALTER TABLE fastsatt_trygdeavgift
    DROP COLUMN avgiftspliktig_utenlandsk_inntekt_md;
