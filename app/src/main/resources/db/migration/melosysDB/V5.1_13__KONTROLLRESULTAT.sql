CREATE TABLE kontrollresultat(
    id              NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    beh_resultat_id NUMBER(19)   NOT NULL,
    begrunnelse     VARCHAR2(99) NOT NULL,
    CONSTRAINT pk_kontrollresultat PRIMARY KEY (id),
    CONSTRAINT fk_kontrollresultat_behandlingsresultat FOREIGN KEY (beh_resultat_id) REFERENCES behandlingsresultat ON DELETE CASCADE
);

CREATE INDEX idx_kontrollresultat_behandlingsresultat ON kontrollresultat (beh_resultat_id);

INSERT INTO kontrollresultat (beh_resultat_id, begrunnelse)
SELECT beh_resultat_id, begrunnelse
FROM avklartefakta a
         JOIN avklartefakta_registrering ar ON a.id = ar.avklartefakta_id
WHERE a.referanse = 'VURDERING_UNNTAK_PERIODE';

DELETE
FROM avklartefakta
where referanse = 'VURDERING_UNNTAK_PERIODE';