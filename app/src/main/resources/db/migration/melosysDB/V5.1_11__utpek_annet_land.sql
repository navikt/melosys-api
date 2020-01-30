INSERT INTO PROSESS_TYPE (KODE, NAVN) VALUES ('UTPEK_LAND', 'Lovvalgslandet er ikke Norge, utpek annet land');

INSERT INTO PROSESS_STEG (KODE, NAVN) VALUES ('UL_SEND_BREV', 'Send brev til bruker om at vedkommende ikke er omfattet av norsk trygd');
INSERT INTO PROSESS_STEG (KODE, NAVN) VALUES ('UL_SEND_UTLAND', 'Send SED A003 eller brev til utenlandsk trygdemyndighet');
INSERT INTO PROSESS_STEG (KODE, NAVN) VALUES ('UL_DISTRIBUER_JOURNALPOST', 'Distribuerer (sender) journalposten dersom den ble opprettet');
INSERT INTO PROSESS_STEG (KODE, NAVN) VALUES ('UL_OPPDATER_MEDL', 'Oppdatering av medlemskap');
