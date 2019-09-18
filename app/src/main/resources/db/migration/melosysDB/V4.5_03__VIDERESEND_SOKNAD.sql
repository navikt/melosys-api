INSERT INTO prosess_type (kode, navn) VALUES ('VIDERESEND_SOKNAD', 'Videresend søknad');

INSERT INTO prosess_steg (kode, navn) VALUES ('VS_OPPDATER_RESULTAT', 'Oppdatering av behandlingsresultat');
INSERT INTO prosess_steg (kode, navn) VALUES ('VS_SEND_ORIENTERINGSBREV', 'Opprett orienteringsbrev');
INSERT INTO prosess_steg (kode, navn) VALUES ('VS_SEND_SOKNAD', 'Opprett brev med vedlagt søknad');


INSERT INTO FAGSAK_STATUS (kode, navn) VALUES ('VIDERESENDT', 'Saken er videresendt');
UPDATE FAGSAK SET STATUS = 'VIDERESENDT' WHERE STATUS = 'AVSLUTTET';
DELETE FROM FAGSAK_STATUS WHERE KODE = 'AVSLUTTET';