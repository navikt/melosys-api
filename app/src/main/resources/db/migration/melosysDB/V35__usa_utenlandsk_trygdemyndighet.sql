UPDATE utenlandsk_myndighet
set navn = 'PT Operations', gateadresse = 'North East England, HM Revenue and Customs', postnummer = 'BX9', poststed = '1AN'
where id = 33;

INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
VALUES(34, null, 'Office of Data Exchange, Policy Publications, and International Negotiations Office of Retirement and Disability Policy', 'Social Security Administration, 4700 Annex Building, 6401 Security Blvd', 'Baltimore, Maryland 21235', null, 'USA', 'US');

INSERT INTO utenlandsk_myndighet_pref(utenlandsk_myndighet_id, preferanse_id) VALUES(34, 1);
