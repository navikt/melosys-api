package no.nav.melosys.service.dokument.brev;

import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.brevdata.felles.v1.navfelles.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.BostedsadresseType;
import no.nav.dok.melosysbrev.felles.melosys_felles.LovvalgsperiodeType;
import no.nav.dok.melosysbrev.felles.melosys_felles.PersonnavnType;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import org.apache.commons.lang3.StringUtils;

import static no.nav.melosys.domain.adresse.Adresse.sammenslå;
import static no.nav.melosys.service.dokument.brev.BrevDataService.*;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;

public final class BrevDataUtils {
    private BrevDataUtils() {
        throw new UnsupportedOperationException("Utility");
    }

    static NavAnsatt lagNavAnsatt(String ansattId, String navn) {
        NavAnsatt navAnsatt = new NavAnsatt();
        navAnsatt.setAnsattId(ansattId != null ? ansattId : "N/A");
        navAnsatt.setNavn(navn);
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
            throw new IllegalStateException(e);
        }
        return lovvalgsperiodeType;
    }

    // Adresse-stubs
    public static Kontaktinformasjon lagKontaktInformasjon() {
        Kontaktinformasjon kontaktinformasjon = new Kontaktinformasjon();

        kontaktinformasjon.setBesoksadresse(lagAdresse(new Besoksadresse(), lagNorskPostadresse()));
        kontaktinformasjon.setPostadresse(lagAdresse(new Postadresse(), lagNorskPostadresse()));
        //Adressen skal benyttes dersom bruker/mottakerRolle har behov for å kontakte NAV per post.
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

    public static BostedsadresseType lagBostedsadresse(StrukturertAdresse bosted) {
        BostedsadresseType bostedAdresse = new BostedsadresseType();
        if (StringUtils.isNotEmpty(bosted.getGatenavn())) {
            bostedAdresse.setGatenavn(bosted.getGatenavn());
        } else {
            bostedAdresse.setGatenavn(" ");
        }
        bostedAdresse.setHusnummer(bosted.getHusnummerEtasjeLeilighet());
        bostedAdresse.setPostnr(bosted.getPostnummer());
        bostedAdresse.setPoststed(bosted.getPoststed());
        bostedAdresse.setRegion(bosted.getRegion());
        bostedAdresse.setLandkode(bosted.getLandkode());
        return bostedAdresse;
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

    public static PersonnavnType lagPersonnavn(PersonDokument personDokument) {
        PersonnavnType navn = new PersonnavnType();
        navn.setFornavn(personDokument.getFornavn());
        navn.setMellomnavn(personDokument.getMellomnavn());
        navn.setEtternavn(personDokument.getEtternavn());
        return navn;
    }
}
