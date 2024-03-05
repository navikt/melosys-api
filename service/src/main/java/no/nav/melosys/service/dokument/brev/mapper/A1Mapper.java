package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000067.LovvalgsperiodeType;
import no.nav.dok.melosysbrev._000067.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.*;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;
import no.nav.melosys.domain.util.IsoLandkodeKonverterer;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.IkkeFysiskArbeidssted;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagPersonnavn;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;

class A1Mapper {
    private static final int MAKS_ANTALL_ARBEIDSSTEDER_PLASS_I_BREV = 3;
    private static final int ANTALL_PÅKREVDE_FELTER_I_LISTE_5_1 = 15;
    private static final int ANTALL_PÅKREVDE_FELTER_I_LISTE_5_2 = 13;
    static final int MAKS_ANTALL_TEGN_PER_LINJE_5_2 = 70;
    static final String STATSLØS_TEKST = "Stateless";
    static final String UNKNOWN_TEKST = "UNKNOWN";
    static final String FLERE_UKJENTE_ELLER_IKKE_OPPGITT_LAND = "Various EEA-countries/Switzerland";

    private BrevDataA1 brevData;

    public A1 mapA1(Behandling behandling, Behandlingsresultat resultat, BrevDataA1 brevData) {
        this.brevData = brevData;

        A1 a1 = new A1();
        a1.setSerienummer(behandling.getFagsak().getSaksnummer() + behandling.getId());

        a1.setOpprettelsesDato(convertToXMLGregorianCalendarRemoveTimezone(Instant.now()));

        a1.setPerson(mapPerson(brevData));

        a1.setLovvalgsperiode(mapLovvalgsperiode(resultat.hentLovvalgsperiode()));

        if (brevData.getYrkesgruppe() != null) {
            a1.setYrkesgruppe(YrkesgruppeKode.valueOf(brevData.getYrkesgruppe().name()));
        }

        a1.setHovedvirksomhet(mapHovedvirksomhet(brevData.getHovedvirksomhet()));

        a1.setBivirksomhetListe(mapBivirksomheter(brevData.getBivirksomheter(), brevData.getArbeidssteder()));

        a1.setFysiskArbeidsstedAdresseListe(mapFysiskeAdresser(brevData.getArbeidssteder(), brevData.getArbeidsland()));

        String ikkeFysiskArbeidssted = harIkkeFastArbeidssted(brevData.getArbeidssteder()) ? "true" : "false";
        a1.setIkkeFysiskArbeidssted(ikkeFysiskArbeidssted);

        return a1;
    }

    private PersonType mapPerson(BrevDataA1 brevDataA1) {
        final var persondata = brevDataA1.getPerson();
        PersonType person = new PersonType();
        person.setKjoenn(KjoennKode.fromValue(persondata.hentKjønnType().getKode()));
        person.setStatsborgerskap(mapStatsborgerskap(persondata.hentAlleStatsborgerskap()));

        person.setPersonnavn(lagPersonnavn(persondata));

        try {
            person.setFoedselsdato(convertToXMLGregorianCalendarRemoveTimezone(persondata.getFødselsdato()));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException("Konverteringsfeil ved konvertering av fødselsdato", e);
        }

        person.setBostedsadresse(mapBostedAdresse(brevDataA1.getBostedsadresse()));
        person.setMidlertidigOppholdsadresse(mapMidlertidigOppholdsadresse(persondata));

        return person;
    }

    private BostedsadresseType mapBostedAdresse(StrukturertAdresse bostedsadresse) {
        return (bostedsadresse == null) ? null : lagBostedsadresse(bostedsadresse);
    }

    private MidlertidigOppholdsadresseType mapMidlertidigOppholdsadresse(Persondata persondata) {
        Optional<StrukturertAdresse> strukturertAdresse = hentNyesteOppholdEllerKontaktadresse(persondata)
            .or(() -> persondata.finnKontaktadresse().map(Kontaktadresse::hentEllerLagStrukturertAdresse))
            .or(() -> persondata.finnOppholdsadresse().map(Oppholdsadresse::strukturertAdresse));
        return strukturertAdresse.map(this::lagMidlertidigOppholdsadresse).orElse(null);
    }

