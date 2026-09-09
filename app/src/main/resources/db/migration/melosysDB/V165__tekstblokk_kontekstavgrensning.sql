CREATE TABLE TEKSTBLOKK_SAKSTYPE
(
    tekstblokk_id NUMBER(19)   NOT NULL,
    sakstype      VARCHAR2(30) NOT NULL,
    CONSTRAINT pk_tekstblokk_sakstype PRIMARY KEY (tekstblokk_id, sakstype),
    CONSTRAINT fk_tekstblokk_sakstype_tekstblokk FOREIGN KEY (tekstblokk_id) REFERENCES TEKSTBLOKK (id) ON DELETE CASCADE
);

CREATE INDEX idx_tekstblokk_sakstype_sakstype ON TEKSTBLOKK_SAKSTYPE(sakstype);

CREATE TABLE TEKSTBLOKK_BEHANDLINGSTEMA
(
    tekstblokk_id   NUMBER(19)   NOT NULL,
    behandlingstema VARCHAR2(60) NOT NULL,
    CONSTRAINT pk_tekstblokk_behandlingstema PRIMARY KEY (tekstblokk_id, behandlingstema),
    CONSTRAINT fk_tekstblokk_behtema_tekstblokk FOREIGN KEY (tekstblokk_id) REFERENCES TEKSTBLOKK (id) ON DELETE CASCADE
);

CREATE INDEX idx_tekstblokk_behtema_behtema ON TEKSTBLOKK_BEHANDLINGSTEMA(behandlingstema);
