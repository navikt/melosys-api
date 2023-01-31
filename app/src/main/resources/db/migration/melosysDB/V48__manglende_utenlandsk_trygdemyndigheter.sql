INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse_1, gateadresse_2, postnummer, poststed, land, landkode)
VALUES(38, null, 'Ministarstvo Rada I Socijalne Politike', 'Vilsonovo Šetalište 10', null, 'BA-71000', 'Sarajevo', 'Bosnia og Hercegovina', 'BA');

INSERT INTO utenlandsk_myndighet_pref(utenlandsk_myndighet_id, preferanse_id) VALUES(38, 1);


INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse_1, gateadresse_2, postnummer, poststed, land, landkode)
VALUES(39, null, 'La Superintendencia de Seguridad Social', 'Social/Morandé 249', 'Morandé, Santiago', null, null, 'Chile', 'CL');

INSERT INTO utenlandsk_myndighet_pref(utenlandsk_myndighet_id, preferanse_id) VALUES(39, 1);


INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse_1, gateadresse_2, postnummer, poststed, land, landkode)
VALUES(40, null, 'Employees’ Provident Fund Organisation', 'Bhavishya Nidhi Bhawan', '14, Bhikaiji Cama Place', null, 'New Dehli – 110 066', 'India', 'IN');

INSERT INTO utenlandsk_myndighet_pref(utenlandsk_myndighet_id, preferanse_id) VALUES(40, 1);


INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse_1, gateadresse_2, postnummer, poststed, land, landkode)
VALUES(41, null, 'National Insurance Institute', '13 Weizman Blvd.', null, null, 'Jerusalem 91099', 'Israel', 'IL');

INSERT INTO utenlandsk_myndighet_pref(utenlandsk_myndighet_id, preferanse_id) VALUES(41, 1);


INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse_1, gateadresse_2, postnummer, poststed, land, landkode)
VALUES(42, null, 'Bureau Des Ententes De Securite Sociale', '1055, Boul. Rene-Levesque Est, 13e Etage', null, null, 'Montreal (Québec) H2l 455', 'Canada', 'CA_CQ');

INSERT INTO utenlandsk_myndighet_pref(utenlandsk_myndighet_id, preferanse_id) VALUES(42, 1);


INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse_1, gateadresse_2, postnummer, poststed, land, landkode)
VALUES(43, null, 'Ministry of Labour and Social Policy', 'Bulevar Mihaila Pupina 2', null, 'RS-11000', 'Beograd', 'Serbia', 'RS');

INSERT INTO utenlandsk_myndighet_pref(utenlandsk_myndighet_id, preferanse_id) VALUES(43, 1);


INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse_1, gateadresse_2, postnummer, poststed, land, landkode)
VALUES(44, null, 'Emeklilik Hizmetleri Genel Mudurlugu', 'Yurtdisi Hizmetleri Daire Baskanligi', 'Mithatpasa Cad. No: 7', '06430', 'Sihhiye- Ankara/Turkiye', 'Tyrkia', 'TR');

INSERT INTO utenlandsk_myndighet_pref(utenlandsk_myndighet_id, preferanse_id) VALUES(44, 1);


-- Endringer i eksisterende rader

UPDATE utenlandsk_myndighet set navn = 'Federal Social Insurance Office' where id = 32; -- Sveits
UPDATE utenlandsk_myndighet set navn = 'Canada Revenue Agency CPP/EI Rulings Division' where id = 35; -- Canada
UPDATE utenlandsk_myndighet set land = 'United Kingdom' where id = 33; -- Storbritannia
UPDATE utenlandsk_myndighet set postnummer = null where id = 37; -- Australia