    private Optional<StrukturertAdresse> hentNyesteOppholdEllerKontaktadresse(Persondata persondata) {
        return persondata.finnOppholdsadresse()
            .map(oppholdsadresse -> persondata.finnKontaktadresse().map(kontaktadresse -> returnerSistRegistrertAdresse(kontaktadresse, oppholdsadresse)))
            .flatMap(strukturertAdresse -> strukturertAdresse.orElse(Optional.empty()));
    }

    private Optional<StrukturertAdresse> returnerSistRegistrertAdresse(Kontaktadresse kontaktadresse, Oppholdsadresse oppholdsadresse) {
        return kontaktadresse.registrertDato().isAfter((oppholdsadresse.registrertDato())) ?
            Optional.ofNullable(kontaktadresse.hentEllerLagStrukturertAdresse()) : Optional.of(oppholdsadresse.strukturertAdresse());
    }

    private static String mapStatsborgerskap(Set<Land> statsborgerskap) {
        if (statsborgerskap.contains(Land.av(Land.STATSLØS))) {
            return STATSLØS_TEKST;
        }

        if (statsborgerskap.contains(Land.av(Land.UNKNOWN))) {
            return UNKNOWN_TEKST;
        }

        return statsborgerskap.stream()
            .sorted(Comparator.comparing(Land::getKode))
            .map(s -> IsoLandkodeKonverterer.tilIso2(s.getKode())).collect(Collectors.joining(","));
    }

