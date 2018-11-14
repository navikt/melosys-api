package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.UstrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.Gateadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevDataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.convertToXMLGregorianCalendarRemoveTimezone;

import no.nav.dok.melosysbrev._000116.ObjectFactory;

public class A1Mapper implements BrevDataMapper {

    final int MAKS_ANTALL_ARBEIDSSTEDER_PLASS_I_BREV = 3;

    private class Virksomhet {
        Virksomhet(String navn, String orgnr, UstrukturertAdresse adresse) {
            this.navn = navn;
            this.orgnr = orgnr;
            this.adresse = adresse;
        }

        Virksomhet(String navn, String orgnr, StrukturertAdresse adresse) {
            this(navn, orgnr, new UstrukturertAdresse(adresse));
        }

        public String navn;
        public String orgnr;
        public UstrukturertAdresse adresse;
    }

    private static final String XSD_LOCATION = "xsd/melosys_000067.xsd";

    @Autowired
    private AvklartefaktaService avklartefaktaService;

    @Autowired
    private RegisterOppslagService registerOppslagService;

    private Behandling behandling;

    private Behandlingsresultat resultat;

    private SoeknadDokument søknad;

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevDataDto brevDataDto) throws JAXBException, SAXException, TekniskException {
        this.behandling = behandling;
        this.resultat = resultat;

        Optional<Saksopplysning> saksopplysning = behandling.getSaksopplysninger().stream()
                .filter(s -> s.getType().equals(SaksopplysningType.SØKNAD))
                .findFirst();

        søknad = (SoeknadDokument)saksopplysning
                .orElseThrow(() -> new TekniskException("Finner ikke søknad ved sending av A1"))
                .getDokument();

        Fag fag = mapFag();
        VedleggType vedlegg = new VedleggType();
        vedlegg.setA1(mapA1());
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag, vedlegg);
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    public Fag mapFag() throws TekniskException {
        Fag fag = new Fag();
        fag.setVedleggA1(true);
        return fag;
    }

    private A1 mapA1() throws TekniskException {
        A1 a1 = new A1();
        a1.setSerienummer(behandling.getFagsak().getSaksnummer() + behandling.getId());

        try {
            a1.setOpprettelsesDato(convertToXMLGregorianCalendarRemoveTimezone(LocalDate.now()));
        } catch (DatatypeConfigurationException e) {}

        Optional<Saksopplysning> saksopplysningPer = behandling.getSaksopplysninger().stream()
                .filter(s -> s.getType().equals(SaksopplysningType.PERSONOPPLYSNING))
                .findFirst();

        PersonDokument personDokument = (PersonDokument) saksopplysningPer
                .orElseThrow(() -> new TekniskException("Finner ikke søknad ved sending av A1"))
                .getDokument();

        a1.setPerson(mapPerson(personDokument));

        List<LovvalgsperiodeType> lovvalgsperioder = hentLovvalgsperioderFraBehandlingsresultat();
        a1.setLovvalgsperiode(lovvalgsperioder.get(0));    // Kun en lovvalgsperiode i Lev 1


        // Yrkesgruppe


        List<Virksomhet> norskeAvklarteVirksomheter = hentNorskeAvklarteForetak();
        List<ForetakUtland> utenlandskeForetak = hentUtenlandskeAvklarteforetak();

        // Lev1 kun norske virksomheter som hovedvirksomhet (og kun én)
        Virksomhet hovedvirksomhet = norskeAvklarteVirksomheter.remove(0);
        a1.setHovedvirksomhet(mapHovedvirksomhet(hovedvirksomhet, "ORDINÆR"));

        List<Virksomhet> bivirksomheter = norskeAvklarteVirksomheter;
        bivirksomheter.addAll(utenlandskeForetak.stream()
                .map(fu -> new Virksomhet(fu.navn, fu.orgnr, fu.adresse))
                .collect(Collectors.toList()));

        boolean ikkeNokPlassIBrev = bivirksomheter.size() + 1 > MAKS_ANTALL_ARBEIDSSTEDER_PLASS_I_BREV;
        a1.setIkkeFysiskArbeidssted(ikkeNokPlassIBrev ? "True" : "False");

        a1.setBivirksomhetListe(mapBivirksomheter(bivirksomheter));
        a1.setBivirksomhetAdresseListe(mapBivirksomhetAdresser(bivirksomheter));

        return a1;
    }

    private PersonType mapPerson(PersonDokument personDokument) throws TekniskException {
        PersonType person = new PersonType();
        person.setKjoenn(KjoennKode.fromValue(personDokument.kjønn.getKode()));
        person.setStatsborgerskap(personDokument.statsborgerskap.getKode());
        PersonnavnType navn = new PersonnavnType();
        navn.setFornavn(personDokument.fornavn);
        //navn.setMellomnavn(personDokument.mellomnavn); // Finnes ikke i XML
        navn.setEtternavn(personDokument.etternavn);
        person.setPersonnavn(navn);
        //person.setFoedested(); // Finnes ikke i XML
        try {
            person.setFoedselsdato(convertToXMLGregorianCalendarRemoveTimezone(personDokument.fødselsdato));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException("Konverteringsfeil ved konvertering av fødselsdato", e);
        }

        Bostedsadresse bosted = personDokument.bostedsadresse;
        BostedsadresseType bostedAdresse = mapBostedsadresse(bosted);
        person.setBostedsadresse(bostedAdresse);
        //person.setMidlertidigOppholdsadresse(); IKKE I BRUK
        return person;
    }

    private BostedsadresseType mapBostedsadresse(Bostedsadresse bosted) {
        Gateadresse gateadresse = bosted.getGateadresse();
        BostedsadresseType bostedAdresse = new BostedsadresseType();
        bostedAdresse.setGatenavn(gateadresse.getGatenavn());
        bostedAdresse.setHusnummer(gateadresse.getGatenummer()+" "+ gateadresse.getHusbokstav());
        bostedAdresse.setPostnr(bosted.getPostnr());
        bostedAdresse.setPoststed(bosted.getPoststed());
        bostedAdresse.setLandkode(bosted.getLand().getKode());
        bostedAdresse.setRegion("");       // Finnes ikke for bostedsadresse
        return bostedAdresse;
    }

    private HovedvirksomhetType mapHovedvirksomhet(Virksomhet virksomhet, String yrkesaktivitet) {
        HovedvirksomhetType hovedvirksomhetBrev = new HovedvirksomhetType();
        UstrukturertAdresse adresse = virksomhet.adresse;
        hovedvirksomhetBrev.setOrgnummer(virksomhet.orgnr);
        //hovedvirksomhetBrev.setRegion();
        hovedvirksomhetBrev.setYrkesaktivitet(YrkesaktivitetsKode.fromValue(yrkesaktivitet));

        return hovedvirksomhetBrev;
    }

    private BivirksomhetAdresseListeType mapBivirksomhetAdresser(List<Virksomhet> virksomheter) {
        BivirksomhetAdresseListeType bivirksomhetAdresserBrev = new BivirksomhetAdresseListeType();
        for (Virksomhet virksomhet : virksomheter) {
            AdresseType adresse = new AdresseType();
            bivirksomhetAdresserBrev.getAdresse().add(adresse);
        }
        return bivirksomhetAdresserBrev;
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

    private List<Virksomhet> hentNorskeAvklarteForetak() {
        Set<String> avklarteOrganisasjoner = avklartefaktaService.hentAvklarteOrganisasjoner(resultat.getId());

        List<Virksomhet> norskeVirksomhet;
        norskeVirksomhet = søknad.selvstendigArbeid.selvstendigForetak.stream()
                .map(f -> new Virksomhet("", f.orgnr, new UstrukturertAdresse()))
                .collect(Collectors.toList());

        norskeVirksomhet.addAll(behandling.getSaksopplysninger().stream()
                .filter(s -> s.getType() != SaksopplysningType.ORGANISASJON)
                .map(s -> (OrganisasjonDokument)s.getDokument())
                .map(org -> new Virksomhet(org.getNavnSammenslått(),
                                           org.getOrgnummer(),
                                           org.getOrganisasjonDetaljer().getForretningsadresseUstrukturert()))
                .collect(Collectors.toList()));

        norskeVirksomhet.retainAll(avklarteOrganisasjoner);

        return norskeVirksomhet;
    }

    private List<ForetakUtland> hentUtenlandskeAvklarteforetak() {
        Set<String> avklarteOrganisasjoner = avklartefaktaService.hentAvklarteOrganisasjoner(resultat.getId());

        return søknad.foretakUtland.stream()
                .filter(foretak -> !avklarteOrganisasjoner.contains(foretak.orgnr))
                .collect(Collectors.toList());
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
        //brevPeriode.setTilleggsbestemmelse();  Mangler i modellen!
        brevPeriode.setFritekst("");   // Uvisst når/om vi trenger dette

        try {
            brevPeriode.setFomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getFom()));
            brevPeriode.setTomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getTom()));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException("Konferteringsfeil ved konvertering av lovvalgsperiode", e);
        }
        return brevPeriode;
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
