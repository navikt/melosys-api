CREATE TABLE behandling_dvh (
  trans_id        NUMBER(19)    NOT NULL,
  id              NUMBER(19)    NOT NULL,
  CONSTRAINT pk_behandling_dvh PRIMARY KEY (trans_id)
);

CREATE SEQUENCE behandling_dvh_seq
MINVALUE 1
NOMAXVALUE
INCREMENT BY 1;
