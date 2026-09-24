ALTER TABLE utenlandsk_myndighet ADD (gateadresse_3 VARCHAR2(99) NULL);

UPDATE utenlandsk_myndighet
SET NAVN          = 'Program Policy and Data Exchange, Agreements and Notices Coordination',
    GATEADRESSE_1 = 'Social Security Administration',
    GATEADRESSE_2 = '4170 Annex Building',
    GATEADRESSE_3 = '6401 Security Blvd'
WHERE ID = 34
  AND LAND = 'USA';
