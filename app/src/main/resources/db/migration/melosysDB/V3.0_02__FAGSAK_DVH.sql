CREATE TABLE fagsak_dvh (
  trans_id        NUMBER(19)    NOT NULL,
  saksnummer      VARCHAR2(99)  NOT NULL,
  CONSTRAINT pk_fagsak_dvh PRIMARY KEY (trans_id)
);

CREATE SEQUENCE fagsak_dvh_seq
MINVALUE 1
NOMAXVALUE
INCREMENT BY 1;