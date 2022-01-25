INSERT INTO ROLLE_TYPE (kode, navn)
VALUES ('TRYGDEMYNDIGHET', 'Trygdemyndigheten det sendes til og/eller mottas dokumentasjon fra i saken.');

UPDATE AKTOER
SET ROLLE = 'TRYGDEMYNDIGHET'
WHERE ROLLE = 'MYNDIGHET';

DELETE FROM ROLLE_TYPE
WHERE KODE = 'MYNDIGHET';