    private LovvalgsperiodeType mapLovvalgsperiode(Lovvalgsperiode lovvalgsperiode) {
        LovvalgsperiodeType brevPeriode = new LovvalgsperiodeType();
        brevPeriode.setLovvalgsLand(lovvalgsperiode.getLovvalgsland().getKode());
        brevPeriode.setLovvalgsbestemmelse(LovvalgsbestemmelseKode.fromValue(lovvalgsperiode.getBestemmelse().getKode()));
        if (lovvalgsperiode.getTilleggsbestemmelse() != null) {
            brevPeriode.setTilleggsbestemmelse(TilleggsbestemmelseKode.fromValue(lovvalgsperiode.getTilleggsbestemmelse().getKode()));
        }

        try {
            brevPeriode.setFomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getFom()));
            brevPeriode.setTomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getTom()));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException("Konverteringsfeil ved konvertering av lovvalgsperiode", e);
        }
        return brevPeriode;
    }

    private HovedvirksomhetType mapHovedvirksomhet(AvklartVirksomhet virksomhet) {
        HovedvirksomhetType hovedvirksomhetBrev = new HovedvirksomhetType();
        StrukturertAdresse adresse = (StrukturertAdresse) virksomhet.adresse;

        // Utenlandsk virksomhet kan oppgis uten orgnr
        String orgnr = StringUtils.isNotEmpty(virksomhet.orgnr) ? virksomhet.orgnr : " ";
        hovedvirksomhetBrev.setOrgnummer(orgnr);
        hovedvirksomhetBrev.setNavn(virksomhet.navn);
        hovedvirksomhetBrev.setGatenavn(adresse.getGatenavn());
        hovedvirksomhetBrev.setPostnr(adresse.getPostnummer());
        hovedvirksomhetBrev.setPoststed(adresse.getPoststed());
        hovedvirksomhetBrev.setLandkode(adresse.getLandkode());
        if (virksomhet.yrkesaktivitet != null) {
            hovedvirksomhetBrev.setYrkesaktivitet(YrkesaktivitetsKode.valueOf(virksomhet.yrkesaktivitet.getKode()));
        }
        return hovedvirksomhetBrev;
    }

    /**
     * Brevtjenesten trenger et fast antall enheter i listen.
     * Fyller derfor opp med tomme elementer for resterende felter
     */
    private BivirksomhetListeType mapBivirksomheter(
        Collection<AvklartVirksomhet> avklarteVirksomheter, List<Arbeidssted> arbeidssteder) {

        BivirksomhetListeType bivirksomheterBrev = new BivirksomhetListeType();
        Stream.concat(
                hentAvklarteVirksomheterOgIkkeFysiskeArbeidssteder(avklarteVirksomheter, arbeidssteder),
                Stream.generate(A1Mapper::lagTomBivirksomhetType))
            .limit(ANTALL_PÅKREVDE_FELTER_I_LISTE_5_1)
            .forEach(bivirksomhetType -> bivirksomheterBrev.getBivirksomhet().add(bivirksomhetType));
        return bivirksomheterBrev;
    }

    /**
     * Brevtjenesten trenger et fast antall enheter i listen.
     * Fyller derfor opp med tomme elementer for resterende felter
     */
    private FysiskArbeidsstedAdresseListeType mapFysiskeAdresser(List<Arbeidssted> arbeidssteder,
                                                                 Collection<Land_iso2> arbeidsland) {
        FysiskArbeidsstedAdresseListeType fysiskeAdresserBrev = new FysiskArbeidsstedAdresseListeType();
        Stream.concat(
                lagAdresserForArbeidsstederOgLandUtenArbeidssted(arbeidssteder, arbeidsland),
                Stream.generate(A1Mapper::lagTomAdresseType)
            )
            .limit(ANTALL_PÅKREVDE_FELTER_I_LISTE_5_2)
            .forEach(adresseType -> fysiskeAdresserBrev.getAdresse().add(adresseType));
        return fysiskeAdresserBrev;
    }

    private Stream<AdresseType> lagAdresserForArbeidsstederOgLandUtenArbeidssted(List<Arbeidssted> arbeidssteder,
                                                                                 Collection<Land_iso2> arbeidsland) {
        if (brevData.getUkjenteEllerAlleEosLand()) {
            return lagAdresselinjeForUkjentEllerIkkeOppgittArbeidssted().stream().map(this::tilAdresseType);
        }

        String landUtenOppgittArbeidsstedBeskrivelse = hentLandUtenOppgittArbeidssted(arbeidsland, arbeidssteder)
            .stream()
            .map(Land_iso2::getBeskrivelse)
            .sorted()
            .collect(Collectors.joining(", "));

        return Stream.concat(
            lagAdresselinjerForArbeidssteder(arbeidssteder).stream().map(this::tilAdresseType),
            brekkTekstTilListe(landUtenOppgittArbeidsstedBeskrivelse).stream().map(this::tilAdresseType)
        );
    }

    private static List<String> lagAdresselinjerForArbeidssteder(List<Arbeidssted> arbeidssteder) {
        return arbeidssteder.stream()
            .map(Arbeidssted::lagAdresselinje)
            .map(adresselinje -> adresselinje.isBlank() ? "" : adresselinje) //uten dette viser ikke brev alle linjene i en A1
            .flatMap(adresselinje -> brekkTekstTilListe(adresselinje).stream())
            .toList();
    }

    private static List<String> lagAdresselinjeForUkjentEllerIkkeOppgittArbeidssted() {
        return brekkTekstTilListe(FLERE_UKJENTE_ELLER_IKKE_OPPGITT_LAND);
    }

    private static List<String> brekkTekstTilListe(String tekst) {
        String tekstMedLinjeskift = WordUtils.wrap(tekst, MAKS_ANTALL_TEGN_PER_LINJE_5_2);
        return List.of(tekstMedLinjeskift.split(System.lineSeparator()));
    }

    private Stream<BivirksomhetType> hentAvklarteVirksomheterOgIkkeFysiskeArbeidssteder(
        Collection<AvklartVirksomhet> avklarteVirksomheter, List<Arbeidssted> arbeidssteder) {

        Stream<IkkeFysiskArbeidssted> ikkeFysiskeArbeidssteder = arbeidssteder.stream()
            .filter(IkkeFysiskArbeidssted.class::isInstance)
            .map(IkkeFysiskArbeidssted.class::cast);
        return Stream.concat(
            avklarteVirksomheter.stream().map(this::tilBivirksomhetType),
            ikkeFysiskeArbeidssteder.map(this::tilBivirksomhetType)
        );
    }

    /**
     * Ikke fast Arbeidssted er definert som flere enn 3 arbeidssteder eller ingen arbeidssteder.
     */
    private boolean harIkkeFastArbeidssted(List<Arbeidssted> fysiskArbeidssteder) {
        long antallArbeidssteder = fysiskArbeidssteder.size();

        return antallArbeidssteder < 1 ||
            antallArbeidssteder > MAKS_ANTALL_ARBEIDSSTEDER_PLASS_I_BREV;
    }

    private Set<Land_iso2> hentLandUtenOppgittArbeidssted(Collection<Land_iso2> arbeidsland,
                                                          List<Arbeidssted> arbeidssteder) {
        final Set<String> arbeidslandMedArbeidsted = arbeidssteder.stream()
            .map(Arbeidssted::getLandkode).collect(Collectors.toSet());

        return arbeidsland.stream()
            .filter(a -> !arbeidslandMedArbeidsted.contains(a.getKode()))
            .collect(Collectors.toSet());
    }

    private MidlertidigOppholdsadresseType lagMidlertidigOppholdsadresse(StrukturertAdresse strukturertAdresse) {
        return new MidlertidigOppholdsadresseType()
            .withGatenavn(strukturertAdresse.getGatenavn())
            .withHusnummer(strukturertAdresse.getHusnummerEtasjeLeilighet())
            .withPostnr(strukturertAdresse.getPostnummer())
            .withPoststed(strukturertAdresse.getPoststed())
            .withRegion(strukturertAdresse.getRegion())
            .withLandkode(strukturertAdresse.getLandkode());
    }

    // Noen felter settes til " " for at de skal gå gjennom XSD validering. Melosys er mindre streng enn
    // XSD'en tilsier.
    private BostedsadresseType lagBostedsadresse(StrukturertAdresse strukturertAdresse) {
        return new BostedsadresseType()
            .withGatenavn(StringUtils.isEmpty(strukturertAdresse.getGatenavn()) ? " " : strukturertAdresse.getGatenavn())
            .withHusnummer(strukturertAdresse.getHusnummerEtasjeLeilighet())
            .withPostnr(strukturertAdresse.erNorsk() ? strukturertAdresse.getPostnummer() : lagXsdGyldigPostnrForUtenlandskAdresse(strukturertAdresse))
            .withPoststed(StringUtils.isEmpty(strukturertAdresse.getPoststed()) ? " " : strukturertAdresse.getPoststed())
            .withRegion(strukturertAdresse.getRegion())
            .withLandkode(strukturertAdresse.getLandkode());
    }

    private String lagXsdGyldigPostnrForUtenlandskAdresse(StrukturertAdresse strukturertAdresse) {
        return StringUtils.isEmpty(strukturertAdresse.getPostnummer()) ? " " : strukturertAdresse.getPostnummer();
    }


    private static AdresseType lagTomAdresseType() {
        AdresseType adresseType = new AdresseType();
        adresseType.setAdresselinje1("");
        return adresseType;
    }

    private AdresseType tilAdresseType(String tekst) {
        AdresseType adresseType = new AdresseType();
        adresseType.setAdresselinje1(tekst);
        return adresseType;
    }

    private static BivirksomhetType lagTomBivirksomhetType() {
        BivirksomhetType bivirksomhetType = new BivirksomhetType();
        bivirksomhetType.setNavn("");
        bivirksomhetType.setOrgnummer("");
        return bivirksomhetType;
    }

    private BivirksomhetType tilBivirksomhetType(AvklartVirksomhet avklartVirksomhet) {
        BivirksomhetType bivirksomhetType = new BivirksomhetType();
        bivirksomhetType.setNavn(avklartVirksomhet.navn);
        bivirksomhetType.setOrgnummer(avklartVirksomhet.orgnr);
        return bivirksomhetType;
    }

    private BivirksomhetType tilBivirksomhetType(IkkeFysiskArbeidssted arbeidssted) {
        BivirksomhetType bivirksomhetType = new BivirksomhetType();
        bivirksomhetType.setNavn(arbeidssted.getEnhetNavn());
        bivirksomhetType.setOrgnummer("");
        return bivirksomhetType;
    }
}
