INSERT INTO PROSESS_STEG (KODE, NAVN) VALUES ('VS_AVKLAR_MYNDIGHET', 'Avklaring av utenlandsk trygdemyndighet');
INSERT INTO PROSESS_STEG (KODE, NAVN) VALUES ('VS_DISTRIBUER_JOURNALPOST', 'Distribuerer (sender) journalposten dersom den ble opprettet');

UPDATE PROSESS_STEG SET NAVN = 'Opprett journalpost eller SED med søknad som vedlegg' WHERE KODE = 'VS_SEND_SOKNAD';
