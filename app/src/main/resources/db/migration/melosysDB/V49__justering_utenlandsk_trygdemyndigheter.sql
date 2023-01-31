-- Legger til Jersey og Isle of Man med samme adresse som Storbritannia
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse_1, gateadresse_2, postnummer, poststed, land, landkode)
VALUES(45, null, 'PT Operations', 'North East England', 'HM Revenue and Customs', 'BX9', '1AN', 'STORBRITANNIA', 'IM');

INSERT INTO utenlandsk_myndighet_pref(utenlandsk_myndighet_id, preferanse_id) VALUES(45, 1);


INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse_1, gateadresse_2, postnummer, poststed, land, landkode)
VALUES(46, null, 'PT Operations', 'North East England', 'HM Revenue and Customs', 'BX9', '1AN', 'STORBRITANNIA', 'JE');

INSERT INTO utenlandsk_myndighet_pref(utenlandsk_myndighet_id, preferanse_id) VALUES(46, 1);

-- Endringer i eksisterende rader, gjør alle land til uppercase for å være konsekvent
UPDATE utenlandsk_myndighet set land = 'CANADA' where id = 35;
UPDATE utenlandsk_myndighet set land = 'JEOLLABUK-DO, REPUBLIC OF KOREA' where id = 36;
UPDATE utenlandsk_myndighet set land = 'AUSTRALIA' where id = 37;
UPDATE utenlandsk_myndighet set land = 'BOSNIA OG HERCEGOVINA' where id = 38;
UPDATE utenlandsk_myndighet set land = 'CHILE' where id = 39;
UPDATE utenlandsk_myndighet set land = 'INDIA' where id = 40;
UPDATE utenlandsk_myndighet set land = 'ISRAEL' where id = 41;
UPDATE utenlandsk_myndighet set landkode = 'CA_QC', land = 'CANADA' where id = 42; -- Quebec
UPDATE utenlandsk_myndighet set land = 'SERBIA' where id = 43;
UPDATE utenlandsk_myndighet set land = 'TYRKIA' where id = 44;
