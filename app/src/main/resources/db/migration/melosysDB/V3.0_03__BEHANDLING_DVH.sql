CREATE TABLE behandling_dvh (
  trans_id        NUMBER(19)    NOT NULL,
  trans_tid       TIMESTAMP(3)  NOT NULL,
  funksjonell_tid TIMESTAMP(3)  NOT NULL,
  endret_av       VARCHAR2(20 CHAR),
  id              NUMBER(19)    NOT NULL,
  saksnummer      VARCHAR2(99)  NOT NULL,
  status          VARCHAR2(99)  NOT NULL,
  beh_type        VARCHAR2(99)  NOT NULL,
  registrert_dato TIMESTAMP     NOT NULL,
  endret_dato     TIMESTAMP     NOT NULL,
  CONSTRAINT pk_behandling_dvh PRIMARY KEY (trans_id)
);

CREATE SEQUENCE behandling_dvh_seq
MINVALUE 1
NOMAXVALUE
INCREMENT BY 1;
