package no.nav.melosys.service.dokument.brev;

import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.brevdata.felles.v1.navfelles.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.LovvalgsperiodeType;
import no.nav.dok.melosysbrev.felles.melosys_felles.PersonnavnType;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.person.Persondata;

import static no.nav.melosys.domain.adresse.Adresse.sammenslå;
import static no.nav.melosys.service.dokument.brev.BrevDataService.*;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;

public final class BrevDataUtils {
    private BrevDataUtils() {
        throw new UnsupportedOperationException("Utility");
    }

    static NavAnsatt lagNavAnsatt(String ansattId, String navn) {
        return new NavAnsatt()
            .withAnsattId(ansattId != null ? ansattId : "N/A")
            .withNavn(navn);
    }

    static NavEnhet lagNavEnhet() {
        return new NavEnhet()
            .withEnhetsId(MELOSYS_ENHET_ID)
            .withEnhetsNavn(PLASSHOLDER_TEKST);
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
        return new Kontaktinformasjon()
            .withBesoksadresse(lagAdresse(new Besoksadresse(), lagNorskPostadresse()))
            .withPostadresse(lagAdresse(new Postadresse(), lagNorskPostadresse()))
            //Adressen skal benyttes dersom bruker/mottakerRolle har behov for å kontakte NAV per post.
            .withReturadresse(lagAdresse(new Returadresse(), lagNorskPostadresse()));
    }

    public static NorskPostadresse lagNorskPostadresse() {
        return new NorskPostadresse()
            .withAdresselinje1(PLASSHOLDER_TEKST)
            .withPostnummer(PLASSHOLDER_POSTNUMMER)
            .withPoststed(PLASSHOLDER_TEKST)
            .withLand(PLASSHOLDER_TEKST);
    }

    public static UtenlandskPostadresse lagUtendlanskAdresse(UtenlandskMyndighet utenlandskMyndighet) {
        return new UtenlandskPostadresse()
            .withAdresselinje1(utenlandskMyndighet.getGateadresse1())
            .withAdresselinje2(utenlandskMyndighet.getGateadresse2())
            .withAdresselinje3(utenlandskMyndighet.getPostnummer() + " " + utenlandskMyndighet.getPoststed())
            .withLand(utenlandskMyndighet.getLand());
    }

    private static <T extends AdresseEnhet> T lagAdresse(T adresse, NorskPostadresse postadresse) {
        adresse.setEnhetsId(MELOSYS_ENHET_ID);
        adresse.setEnhetsNavn(PLASSHOLDER_TEKST);
        adresse.setKontaktTelefonnummer(PLASSHOLDER_TEKST);
        adresse.setAdresse(postadresse);
        return adresse;
    }

    public static UtenlandskPostadresse lagAdresse(StrukturertAdresse adresse) {
        return new UtenlandskPostadresse()
            .withAdresselinje1(sammenslå(
                adresse.getGatenavn(),
                adresse.getHusnummerEtasjeLeilighet(),
                adresse.getPostboks()))
            .withAdresselinje2(sammenslå(adresse.getPostnummer(), adresse.getPoststed()))
            .withAdresselinje3(adresse.getRegion())
            .withLand(Landkoder.valueOf(adresse.getLandkode()).getBeskrivelse());
    }

    public static PersonnavnType lagPersonnavn(Persondata persondata) {
        return new PersonnavnType()
            .withFornavn(persondata.getFornavn())
            .withMellomnavn(persondata.getMellomnavn())
            .withEtternavn(persondata.getEtternavn());
    }
}
