package no.nav.melosys.service.dokument.brev;

import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.brevdata.felles.v1.navfelles.*;
import no.nav.dok.melosysbrev._000067.MidlertidigOppholdsadresseType;
import no.nav.dok.melosysbrev.felles.melosys_felles.BostedsadresseType;
import no.nav.dok.melosysbrev.felles.melosys_felles.LovvalgsperiodeType;
import no.nav.dok.melosysbrev.felles.melosys_felles.PersonnavnType;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;

import static no.nav.melosys.domain.adresse.Adresse.sammenslå;
import static no.nav.melosys.service.dokument.brev.BrevDataService.*;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;

public final class BrevDataUtils {
    private BrevDataUtils() {
        throw new UnsupportedOperationException("Utility");
    }

    static NavAnsatt lagNavAnsatt(String ansattId, String navn) {
        return NavAnsatt.builder()
            .withAnsattId(ansattId != null ? ansattId : "N/A")
            .withNavn(navn)
            .build();
    }

    static NavEnhet lagNavEnhet() {
        return NavEnhet.builder()
            .withEnhetsId(MELOSYS_ENHET_ID)
            .withEnhetsNavn(PLASSHOLDER_TEKST)
            .build();
    }

    public static LovvalgsperiodeType lagLovvalgsperiodeType(Lovvalgsperiode lovvalgsperiode) {
        LovvalgsperiodeType lovvalgsperiodeType = new LovvalgsperiodeType();
        try {
            lovvalgsperiodeType.setFomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getFom()));
            lovvalgsperiodeType.setTomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getTom()));
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException(e);
        }
        return lovvalgsperiodeType;
    }

    // Adresse-stubs
    public static Kontaktinformasjon lagKontaktInformasjon() {
        return Kontaktinformasjon.builder()
            .withBesoksadresse(lagAdresse(new Besoksadresse(), lagNorskPostadresse()))
            .withPostadresse(lagAdresse(new Postadresse(), lagNorskPostadresse()))
            //Adressen skal benyttes dersom bruker/mottakerRolle har behov for å kontakte NAV per post.
            //Adressen skal benyttes dersom bruker/mottakerRolle har behov for å kontakte NAV per post.
            .withReturadresse(lagAdresse(new Returadresse(), lagNorskPostadresse()))
            .build();
    }

    public static NorskPostadresse lagNorskPostadresse() {
        return NorskPostadresse.builder()
            .withAdresselinje1(PLASSHOLDER_TEKST)
            .withPostnummer(PLASSHOLDER_POSTNUMMER)
            .withPoststed(PLASSHOLDER_TEKST)
            .withLand(PLASSHOLDER_TEKST)
            .build();
    }

    public static UtenlandskPostadresse lagUtendlanskAdresse(UtenlandskMyndighet utenlandskMyndighet) {
        return UtenlandskPostadresse.builder().withAdresselinje1(utenlandskMyndighet.gateadresse)
            .withAdresselinje2(utenlandskMyndighet.postnummer + " " + utenlandskMyndighet.poststed)
            .withAdresselinje3("")
            .withLand(utenlandskMyndighet.land).build();
    }

    private static <T extends AdresseEnhet> T lagAdresse(T adresse, NorskPostadresse postadresse) {
        adresse.setEnhetsId(MELOSYS_ENHET_ID);
        adresse.setEnhetsNavn(PLASSHOLDER_TEKST);
        adresse.setKontaktTelefonnummer(PLASSHOLDER_TEKST);
        adresse.setAdresse(postadresse);
        return adresse;
    }

    public static BostedsadresseType lagBostedsadresse(Bostedsadresse bosted) {
        final var strukturertadresse = bosted.strukturertAdresse();
        return BostedsadresseType.builder()
            .withGatenavn(strukturertadresse.getGatenavn().isEmpty() ? " " : strukturertadresse.getGatenavn())
            .withHusnummer(strukturertadresse.getHusnummerEtasjeLeilighet())
            .withPostnr(strukturertadresse.getPostnummer())
            .withPoststed(strukturertadresse.getPoststed())
            .withRegion(strukturertadresse.getRegion())
            .withLandkode(strukturertadresse.getLandkode())
            .build();
    }


    public static MidlertidigOppholdsadresseType lagMidlertidigOppholdsadresse(StrukturertAdresse strukturertAdresse) {
        return MidlertidigOppholdsadresseType.builder().withGatenavn(strukturertAdresse.getGatenavn())
            .withHusnummer(strukturertAdresse.getHusnummerEtasjeLeilighet())
            .withPostnr(strukturertAdresse.getPostnummer())
            .withPoststed(strukturertAdresse.getPoststed())
            .withRegion(strukturertAdresse.getRegion())
            .withLandkode(strukturertAdresse.getLandkode())
            .build();
    }


    public static UtenlandskPostadresse lagAdresse(StrukturertAdresse adresse) {
        return UtenlandskPostadresse.builder()
            .withAdresselinje1(sammenslå(adresse.getGatenavn(), adresse.getHusnummerEtasjeLeilighet(),
                adresse.getPostboks()))
            .withAdresselinje2(sammenslå(adresse.getPostnummer(), adresse.getPoststed()))
            .withAdresselinje3(adresse.getRegion())
            .withLand(Landkoder.valueOf(adresse.getLandkode()).getBeskrivelse())
            .build();
    }

    public static PersonnavnType lagPersonnavn(Persondata persondata) {
        return PersonnavnType.builder()
            .withFornavn(persondata.getFornavn())
            .withMellomnavn(persondata.getMellomnavn())
            .withEtternavn(persondata.getEtternavn())
            .build();
    }
}
