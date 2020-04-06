DELETE FROM avklartefakta WHERE beh_resultat_id in (3, 4, 5) AND referanse = 'YRKESAKTIVITET_ANTALL_LAND';

UPDATE behandling SET beh_tema = 'ARBEID_FLERE_LAND' WHERE id in (3, 4, 5);
