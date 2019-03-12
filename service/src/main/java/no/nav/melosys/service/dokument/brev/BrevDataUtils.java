package no.nav.melosys.service.dokument.brev;

import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.brevdata.felles.v1.navfelles.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.BostedsadresseType;
import no.nav.dok.melosysbrev.felles.melosys_felles.LovvalgsperiodeType;
import no.nav.dok.melosysbrev.felles.melosys_felles.PersonnavnType;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.Gateadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;

import static no.nav.melosys.service.dokument.brev.BrevDataService.*;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;

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

    public static LovvalgsperiodeType lagLovvalgsperiodeType(Lovvalgsperiode lovvalgsperiode)  {
        LovvalgsperiodeType lovvalgsperiodeType = new LovvalgsperiodeType();
        try {
            lovvalgsperiodeType.setFomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getFom()));
            lovvalgsperiodeType.setTomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getTom()));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        return lovvalgsperiodeType;
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
