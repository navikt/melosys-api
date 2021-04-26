INSERT INTO PROSESS_TYPE(KODE, NAVN) VALUES('IVERKSETT_VEDTAK_FTRL','Iverksett nytt vedtak Folketrygdeloven');
INSERT INTO PROSESS_STEG (kode, navn) VALUES ('LAGRE_MEDLEMSPERIODE_MEDL', 'Lagrer en medlemsperiode (Folketrygden) i MEDL som endelig');
INSERT INTO PROSESS_STEG (kode, navn) VALUES ('PUBLISER_VEDTAK', 'Publiserer vedtak på Kafka');
