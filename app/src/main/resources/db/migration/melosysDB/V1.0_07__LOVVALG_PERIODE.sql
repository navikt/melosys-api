CREATE TABLE lovvalg_periode (
    id                      NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    beh_resultat_id         NUMBER(19)     NOT NULL,
    fom_dato                DATE           NOT NULL,
    tom_dato                DATE           NOT NULL,
    lovvalgsland            VARCHAR2(99)   NOT NULL,
    lovvalg_bestemmelse     VARCHAR2(99)   NOT NULL,
    unntak_fra_lovvalgsland VARCHAR(99)    NULL,
    unntak_fra_bestemmelse  VARCHAR(99)    NULL,    
    innvilgelse_resultat    VARCHAR2(99)   NOT NULL,
    medlemskapstype         VARCHAR2(99)   NOT NULL,
    trygde_dekning          VARCHAR2(99)   NULL,
    medlperiode_id          NUMBER(19)     NULL,
    CONSTRAINT pk_lovvalg_periode PRIMARY KEY (id)
    
);

ALTER TABLE lovvalg_periode
    ADD CONSTRAINT fk_periode_beh_resultat FOREIGN KEY (beh_resultat_id) REFERENCES behandlingsresultat;
    
CREATE UNIQUE INDEX idx_periode_unik_i_beh ON lovvalg_periode(beh_resultat_id, fom_dato, tom_dato);    
ALTER TABLE lovvalg_periode
    ADD CONSTRAINT periode_unik_i_beh UNIQUE(beh_resultat_id, fom_dato, tom_dato) using index idx_periode_unik_i_beh;