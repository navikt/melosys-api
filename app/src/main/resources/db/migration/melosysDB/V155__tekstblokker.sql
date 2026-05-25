CREATE TABLE TEKSTBLOKK
(
    id              NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    tittel          VARCHAR2(200)  NOT NULL,
    innhold         CLOB           NOT NULL,
    type            VARCHAR2(20)   NOT NULL,
    registrert_dato TIMESTAMP      NOT NULL,
    endret_dato     TIMESTAMP      NOT NULL,
    registrert_av   VARCHAR2(99)   NULL,
    endret_av       VARCHAR2(99)   NULL,
    CONSTRAINT pk_tekstblokk PRIMARY KEY (id)
);

CREATE INDEX idx_tekstblokk_type ON TEKSTBLOKK(type);

CREATE TABLE TEKSTBLOKK_TAG
(
    tekstblokk_id NUMBER(19)   NOT NULL,
    tag           VARCHAR2(60) NOT NULL,
    CONSTRAINT pk_tekstblokk_tag PRIMARY KEY (tekstblokk_id, tag),
    CONSTRAINT fk_tekstblokk_tag_tekstblokk FOREIGN KEY (tekstblokk_id) REFERENCES TEKSTBLOKK (id) ON DELETE CASCADE
);

CREATE INDEX idx_tekstblokk_tag_tag ON TEKSTBLOKK_TAG(tag);
