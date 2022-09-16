UPDATE FAGSAK_TYPE
SET NAVN = 'Saken skal behandles etter en bilaterale trygdeavtale'
WHERE KODE = 'TRYGDEAVTALE';

UPDATE FAGSAK_TYPE
SET NAVN = 'Saken skal behandles etter folketrygdloven'
WHERE KODE = 'FTRL';
