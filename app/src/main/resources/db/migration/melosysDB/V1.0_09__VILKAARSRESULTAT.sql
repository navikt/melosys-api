CREATE TABLE vilkaarsresultat (
    id                   NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    beh_resultat_id      NUMBER(19) NOT NULL,
    vilkaar              VARCHAR2(99) NOT NULL,
    oppfylt              NUMBER(1) NOT NULL,
    begrunnelse          VARCHAR2(99) NOT NULL,
    begrunnelse_fritekst VARCHAR2(4000) NULL,
    CONSTRAINT pk_vilkaarsresultat PRIMARY KEY (id)
);