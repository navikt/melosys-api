CREATE TABLE medlem_av_folketrygden
(
    id                                  NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    beh_resultat_id                     NUMBER(19) NOT NULL,
    trygdeavgift_nav_norsk_inntekt      VARCHAR2(99),
    trygdeavgift_nav_utenlandsk_inntekt VARCHAR2(99),
    CONSTRAINT pk_medlem_av_folketrygden PRIMARY KEY (id)
);

ALTER TABLE medlem_av_folketrygden
    ADD CONSTRAINT fk_medlemfolketrygd_beh_resultat FOREIGN KEY (beh_resultat_id) REFERENCES behandlingsresultat;
CREATE UNIQUE INDEX idx_medlemfolketrygd_behandling_unik ON medlem_av_folketrygden (beh_resultat_id);

ALTER TABLE medlemskapsperiode DROP CONSTRAINT fk_medlemskapsresultat;
ALTER INDEX idx_medlemskapsperiode_resultat RENAME TO idx_medlemskapsperiode_medlem_folketrygd;
ALTER TABLE medlemskapsperiode RENAME COLUMN beh_resultat_id TO medlem_av_folketrygden_id;
ALTER TABLE medlemskapsperiode
    ADD CONSTRAINT fk_medlemskapsperiode_medlem_folketrygd FOREIGN KEY (medlem_av_folketrygden_id) REFERENCES medlem_av_folketrygden;