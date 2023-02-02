CREATE TABLE UTKAST_BREV
(
    id                      NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    behandling_id           NUMBER(19)   NOT NULL,
    lagringsdato            TIMESTAMP    NOT NULL,
    lagret_av_saksbehandler VARCHAR2(99) NOT NULL,
    brevbestilling_utkast   CLOB         NOT NULL,
    CONSTRAINT pk_utkast_brev PRIMARY KEY (id),
    CONSTRAINT ENSURE_JSON CHECK (brevbestilling_utkast IS JSON)
);

CREATE INDEX idx_utkastbrev_behandling ON UTKAST_BREV (behandling_id);
