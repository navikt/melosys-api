package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000067.LovvalgsperiodeType;
import no.nav.dok.melosysbrev._000067.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.*;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.util.LandkoderUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import org.apache.commons.lang3.StringUtils;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagBostedsadresse;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagPersonnavn;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.brekkTekstTilListe;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;

class A1Mapper {
    private static final int MAKS_ANTALL_ARBEIDSSTEDER_PLASS_I_BREV = 3;
    private static final int ANTALL_PÅKREVDE_FELTER_I_LISTE_5_1 = 15;
    private static final int ANTALL_PÅKREVDE_FELTER_I_LISTE_5_2 = 13;
    private static final int MAKS_ANTALL_TEGN_PER_LINJE_5_2 = 70;

    private BrevDataA1 brevData;

    public A1 mapA1(Behandling behandling, Behandlingsresultat resultat, BrevDataA1 brevData) throws TekniskException {
        this.brevData = brevData;

        A1 a1 = new A1();
        a1.setSerienummer(behandling.getFagsak().getSaksnummer() + behandling.getId());

        a1.setOpprettelsesDato(convertToXMLGregorianCalendarRemoveTimezone(Instant.now()));

        a1.setPerson(mapPerson(brevData.person));

        a1.setLovvalgsperiode(mapLovvalgsperiode(resultat.hentValidertLovvalgsperiode()));

        if (brevData.yrkesgruppe != null) {
            a1.setYrkesgruppe(YrkesgruppeKode.valueOf(brevData.yrkesgruppe.name()));
        }

        a1.setHovedvirksomhet(mapHovedvirksomhet(brevData.hovedvirksomhet));

        a1.setBivirksomhetListe(mapBivirksomheter(brevData.bivirksomheter));

        a1.setFysiskArbeidsstedAdresseListe(mapFysiskeAdresser(brevData.arbeidssteder, brevData.arbeidsland));

        String ikkeFysiskArbeidssted = harIkkeFastArbeidssted(brevData.arbeidssteder) ? "true" : "false";
        a1.setIkkeFysiskArbeidssted(ikkeFysiskArbeidssted);

        return a1;
    }

    private PersonType mapPerson(PersonDokument personDokument) throws TekniskException {
        PersonType person = new PersonType();
        person.setKjoenn(KjoennKode.fromValue(personDokument.kjønn.getKode()));
        person.setStatsborgerskap(LandkoderUtils.tilIso2(personDokument.statsborgerskap.getKode()));

        person.setPersonnavn(lagPersonnavn(personDokument));

        try {
            person.setFoedselsdato(convertToXMLGregorianCalendarRemoveTimezone(personDokument.fødselsdato));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException("Konverteringsfeil ved konvertering av fødselsdato", e);
        }

        person.setBostedsadresse(lagBostedsadresse(brevData.bostedsadresse));
        return person;
    }

    private LovvalgsperiodeType mapLovvalgsperiode(Lovvalgsperiode lovvalgsperiode) throws TekniskException {
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
        hovedvirksomhetBrev.setGatenavn(adresse.gatenavn);
        hovedvirksomhetBrev.setPostnr(adresse.postnummer);
        hovedvirksomhetBrev.setPoststed(adresse.poststed);
        hovedvirksomhetBrev.setLandkode(adresse.landkode);
        if (virksomhet.yrkesaktivitet != null) {
            hovedvirksomhetBrev.setYrkesaktivitet(YrkesaktivitetsKode.valueOf(virksomhet.yrkesaktivitet.getKode()));
        }
        return hovedvirksomhetBrev;
    }

    private BivirksomhetListeType mapBivirksomheter(Collection<AvklartVirksomhet> avklarteVirksomheter) {
        avklarteVirksomheter = fyllMinimumAntallArbeidsgiverOppdragsgiverMedDummyVerdier(avklarteVirksomheter);

        BivirksomhetListeType bivirksomheterBrev = new BivirksomhetListeType();
        for (AvklartVirksomhet arbeidssted : avklarteVirksomheter) {
            BivirksomhetType bivirksomhetType = new BivirksomhetType();
            bivirksomhetType.setNavn(arbeidssted.navn);
            bivirksomhetType.setOrgnummer(arbeidssted.orgnr);
            bivirksomheterBrev.getBivirksomhet().add(bivirksomhetType);
        }
        return bivirksomheterBrev;
    }

