package no.nav.melosys.service.dokument.brev;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.dok.brevdata.felles.v1.navfelles.*;
import org.springframework.cglib.core.Local;

import static no.nav.melosys.service.dokument.brev.BrevDataService.MELOSYS_ENHET_ID;

public final class BrevDataUtils {

    private BrevDataUtils() {

    }

    static NavAnsatt lagNavAnsatt(String ansattId) {
        NavAnsatt navAnsatt = new NavAnsatt();
        navAnsatt.setAnsattId(ansattId);
        navAnsatt.setBerik(true); // Gjør oppslag mot AD
        navAnsatt.setNavn("Navn");
        return navAnsatt;
    }

    static NavEnhet lagNavEnhet() {
        NavEnhet navEnhet = new NavEnhet();
        navEnhet.setEnhetsId(MELOSYS_ENHET_ID);
        navEnhet.setBerik(true); // Gjør oppslag mot NORG
        navEnhet.setEnhetsNavn("EnhetsNavn");
        return navEnhet;
    }

    public static XMLGregorianCalendar convertToXMLGregorianCalendarRemoveTimezone(Instant instant) throws DatatypeConfigurationException {
        if (instant == null) {
            return null;
        }
        return convertToXMLGregorianCalendarRemoveTimezone(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
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
    static Kontaktinformasjon lagKontaktInformasjon() {
        Kontaktinformasjon kontaktinformasjon = new Kontaktinformasjon();

        kontaktinformasjon.setBesoksadresse(lagAdresse(new Besoksadresse(), lagNorskPostadresse()));
        kontaktinformasjon.setPostadresse(lagAdresse(new Postadresse(), lagNorskPostadresse()));
        //Adressen skal benyttes dersom bruker/mottaker har behov for å kontakte NAV per post.
        kontaktinformasjon.setReturadresse(lagAdresse(new Returadresse(), lagNorskPostadresse()));

        return kontaktinformasjon;
    }

    static NorskPostadresse lagNorskPostadresse() {
        NorskPostadresse adresse = new NorskPostadresse();
        adresse.setAdresselinje1("Adresselinje1");
        adresse.setAdresselinje2("Adresselinje2");
        adresse.setAdresselinje3("Adresselinje3");
        adresse.setPostnummer("7777");
        adresse.setPoststed("Poststed");
        adresse.setLand("Land");
        return adresse;
    }

    private static <T extends AdresseEnhet> T lagAdresse(T adresse, NorskPostadresse postadresse) {
        adresse.setEnhetsId(MELOSYS_ENHET_ID);
        adresse.setBerik(true); // Gjør oppslag mot EREG/TPS
        adresse.setEnhetsNavn("EnhetsNavn");
        adresse.setKontaktTelefonnummer("KontaktTelefonnummer");
        adresse.setAdresse(postadresse);
        return adresse;
    }
}
