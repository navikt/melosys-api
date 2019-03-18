CREATE TABLE utenlandsk_myndighet (
    id                              NUMBER(19),
    institusjonskode                VARCHAR2(99) NULL,
    navn                            VARCHAR2(199) NULL,
    gateadresse                     VARCHAR2(99) NULL,
    postnummer                      VARCHAR2(99) NULL,
    poststed                        VARCHAR2(99) NULL,
    land                            VARCHAR2(99) NOT NULL,
    landkode                        VARCHAR2(99) NOT NULL,
    CONSTRAINT pk_utenlandsk_myndighet PRIMARY KEY (id)
);

CREATE UNIQUE INDEX utenlandsk_myndighet_landkode ON utenlandsk_myndighet (landkode);

INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(1, '9600', 'Federal Ministry for Labour, Social Affairs and Consumer Protection', 'Stubenring 1', '1010', 'Vienna', 'Austria', 'AT');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
      VALUES(2, '206731645', 'National Social Security Office', 'Place Victor Horta 11', '1060', 'Bruxelles', 'Belgium', 'BE');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(3, '131063188', 'National Revenue Agency', 'Dondukov blvd 52', '1000', 'Sofia', 'Bulgaria', 'BG');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(4, '2001', 'Croatian Pension Insurance Institute Central Office Zagreb', 'Mihanoviceva 3', '10000', 'Zagreb', 'Croatia', 'HR');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(5, '1146505', 'Social Insurance Services Ministry of Labour and Social Insurance', 'Vyronos avenue 7', '1465', 'Nicosia', 'Cyprus', 'CY');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(6, 'SZUC10416', 'Czech Social Security Administration', 'Krizova 25', '225 08', 'Praha 5', 'Czech Republic', 'CZ');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(7, '0997', 'Udbetaling Danmark - IPOS', 'Kongens Vænge 8', '3400', 'Hillerød', 'Denmark', 'DK');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(8, '70001975', 'Estonian Social Insurance Board', 'Endla 8', '15092', 'Tallinn', 'Estonia', 'EE');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(9, 'Ingen', 'Almannastovan', 'Box 3096', '110', 'Torshavn', 'Faroe Islands', 'FO');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(10, '200000010', 'The Finnish Centre for Pensions', 'Bokhållargatan 3', '00065', 'Pensionsskyddscentralen', 'Finland', 'FI');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(11, 'CLEISS0000', 'Centre of European and International Liaisons for Social Security', '11 street de la Tour des Dames', '75436', 'Paris Cedex 09', 'France', 'FR');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(12, '109910998', 'German Liaison Agency Health Insurance - International', 'Pennefeldsweg 12c', '53177', 'Bonn', 'Germany', 'DE');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(13, NULL, 'Ministry of Employment And Social Security, General Secretariat of Social Insurance, Branch of International Social Insurance', 'Stadiou 29', '101 10', 'Athens', 'Greece', 'GR');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(14, NULL, 'Family Directorate', 'Box 260', '3900', 'Nuuk', 'Greenland', 'GL');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(15, '1000', 'National Institute of Health Insurance Fund Management', 'Vaci street 73/A', '1139', 'Budapest', 'Hungary', 'HU');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(16, '6602692669', 'Social Insurance Administration', 'Laugavegur 114', '150', 'Reykjavik', 'Iceland', 'IS');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(17, '0073', 'Social Insurance While Working Abroad, Department of Social Protection', 'Cork Road', NULL, 'Waterford', 'Ireland', 'IE');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(18, '403080', 'INPS Regional Direction of Toscana', 'Via del Proconsolo n. 10', '50122', 'Firenze', 'Italy', 'IT');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(19, '0002', 'State Social Insurance Agency', 'Lacplesha street 70a', '1011', 'Riga', 'Latvia', 'LV');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(20, NULL, '???', 'Gerberweg 2', '9490', 'Vaduz', 'Liechtenstein', 'LI');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(21, '188735972', 'Foreign Benefits Office of the State Social Insurance Fund Board', 'Kalvariju str. 147', '8221', 'Vilnius', 'Lithuania', 'LT');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(22, '0073', 'Common centre of social security', 'route d''Esch, 125', '2975', 'Luxembourg', 'Luxembourg', 'LU');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(23, '35604', 'Social Security Division', '38, Ordnance Street', 'VLT 1021', 'Valletta', 'Malta', 'MT');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(24, '1000', 'Social Insurance Bank', 'Postbus 1100', '1180 BH', 'Amstelveen', 'Netherlands', 'NL');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(25, 'PL150000U', 'Social Insurance Institution Branch Office in Kielce Insurances and Contributions Section - 1', 'ul. Piotrkowska 27', '25-510', 'Kielce', 'Poland', 'PL');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(26, '1002', 'Institute of Social Security, Department of Benefits and Contributions', 'Av. República, no. 4', '1069-062', 'Lisboa', 'Portugal', 'PT');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(27, 'CNPAS002', 'National House of Pensions And Other Social Insurance Rights, Applicable Legislation, Ext. Relationships', 'No. 8 Latina St., district 2', '20793', 'Bucharest', 'Romania', 'RO');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(28, NULL, 'Ministry of Labour, Social Affairs and Family of the Slovak Republic', 'Spitalska 4, 6, 8', '816 43', 'Bratislava 1', 'Slovakia', 'SK');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(29, '0500000', 'Ministry of Labour, Family, Social Affairs and Equal Opportunities', 'Kotnikova Ulica 28', '1000', 'Ljubljana', 'Slovenia', 'SI');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(30, '6020', 'General Treasury of the Social Security. General Directorate', 'C/ Astros, 5 y 7', '28007', 'Madrid', 'Spain', 'ES');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(31, '12600', 'The Swedish Social Insurance Agency', 'Box 1164', '62122', 'Visby', 'Sweden', 'SE');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(32, '000001', 'Federal Social Insurance Office, International Affairs', 'Effinger St. 20', '3003', 'Bern', 'Switzerland', 'CH');
INSERT INTO utenlandsk_myndighet (id, institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode)
	  VALUES(33, 'UK010', 'HM Revenue and Customs', 'PT Operations North East England, HM Revenue and Customs', 'BX9 1AN', 'Wolverhampton', 'United Kingdom', 'GB');

CREATE TABLE preferanse(
    id      NUMBER(19) PRIMARY KEY,
    kode    VARCHAR(99)
);

INSERT INTO preferanse(id, kode) VALUES(1, 'RESERVERT_FRA_A1');

CREATE TABLE utenlandsk_myndighet_preferanse(
    utenlandsk_myndighet_id NUMBER(19),
    preferanse_id           NUMBER(19),
    CONSTRAINT fk_utenlandsk_myndighet FOREIGN KEY (utenlandsk_myndighet_id) REFERENCES utenlandsk_myndighet,
    CONSTRAINT fk_preferanse FOREIGN KEY (preferanse_id) REFERENCES preferanse
);

INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(5, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(6, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(7, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(9, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(13, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(14, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(15, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(17, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(20, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(21, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(22, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(23, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(25, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(26, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(27, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(29, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(30, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(32, 1);
INSERT INTO utenlandsk_myndighet_preferanse(utenlandsk_myndighet_id, preferanse_id) VALUES(33, 1);
