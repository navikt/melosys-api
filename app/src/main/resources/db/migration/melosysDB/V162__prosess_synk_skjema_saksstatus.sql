-- Ny prosesstype og prosessteg for synk av saksstatus til melosys-skjema-api
INSERT INTO PROSESS_TYPE(KODE, NAVN) VALUES ('SYNK_SKJEMA_SAKSSTATUS', 'Synkroniserer saksstatus til melosys-skjema-api');

INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('SYNK_SKJEMA_SAKSSTATUS', 'Synkroniserer saksstatus til melosys-skjema-api');
