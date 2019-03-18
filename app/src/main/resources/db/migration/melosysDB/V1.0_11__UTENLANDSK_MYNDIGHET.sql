CREATE TABLE utenlandsk_myndighet (
    id                              NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    institusjonskode                VARCHAR2(99) NULL,
    navn                            VARCHAR2(199) NULL,
    gateadresse                     VARCHAR2(99) NULL,
    postnummer                      VARCHAR2(99) NULL,
    poststed                        VARCHAR2(99) NULL,
    land                            VARCHAR2(99) NOT NULL,
    landkode                        VARCHAR2(99) NOT NULL,
    reservert_innvilgelse_brev  NUMBER(1) DEFAULT 0,
    CONSTRAINT pk_utenlandsk_myndighet PRIMARY KEY (id)
);

CREATE UNIQUE INDEX utenlandsk_myndighet_landkode ON utenlandsk_myndighet (landkode);

INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('9600', 'Federal Ministry for Labour, Social Affairs and Consumer Protection', 'Stubenring 1', '1010', 'Vienna', 'Austria', 'AT', 0);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
    VALUES('206731645', 'National Social Security Office', 'Place Victor Horta 11', '1060', 'Bruxelles', 'Belgium', 'BE', 0);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('131063188', 'National Revenue Agency', 'Dondukov blvd 52', '1000', 'Sofia', 'Bulgaria', 'BG', 0);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('2001', 'Croatian Pension Insurance Institute Central Office Zagreb', 'Mihanoviceva 3', '10000', 'Zagreb', 'Croatia', 'HR', 0);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('1146505', 'Social Insurance Services Ministry of Labour and Social Insurance', 'Vyronos avenue 7', '1465', 'Nicosia', 'Cyprus', 'CY', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('SZUC10416', 'Czech Social Security Administration', 'Krizova 25', '225 08', 'Praha 5', 'Czech Republic', 'CZ', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('0997', 'Udbetaling Danmark - IPOS', 'Kongens Vænge 8', '3400', 'Hillerød', 'Denmark', 'DK', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('70001975', 'Estonian Social Insurance Board', 'Endla 8', '15092', 'Tallinn', 'Estonia', 'EE', 0);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('Ingen', 'Almannastovan', 'Box 3096', '110', 'Torshavn', 'Faroe Islands', 'FO', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('200000010', 'The Finnish Centre for Pensions', 'Bokhållargatan 3', '00065', 'Pensionsskyddscentralen', 'Finland', 'FI', 0);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('CLEISS0000', 'Centre of European and International Liaisons for Social Security', '11 street de la Tour des Dames', '75436', 'Paris Cedex 09', 'France', 'FR', 0);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('109910998', 'German Liaison Agency Health Insurance - International', 'Pennefeldsweg 12c', '53177', 'Bonn', 'Germany', 'DE', 0);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES(NULL, 'Ministry of Employment And Social Security, General Secretariat of Social Insurance, Branch of International Social Insurance', 'Stadiou 29', '101 10', 'Athens', 'Greece', 'GR', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES(NULL, 'Family Directorate', 'Box 260', '3900', 'Nuuk', 'Greenland', 'GL', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('1000', 'National Institute of Health Insurance Fund Management', 'Vaci street 73/A', '1139', 'Budapest', 'Hungary', 'HU', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('6602692669', 'Social Insurance Administration', 'Laugavegur 114', '150', 'Reykjavik', 'Iceland', 'IS', 0);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('0073', 'Social Insurance While Working Abroad, Department of Social Protection', 'Cork Road', NULL, 'Waterford', 'Ireland', 'IE', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('403080', 'INPS Regional Direction of Toscana', 'Via del Proconsolo n. 10', '50122', 'Firenze', 'Italy', 'IT', 0);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('0002', 'State Social Insurance Agency', 'Lacplesha street 70a', '1011', 'Riga', 'Latvia', 'LV', 0);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES(NULL, '???', 'Gerberweg 2', '9490', 'Vaduz', 'Liechtenstein', 'LI', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('188735972', 'Foreign Benefits Office of the State Social Insurance Fund Board', 'Kalvariju str. 147', '8221', 'Vilnius', 'Lithuania', 'LT', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('0073', 'Common centre of social security', 'route d''Esch, 125', '2975', 'Luxembourg', 'Luxembourg', 'LU', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('35604', 'Social Security Division', '38, Ordnance Street', 'VLT 1021', 'Valletta', 'Malta', 'MT', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('1000', 'Social Insurance Bank', 'Postbus 1100', '1180 BH', 'Amstelveen', 'Netherlands', 'NL', 0);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('PL150000U', 'Social Insurance Institution Branch Office in Kielce Insurances and Contributions Section - 1', 'ul. Piotrkowska 27', '25-510', 'Kielce', 'Poland', 'PL', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('1002', 'Institute of Social Security, Department of Benefits and Contributions', 'Av. República, no. 4', '1069-062', 'Lisboa', 'Portugal', 'PT', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('CNPAS002', 'National House of Pensions And Other Social Insurance Rights, Applicable Legislation, Ext. Relationships', 'No. 8 Latina St., district 2', '20793', 'Bucharest', 'Romania', 'RO', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES(NULL, 'Ministry of Labour, Social Affairs and Family of the Slovak Republic', 'Spitalska 4, 6, 8', '816 43', 'Bratislava 1', 'Slovakia', 'SK', 0);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('0500000', 'Ministry of Labour, Family, Social Affairs and Equal Opportunities', 'Kotnikova Ulica 28', '1000', 'Ljubljana', 'Slovenia', 'SI', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('6020', 'General Treasury of the Social Security. General Directorate', 'C/ Astros, 5 y 7', '28007', 'Madrid', 'Spain', 'ES', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('12600', 'The Swedish Social Insurance Agency', 'Box 1164', '62122', 'Visby', 'Sweden', 'SE', 0);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('000001', 'Federal Social Insurance Office, International Affairs', 'Effinger St. 20', '3003', 'Bern', 'Switzerland', 'CH', 1);
INSERT INTO utenlandsk_myndighet (institusjonskode, navn, gateadresse, postnummer, poststed, land, landkode, reservert_innvilgelse)
	  VALUES('UK010', 'HM Revenue and Customs', 'PT Operations North East England, HM Revenue and Customs', 'BX9 1AN', 'Wolverhampton', 'United Kingdom', 'GB', 1);
