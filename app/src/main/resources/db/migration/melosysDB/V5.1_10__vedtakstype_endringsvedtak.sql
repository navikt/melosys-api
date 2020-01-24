INSERT INTO vedtak_type (kode, navn) VALUES ('ENDRINGSVEDTAK', 'Endring av et eksisterende vedtak');

UPDATE vedtak_metadata SET vedtak_type = 'ENDRINGSVEDTAK' WHERE behandlingsresultat_id IN (SELECT id FROM BEHANDLING WHERE BEH_TYPE = 'ENDRET_PERIODE');