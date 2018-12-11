package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000067.*;
import no.nav.dok.melosysbrev._000116.BrevdataType;
import no.nav.dok.melosysbrev._000116.Fag;
import no.nav.dok.melosysbrev._000116.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.*;
import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.*;

import no.nav.dok.melosysbrev._000116.ObjectFactory;

public class A1Mapper implements BrevDataMapper {

    private static final int MAKS_ANTALL_ARBEIDSSTEDER_PLASS_I_BREV = 3;

    private static final String XSD_LOCATION = "melosysbrev/melosys_000116.xsd";

    private Behandling behandling;

    private Behandlingsresultat resultat;

    private BrevDataA1 brevData;

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData) throws JAXBException, SAXException, TekniskException {
        this.behandling = behandling;
        this.resultat = resultat;
        this.brevData = (BrevDataA1) brevData;

        Objects.requireNonNull(brevData, "A1 mapper trenger brevdata av type BrevDataA1Dto");

        Fag fag = mapFag();
        VedleggType vedlegg = new VedleggType();
        vedlegg.setA1(mapA1());
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag, vedlegg);
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    public Fag mapFag() {
        Fag fag = new Fag();
        fag.setVedleggA1("true");
        return fag;
    }

    private A1 mapA1() throws TekniskException {
        A1 a1 = new A1();
        a1.setSerienummer(behandling.getFagsak().getSaksnummer() + behandling.getId());

        try {
            a1.setOpprettelsesDato(convertToXMLGregorianCalendarRemoveTimezone(LocalDate.now()));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException("Konverteringsfeil");
        }

        PersonDokument personDokument = SaksopplysningerUtils.hentPersonDokument(behandling);
        a1.setPerson(mapPerson(personDokument));

        List<LovvalgsperiodeType> lovvalgsperioder = hentLovvalgsperioderFraBehandlingsresultat();
        a1.setLovvalgsperiode(lovvalgsperioder.get(0));    // Kun en lovvalgsperiode i Lev 1

        a1.setYrkesgruppe(YrkesgruppeKode.valueOf(brevData.yrkesgruppe.name()));

        List<Virksomhet> virksomheter = brevData.norskeVirksomheter;
        if (virksomheter.isEmpty()) {
            throw new TekniskException("Trenger minst en valgt norsk virksomhet for ART12.1");
        }

        // Lev1 kun norske virksomheter som hovedvirksomhet (og kun én)
        Virksomhet hovedvirksomhet = virksomheter.remove(0);
        a1.setHovedvirksomhet(mapHovedvirksomhet(hovedvirksomhet));

        virksomheter.addAll(brevData.utenlandskeVirksomheter);

        a1.setBivirksomhetListe(mapBivirksomheter(virksomheter));

        Set<StrukturertAdresse> fysiskArbeidssteder = hentFysiskArbeidssteder();
        if (harIkkeFysiskArbeidssted(fysiskArbeidssteder)) {
            a1.setFysiskArbeidsstedAdresseListe(new FysiskArbeidsstedAdresseListeType());
            a1.setIkkeFysiskArbeidssted("true");
        }
        else {
            Set<Arbeidssted> ikkeFysiskArbeidssteder = hentIkkeFysiskeArbeidssteder();
            a1.setFysiskArbeidsstedAdresseListe(mapFysiskeAdresser(fysiskArbeidssteder, ikkeFysiskArbeidssteder));
            a1.setIkkeFysiskArbeidssted("false");
        }

        return a1;
    }

    private PersonType mapPerson(PersonDokument personDokument) throws TekniskException {
        PersonType person = new PersonType();
        person.setKjoenn(KjoennKode.fromValue(personDokument.kjønn.getKode()));
        person.setStatsborgerskap(personDokument.statsborgerskap.getKode());

        person.setPersonnavn(lagPersonnavn(personDokument));

        try {
            person.setFoedselsdato(convertToXMLGregorianCalendarRemoveTimezone(personDokument.fødselsdato));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException("Konverteringsfeil ved konvertering av fødselsdato", e);
        }

        person.setBostedsadresse(lagBostedsadresse(brevData.bostedsadresse));
        return person;
    }

    private List<LovvalgsperiodeType> hentLovvalgsperioderFraBehandlingsresultat() throws TekniskException {
        List<LovvalgsperiodeType> lovvalgsperiodeTyper = new ArrayList<>();

        Set<Lovvalgsperiode> lovvalgsperioder = resultat.getLovvalgsperioder();
        for (Lovvalgsperiode periode : lovvalgsperioder) {
            lovvalgsperiodeTyper.add(mapLovvalgsperiode(periode));
        }
        if (lovvalgsperiodeTyper.isEmpty()) {
            throw new TekniskException("Ingen lovvalgsperiode funnet for behandlingsresultat");
        }
        return lovvalgsperiodeTyper;
    }

    private LovvalgsperiodeType mapLovvalgsperiode(Lovvalgsperiode lovvalgsperiode) throws TekniskException {
        LovvalgsperiodeType brevPeriode = new LovvalgsperiodeType();
        brevPeriode.setLovvalgsLand(lovvalgsperiode.getLovvalgsland().getKode());
        brevPeriode.setLovvalgsbestemmelse(LovvalgsbestemmelseKode.fromValue(lovvalgsperiode.getBestemmelse().getKode()));
        //brevPeriode.setTilleggsbestemmelse();  // TODO: Mangler i modellen!
        //brevPeriode.setFritekst("");   // TODO: Uvisst når/om vi trenger dette

        try {
            brevPeriode.setFomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getFom()));
            brevPeriode.setTomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getTom()));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException("Konferteringsfeil ved konvertering av lovvalgsperiode", e);
        }
        return brevPeriode;
    }

    private HovedvirksomhetType mapHovedvirksomhet(Virksomhet virksomhet) {
        HovedvirksomhetType hovedvirksomhetBrev = new HovedvirksomhetType();
        StrukturertAdresse adresse = (StrukturertAdresse) virksomhet.adresse;
        hovedvirksomhetBrev.setOrgnummer(virksomhet.orgnr);
        hovedvirksomhetBrev.setNavn(virksomhet.navn);
        hovedvirksomhetBrev.setGatenavn(adresse.gatenavn);
        hovedvirksomhetBrev.setPostnr(adresse.postnummer);
        hovedvirksomhetBrev.setPoststed(adresse.poststed);
        hovedvirksomhetBrev.setLandkode(adresse.landKode);

        boolean selvstendigForetak = brevData.selvstendigeForetak.contains(virksomhet.orgnr);
        if (selvstendigForetak) {
            hovedvirksomhetBrev.setYrkesaktivitet(YrkesaktivitetsKode.SELVSTENDIG);
        }
        else {
            hovedvirksomhetBrev.setYrkesaktivitet(YrkesaktivitetsKode.LOENNET_ARBEID);
        }
        return hovedvirksomhetBrev;
    }

    private BivirksomhetListeType mapBivirksomheter(List<Virksomhet> virksomheter) {
        BivirksomhetListeType bivirksomheterBrev = new BivirksomhetListeType();
        for (Virksomhet virksomhet : virksomheter) {
            BivirksomhetType bivirksomhetType = new BivirksomhetType();
            bivirksomhetType.setNavn(virksomhet.navn);
            bivirksomhetType.setOrgnummer(virksomhet.orgnr);
            bivirksomheterBrev.getBivirksomhet().add(bivirksomhetType);
        }
        return bivirksomheterBrev;
    }

    private FysiskArbeidsstedAdresseListeType mapFysiskeAdresser(Set<StrukturertAdresse> fysiskeArbeidssteder,
                                                                 Set<Arbeidssted> ikkeFysiskArbeidssteder) {
        FysiskArbeidsstedAdresseListeType fysiskeAdresserBrev = new FysiskArbeidsstedAdresseListeType();
        for (Arbeidssted ikkeFysiskArbeidssted : ikkeFysiskArbeidssteder) {
            AdresseType adresseType = new AdresseType();
            adresseType.setNavn(ikkeFysiskArbeidssted.navn);
            adresseType.setLand(ikkeFysiskArbeidssted.landKode);
            fysiskeAdresserBrev.getAdresse().add(adresseType);
        }

        for (StrukturertAdresse adresse : fysiskeArbeidssteder) {
            AdresseType adresseType = new AdresseType();
            adresseType.setNavn("");
            adresseType.setAdresselinje1(adresse.gatenavn);
            adresseType.setAdresselinje2(adresse.postnummer);
            adresseType.setAdresselinje3(adresse.poststed);
            adresseType.setLand(adresse.landKode);

            fysiskeAdresserBrev.getAdresse().add(adresseType);
        }
        return fysiskeAdresserBrev;
    }

    /**
     * Ikke fysisk Arbeidssted er definert som flere enn 3 fysiske arbeidssteder
     * eller ingen fysiske arbeidssteder.
     */
    private boolean harIkkeFysiskArbeidssted(Set<StrukturertAdresse> fysiskArbeidssteder) {
        if (fysiskArbeidssteder.isEmpty()) {
            return true;
        }

        return (fysiskArbeidssteder.size() > MAKS_ANTALL_ARBEIDSSTEDER_PLASS_I_BREV);
    }

    private Set<StrukturertAdresse> hentFysiskArbeidssteder() {
        return brevData.søknad.arbeidUtland.stream()
                .map(au -> au.adresse)
                .collect(Collectors.toSet());
    }

    private Set<Arbeidssted> hentIkkeFysiskeArbeidssteder() {
        Set<Arbeidssted> ikkeFysiskArbeidssteder = new HashSet<>();

        // TODO: Sokkel skip mappes mot installasjonen
        //fysiskeArbeidssteder.add(new Arbeidssted("Ekofisk", "NO");
        //fysiskeArbeidssteder.add(new Arbeidssted("Seven Kestrel", "GB");

        return ikkeFysiskArbeidssteder;
    }

    private JAXBElement<BrevdataType> mapintoBrevdataType(FellesType fellesType, MelosysNAVFelles navFelles, Fag fag, VedleggType vedlegg) {
        ObjectFactory factory = new ObjectFactory();
        BrevdataType brevdataType = factory.createBrevdataType();
        brevdataType.setFelles(fellesType);
        brevdataType.setNAVFelles(navFelles);
        brevdataType.setFag(fag);
        brevdataType.setVedlegg(vedlegg);
        return factory.createBrevdata(brevdataType);
    }

}