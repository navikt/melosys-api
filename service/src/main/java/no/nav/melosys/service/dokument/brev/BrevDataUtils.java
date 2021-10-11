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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.melosys.domain.adresse.Adresse.sammenslå;
import static no.nav.melosys.service.dokument.brev.BrevDataService.*;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;

public final class BrevDataUtils {
    private static final Logger log = LoggerFactory.getLogger(BrevDataUtils.class);

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
        Kontaktinformasjon kontaktinformasjon = new Kontaktinformasjon();
        kontaktinformasjon.setBesoksadresse(lagAdresse(new Besoksadresse(), lagNorskPostadresse()));
        kontaktinformasjon.setPostadresse(lagAdresse(new Postadresse(), lagNorskPostadresse()));
        //Adressen skal benyttes dersom bruker/mottakerRolle har behov for å kontakte NAV per post.
        kontaktinformasjon.setReturadresse(lagAdresse(new Returadresse(), lagNorskPostadresse()));
        log.debug("Laget kontaktinformasjon med postnummer {} og poststed {}",kontaktinformasjon.getPostadresse().getAdresse().getPostnummer(), kontaktinformasjon.getPostadresse().getAdresse().getPoststed());
        return kontaktinformasjon;
    }

    public static NorskPostadresse lagNorskPostadresse() {
        NorskPostadresse adresse = new NorskPostadresse();
        log.debug("lager Norsk postadresse med tekst {} og postnumemer {}",PLASSHOLDER_TEKST,PLASSHOLDER_POSTNUMMER);
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

    public static UtenlandskPostadresse lagAdresse(StrukturertAdresse adresse) {
        return UtenlandskPostadresse.builder()
            .withAdresselinje1(sammenslå(
                adresse.getGatenavn(),
                adresse.getHusnummerEtasjeLeilighet(),
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
