package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import no.nav.melosys.domain.dokument.felles.UstrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevDataDto;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.*;

public class A1Mapper implements BrevDataMapper {

    private final int MAKS_ANTALL_ARBEIDSSTEDER_PLASS_I_BREV = 3;

    private static final String XSD_LOCATION = "xsd/melosys_000116.xsd";

    private Behandling behandling;

    private Behandlingsresultat resultat;

    private BrevDataDto brevDataDto;

    private class Virksomhet {
        Virksomhet(ForetakUtland foretak) {
            this.navn = foretak.navn;
            this.orgnr = foretak.orgnr;
            this.adresse = new UstrukturertAdresse(foretak.adresse);
        }

        Virksomhet(OrganisasjonDokument foretak) {
            this.navn = foretak.getNavnSammenslått();
            this.orgnr = foretak.getOrgnummer();
            this.adresse = foretak.getOrganisasjonDetaljer().getForretningsadresseUstrukturert();
        }

        public String navn;
        public String orgnr;
        public UstrukturertAdresse adresse;
    }

    private class IkkeFysiskArbeidssted {
        IkkeFysiskArbeidssted(String navn, String land) {
            this.navn = navn;
            this.land = land;
        }

        public String navn;
        public String land;
    }

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevDataDto brevDataDto) throws JAXBException, SAXException, TekniskException {
        this.behandling = behandling;
        this.resultat = resultat;
        this.brevDataDto = brevDataDto;

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

        a1.setYrkesgruppe(YrkesgruppeKode.valueOf(brevDataDto.yrkesgruppe.navn));

        List<Virksomhet> virksomheter = brevDataDto.norskeVirksomheter.stream()
                .map(Virksomhet::new)
                .collect(Collectors.toList());

        if (virksomheter.isEmpty()) {
            throw new TekniskException("Trenger minst en valgt norsk virksomhet for ART12.1");
        }

        // Lev1 kun norske virksomheter som hovedvirksomhet (og kun én)
        Virksomhet hovedvirksomhet = virksomheter.remove(0);
        a1.setHovedvirksomhet(mapHovedvirksomhet(hovedvirksomhet));

        virksomheter.addAll(brevDataDto.utenlandskeVirksomheter.stream()
                .map(Virksomhet::new)
                .collect(Collectors.toList()));

        a1.setBivirksomhetListe(mapBivirksomheter(virksomheter));

        Set<UstrukturertAdresse> fysiskArbeidssteder = hentFysiskArbeidssteder();
        if (harIkkeFysiskArbeidssted(fysiskArbeidssteder)) {
            a1.setBivirksomhetAdresseListe(new BivirksomhetAdresseListeType());
            a1.setIkkeFysiskArbeidssted("true");
        }
        else {
            Set<IkkeFysiskArbeidssted> ikkeFysiskArbeidssteder = hentIkkeFysiskeArbeidssteder();
            a1.setBivirksomhetAdresseListe(mapFysiskeAdresser(fysiskArbeidssteder, ikkeFysiskArbeidssteder));
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

        Bostedsadresse bosted = personDokument.bostedsadresse;
        BostedsadresseType bostedAdresse = lagBostedsadresse(bosted);
        person.setBostedsadresse(bostedAdresse);
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
        UstrukturertAdresse adresse = virksomhet.adresse; // TODO: Map ustrukturert adresse når dette er på plass
        hovedvirksomhetBrev.setOrgnummer(virksomhet.orgnr);
        hovedvirksomhetBrev.setNavn(virksomhet.navn);
        hovedvirksomhetBrev.setAdresselinje1(adresse.adresselinjer.get(0));
        hovedvirksomhetBrev.setAdresselinje2(adresse.adresselinjer.get(1));
        hovedvirksomhetBrev.setAdresselinje3(adresse.adresselinjer.get(2));
        hovedvirksomhetBrev.setAdresselinje4(adresse.adresselinjer.get(3));
        hovedvirksomhetBrev.setAdresselinje5(adresse.adresselinjer.get(4));
        hovedvirksomhetBrev.setLand(adresse.landKode);

        boolean selvstendigForetak = brevDataDto.selvstendigeForetak.contains(virksomhet.orgnr);
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

    private BivirksomhetAdresseListeType mapFysiskeAdresser(Set<UstrukturertAdresse> fysiskeArbeidssteder,
                                                            Set<IkkeFysiskArbeidssted> ikkeFysiskArbeidssteder) {
        BivirksomhetAdresseListeType bivirksomhetAdresserBrev = new BivirksomhetAdresseListeType();
        for (IkkeFysiskArbeidssted ikkeFysiskArbeidssted : ikkeFysiskArbeidssteder) {
            AdresseType adresseType = new AdresseType();
            adresseType.setNavn(ikkeFysiskArbeidssted.navn);
            adresseType.setLand(ikkeFysiskArbeidssted.land);
            bivirksomhetAdresserBrev.getAdresse().add(adresseType);
        }

        for (UstrukturertAdresse adresse : fysiskeArbeidssteder) {
            AdresseType adresseType = new AdresseType();
            adresseType.setNavn("");
            adresseType.setAdresselinje1(adresse.adresselinjer.get(0));
            adresseType.setAdresselinje2(adresse.adresselinjer.get(1));
            adresseType.setAdresselinje3(adresse.adresselinjer.get(2));
            adresseType.setAdresselinje4(adresse.adresselinjer.get(3));
            adresseType.setAdresselinje5(adresse.adresselinjer.get(4));
            adresseType.setLand(adresse.landKode);

            bivirksomhetAdresserBrev.getAdresse().add(adresseType);
        }
        return bivirksomhetAdresserBrev;
    }

    private boolean harIkkeFysiskArbeidssted(Set<UstrukturertAdresse> fysiskArbeidssteder) {
        if (fysiskArbeidssteder.isEmpty()) {
            return true;
        }
        boolean ikkeNokPlassiBrev = fysiskArbeidssteder.size() + 1 > MAKS_ANTALL_ARBEIDSSTEDER_PLASS_I_BREV;
        if (ikkeNokPlassiBrev) {
            return true;
        }
        return false;
    }

    private Set<UstrukturertAdresse> hentFysiskArbeidssteder() {
        return brevDataDto.søknad.arbeidUtland.stream()
                .map(au -> au.adresse)
                .map(UstrukturertAdresse::new)
                .collect(Collectors.toSet());
    }

    private Set<IkkeFysiskArbeidssted> hentIkkeFysiskeArbeidssteder() {
        Set<IkkeFysiskArbeidssted> ikkeFysiskArbeidssteder = new HashSet<>();

        // TODO: Sokkel skip mappes mot installasjonen
        //fysiskeArbeidssteder.add(new IkkeFysiskArbeidssted("Ekofisk", "NO");
        //fysiskeArbeidssteder.add(new IkkeFysiskArbeidssted("Seven Kestrel", "GB");

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