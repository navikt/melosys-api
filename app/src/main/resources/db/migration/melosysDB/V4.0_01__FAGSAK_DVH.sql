CREATE TABLE fagsak_dvh (
  trans_id        NUMBER(19)    NOT NULL,
  trans_tid       TIMESTAMP(3)  NOT NULL,
  funksjonell_tid TIMESTAMP(3)  NOT NULL,
  endret_av       VARCHAR2(20 CHAR),
  saksnummer      VARCHAR2(99)  NOT NULL,
  gsak_saksnummer NUMBER(19)    NULL,
  fagsak_type     VARCHAR2(99)  NULL,
  status          VARCHAR2(99)  NOT NULL,
  bruker_id       VARCHAR2(99)  NOT NULL,
  arbeidsgiver_id VARCHAR2(99)  NOT NULL,
  representant_id VARCHAR2(99)  NULL,
  registrert_dato TIMESTAMP     NOT NULL,
  endret_dato     TIMESTAMP     NOT NULL,
  CONSTRAINT pk_fagsak_dvh PRIMARY KEY (trans_id)
);

CREATE SEQUENCE fagsak_dvh_seq
MINVALUE 1
NOMAXVALUE
INCREMENT BY 1;