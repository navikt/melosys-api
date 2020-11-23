CREATE TABLE medlemskapsperiode
(
    id                      NUMBER(19)   GENERATED ALWAYS AS IDENTITY,
    beh_resultat_id         NUMBER(19)   NOT NULL,
    fom_dato                DATE         NOT NULL,
    tom_dato                DATE         NULL,
    arbeidsland             VARCHAR2(99) NOT NULL,
    bestemmelse             VARCHAR2(99) NOT NULL,
    innvilgelse_resultat    VARCHAR2(99) NOT NULL,
    medlemskapstype         VARCHAR2(99) NOT NULL,
    trygde_dekning          VARCHAR2(99) NOT NULL,
    medlperiode_id          NUMBER(19)   NULL,
    CONSTRAINT pk_medlemskapsperiode     PRIMARY KEY (id)
);

CREATE INDEX idx_medlemskapsperiode_resultat ON medlemskapsperiode(beh_resultat_id);
ALTER TABLE medlemskapsperiode ADD CONSTRAINT fk_medlemskapsresultat FOREIGN KEY (beh_resultat_id) REFERENCES behandlingsresultat;
