ALTER TABLE utenlandsk_myndighet
RENAME COLUMN
    GATEADRESSE
TO
    GATEADRESSE_1;

ALTER TABLE utenlandsk_myndighet
ADD GATEADRESSE_2 VARCHAR2(99);

UPDATE utenlandsk_myndighet
set gateadresse_1 = 'North East England', gateadresse_2 = 'HM Revenue and Customs'
where id = 33;

UPDATE utenlandsk_myndighet
set gateadresse_1 = 'Social Security Administration', gateadresse_2 = '4700 Annex Building, 6401 Security Blvd'
where id =34;
