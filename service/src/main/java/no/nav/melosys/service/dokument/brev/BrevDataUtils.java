package no.nav.melosys.service.dokument.brev;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.dok.brevdata.felles.v1.navfelles.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.BostedsadresseType;
import no.nav.dok.melosysbrev.felles.melosys_felles.PersonnavnType;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.Gateadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;

import static no.nav.melosys.service.dokument.brev.BrevDataService.*;

public final class BrevDataUtils {

    private BrevDataUtils() {

    }

    static NavAnsatt lagNavAnsatt(String ansattId) {
        NavAnsatt navAnsatt = new NavAnsatt();
        navAnsatt.setAnsattId(ansattId);
        navAnsatt.setNavn(PLASSHOLDER_TEKST);
        return navAnsatt;
    }

    static NavEnhet lagNavEnhet() {
        NavEnhet navEnhet = new NavEnhet();
        navEnhet.setEnhetsId(MELOSYS_ENHET_ID);
        navEnhet.setEnhetsNavn(PLASSHOLDER_TEKST);
        return navEnhet;
    }

    public static XMLGregorianCalendar convertToXMLGregorianCalendarRemoveTimezone(Instant instant) {
        if (instant == null) {
            return null;
        }
        try {
            return convertToXMLGregorianCalendarRemoveTimezone(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException("Feil ved konvertering av Instant til XmlGregorianCalendar");
        }
    }

    public static XMLGregorianCalendar convertToXMLGregorianCalendarRemoveTimezone(LocalDateTime localDateTime) throws DatatypeConfigurationException {
        if (localDateTime == null) {
            return null;
        }
        return convertToXMLGregorianCalendarRemoveTimezone(localDateTime.toLocalDate());
    }

    public static XMLGregorianCalendar convertToXMLGregorianCalendarRemoveTimezone(LocalDate localDate) throws DatatypeConfigurationException {
        if (localDate == null) {
            return null;
        }
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(
            localDate.getYear(),
            localDate.getMonthValue(),
            localDate.getDayOfMonth(),
            DatatypeConstants.FIELD_UNDEFINED,
            DatatypeConstants.FIELD_UNDEFINED,
            DatatypeConstants.FIELD_UNDEFINED,
            DatatypeConstants.FIELD_UNDEFINED,
            DatatypeConstants.FIELD_UNDEFINED
        );
    }

    // Adresse-stubs
    public static Kontaktinformasjon lagKontaktInformasjon() {
        Kontaktinformasjon kontaktinformasjon = new Kontaktinformasjon();

        kontaktinformasjon.setBesoksadresse(lagAdresse(new Besoksadresse(), lagNorskPostadresse()));
        kontaktinformasjon.setPostadresse(lagAdresse(new Postadresse(), lagNorskPostadresse()));
        //Adressen skal benyttes dersom bruker/mottaker har behov for å kontakte NAV per post.
        kontaktinformasjon.setReturadresse(lagAdresse(new Returadresse(), lagNorskPostadresse()));

        return kontaktinformasjon;
    }

    public static NorskPostadresse lagNorskPostadresse() {
        NorskPostadresse adresse = new NorskPostadresse();
        adresse.setAdresselinje1(PLASSHOLDER_TEKST);
        adresse.setPostnummer(PLASSHOLDER_POSTNUMMER);
        adresse.setPoststed(PLASSHOLDER_TEKST);
        adresse.setLand(PLASSHOLDER_TEKST);
        return adresse;
    }

    private static <T extends AdresseEnhet> T lagAdresse(T adresse, NorskPostadresse postadresse) {
        adresse.setEnhetsId(MELOSYS_ENHET_ID);
        adresse.setEnhetsNavn(PLASSHOLDER_TEKST);
        adresse.setKontaktTelefonnummer(PLASSHOLDER_TEKST);
        adresse.setAdresse(postadresse);
        return adresse;
    }

    public static BostedsadresseType lagBostedsadresse(Bostedsadresse bosted) {
        Gateadresse gateadresse = bosted.getGateadresse();
        BostedsadresseType bostedAdresse = new BostedsadresseType();
        bostedAdresse.setGatenavn(gateadresse.getGatenavn());
        bostedAdresse.setHusnummer(gateadresse.getGatenummer() + " " + gateadresse.getHusbokstav());
        bostedAdresse.setPostnr(bosted.getPostnr());
        bostedAdresse.setPoststed(bosted.getPoststed());
        bostedAdresse.setLandkode(bosted.getLand().getKode());
        //bostedAdresse.setRegion("");       // TODO: Finnes ikke for bostedsadresse
        return bostedAdresse;
    }

    public static PersonnavnType lagPersonnavn(PersonDokument personDokument) {
        PersonnavnType navn = new PersonnavnType();
        navn.setFornavn(personDokument.fornavn);
        navn.setMellomnavn(personDokument.mellomnavn);
        navn.setEtternavn(personDokument.etternavn);
        return navn;
    }

}