    /**
     * Brevtjenesten trenger et fast antall enheter i listen.
     * Fyller derfor opp med tomme elementer for resterende felter
     */
    private FysiskArbeidsstedAdresseListeType mapFysiskeAdresser(List<Arbeidssted> arbeidssteder, Collection<Landkoder> arbeidsland) {
        FysiskArbeidsstedAdresseListeType fysiskeAdresserBrev = new FysiskArbeidsstedAdresseListeType();
        Stream.concat(
            hentArbeidsstederOgLandUtenOppgittArbeidssted(arbeidssteder, arbeidsland),
            Stream.generate(A1Mapper::lagTomAdresseType))
            .limit(ANTALL_PÅKREVDE_FELTER_I_LISTE_5_2)
            .forEach(adresseType -> fysiskeAdresserBrev.getAdresse().add(adresseType));
        return fysiskeAdresserBrev;
    }

    private Stream<AdresseType> hentArbeidsstederOgLandUtenOppgittArbeidssted(List<Arbeidssted> arbeidssteder, Collection<Landkoder> arbeidsland) {
        List<String> landUtenOppgittArbeidssted = hentLandUtenOppgittArbeidssted(arbeidssteder, arbeidsland);
        return Stream.concat(
            arbeidssteder.stream().map(this::tilAdresseType),
            landUtenOppgittArbeidssted.stream().map(this::tilAdresseType)
        );
    }

    private AdresseType tilAdresseType(Arbeidssted arbeidssted) {
        AdresseType adresseType = new AdresseType();
        String adresselinje = arbeidssted.lagAdresselinje();
        adresseType.setAdresselinje1(adresselinje.isBlank() ? "" : adresselinje); //uten dette viser ikke brev alle linjene i en A1
        return adresseType;
    }

    /**
     * Brevtjenesten trenger et fast antall enheter i listen.
     * Fyller derfor opp med tomme elementer for resterende felter
     */
    private List<AvklartVirksomhet> fyllMinimumAntallArbeidsgiverOppdragsgiverMedDummyVerdier(Collection<AvklartVirksomhet> avklarteVirksomheter) {
        List<AvklartVirksomhet> utfylltListe = new ArrayList<>(avklarteVirksomheter);
        int antallAdresserIListe = avklarteVirksomheter.size();
        int gjenståendeAdresser = ANTALL_PÅKREVDE_FELTER_I_LISTE_5_1 - antallAdresserIListe;
        for (int i = 0; i < gjenståendeAdresser; i++) {
            utfylltListe.add(new AvklartVirksomhet("", "", null, null));
        }
        return utfylltListe;
    }

    /**
     * Ikke fast Arbeidssted er definert som flere enn 3 arbeidssteder eller ingen arbeidssteder.
     */
    private boolean harIkkeFastArbeidssted(List<Arbeidssted> fysiskArbeidssteder) {
        long antallArbeidssteder = fysiskArbeidssteder.size();

        return antallArbeidssteder < 1 ||
               antallArbeidssteder > MAKS_ANTALL_ARBEIDSSTEDER_PLASS_I_BREV;
    }

    private List<String> hentLandUtenOppgittArbeidssted(List<Arbeidssted> arbeidssteder, Collection<Landkoder> arbeidsland) {
        Set<String> utfylteArbeidsland = arbeidssteder.stream()
            .map(Arbeidssted::getLandkode).collect(Collectors.toSet());
        String beskrivelser = arbeidsland.stream()
            .filter(a -> !utfylteArbeidsland.contains(a.getKode()))
            .map(Landkoder::getBeskrivelse)
            .collect(Collectors.joining(", "));
        return brekkTekstTilListe(beskrivelser, MAKS_ANTALL_TEGN_PER_LINJE_5_2);
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
}