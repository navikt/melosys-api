package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000067.LovvalgsperiodeType;
import no.nav.dok.melosysbrev._000067.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.*;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.*;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;

public class A1Mapper {

    private static final int MAKS_ANTALL_ARBEIDSSTEDER_PLASS_I_BREV = 3;
    private static final int ANTALL_PÅKREVDE_FELTER_I_LISTE_5_1 = 15;
    private static final int ANTALL_PÅKREVDE_FELTER_I_LISTE_5_2 = 13;

    private Behandlingsresultat resultat;

    private BrevDataA1 brevData;

    public A1 mapA1(Behandling behandling, Behandlingsresultat resultat, BrevDataA1 brevData) throws TekniskException {
        this.brevData = brevData;
        this.resultat = resultat;

        A1 a1 = new A1();
        a1.setSerienummer(behandling.getFagsak().getSaksnummer() + behandling.getId());

        a1.setOpprettelsesDato(convertToXMLGregorianCalendarRemoveTimezone(Instant.now()));

        a1.setPerson(mapPerson(brevData.person));

        List<LovvalgsperiodeType> lovvalgsperioder = hentLovvalgsperioderFraBehandlingsresultat();
        a1.setLovvalgsperiode(lovvalgsperioder.get(0));    // Alle lovvalgsperiodene har samme bestemmelse og land i Lev1

        a1.setYrkesgruppe(YrkesgruppeKode.valueOf(brevData.yrkesgruppe.name()));

        a1.setHovedvirksomhet(mapHovedvirksomhet(brevData.hovedvirksomhet));

        a1.setBivirksomhetListe(mapBivirksomheter(brevData.arbeidssteder));

        if (harIkkeFysiskArbeidssted(brevData.arbeidssteder)) {
            a1.setFysiskArbeidsstedAdresseListe(mapFysiskeAdresser(Collections.emptyList()));
            a1.setIkkeFysiskArbeidssted("true");
        }
        else {
            a1.setFysiskArbeidsstedAdresseListe(mapFysiskeAdresser(brevData.arbeidssteder));
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
        if (lovvalgsperiode.getTilleggsbestemmelse() != null) {
            brevPeriode.setTilleggsbestemmelse(TilleggsbestemmelseKode.fromValue(lovvalgsperiode.getTilleggsbestemmelse().getKode()));
        }

        try {
            brevPeriode.setFomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getFom()));
            brevPeriode.setTomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getTom()));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException("Konferteringsfeil ved konvertering av lovvalgsperiode", e);
        }
        return brevPeriode;
    }

    private HovedvirksomhetType mapHovedvirksomhet(AvklartVirksomhet virksomhet) {
        HovedvirksomhetType hovedvirksomhetBrev = new HovedvirksomhetType();
        StrukturertAdresse adresse = (StrukturertAdresse) virksomhet.adresse;
        hovedvirksomhetBrev.setOrgnummer(virksomhet.orgnr);
        hovedvirksomhetBrev.setNavn(virksomhet.navn);
        hovedvirksomhetBrev.setGatenavn(adresse.gatenavn);
        hovedvirksomhetBrev.setPostnr(adresse.postnummer);
        hovedvirksomhetBrev.setPoststed(adresse.poststed);
        hovedvirksomhetBrev.setLandkode(adresse.landkode);
        hovedvirksomhetBrev.setYrkesaktivitet(YrkesaktivitetsKode.valueOf(virksomhet.yrkesaktivitet.getKode()));
        return hovedvirksomhetBrev;
    }

    private BivirksomhetListeType mapBivirksomheter(List<Arbeidssted> arbeidssteder) {
        arbeidssteder = fyllMinimumAntallArbeidsstederMedDummyVerdier(arbeidssteder, ANTALL_PÅKREVDE_FELTER_I_LISTE_5_1);

        BivirksomhetListeType bivirksomheterBrev = new BivirksomhetListeType();
        for (Arbeidssted arbeidssted : arbeidssteder) {
            BivirksomhetType bivirksomhetType = new BivirksomhetType();
            bivirksomhetType.setNavn(arbeidssted.navn);
            bivirksomhetType.setOrgnummer(arbeidssted.orgnummer);
            bivirksomheterBrev.getBivirksomhet().add(bivirksomhetType);
        }
        return bivirksomheterBrev;
    }

    private FysiskArbeidsstedAdresseListeType mapFysiskeAdresser(List<Arbeidssted> arbeidssteder) {
        arbeidssteder = fyllMinimumAntallArbeidsstederMedDummyVerdier(arbeidssteder, ANTALL_PÅKREVDE_FELTER_I_LISTE_5_2);

        FysiskArbeidsstedAdresseListeType fysiskeAdresserBrev = new FysiskArbeidsstedAdresseListeType();
        for (Arbeidssted arbeidssted : arbeidssteder) {
            if (arbeidssted.erFysisk()) {
                fysiskeAdresserBrev.getAdresse().add(mapFysiskArbeidssted(arbeidssted));
            }
            else {
                fysiskeAdresserBrev.getAdresse().add(mapIkkeFysiskArbeidssted(arbeidssted));
            }
        }
        return fysiskeAdresserBrev;
    }

    /**
     * Brevtjenesten trenger et fast antall enheter i listen.
     * Fyller derfor opp med tomme elementer for resterende felter
     */
    private List<Arbeidssted> fyllMinimumAntallArbeidsstederMedDummyVerdier(List<Arbeidssted> arbeidssteder, int forventetAntall) {
        List<Arbeidssted> utfylltListe = new ArrayList<>(arbeidssteder);
        int antallAdresserIListe = arbeidssteder.size();
        int gjenståendeAdresser = forventetAntall - antallAdresserIListe;
        for (int i = 0; i < gjenståendeAdresser; i++) {
            utfylltListe.add(new Arbeidssted("", "", ""));
        }
        return utfylltListe;
    }

    private AdresseType mapFysiskArbeidssted(Arbeidssted fysiskArbeidssted) {
        AdresseType adresseType = new AdresseType();
        adresseType.setNavn(fysiskArbeidssted.navn);
        StrukturertAdresse adresse = fysiskArbeidssted.adresse;
        adresseType.setAdresselinje1(adresse.gatenavn);
        adresseType.setAdresselinje2(adresse.postnummer);
        adresseType.setAdresselinje3(adresse.poststed);
        adresseType.setLand(adresse.landkode);
        return adresseType;
    }

    private AdresseType mapIkkeFysiskArbeidssted(Arbeidssted ikkeFysiskArbeidssted) {
        AdresseType adresseType = new AdresseType();
        adresseType.setNavn(ikkeFysiskArbeidssted.navn);
        adresseType.setLand(ikkeFysiskArbeidssted.landkode);
        return adresseType;
    }

    /**
     * Ikke fysisk Arbeidssted er definert som flere enn 3 fysiske arbeidssteder
     * eller ingen fysiske arbeidssteder.
     */
    private boolean harIkkeFysiskArbeidssted(List<Arbeidssted> fysiskArbeidssteder) {
        long antallFysiskeArbeidssteder = fysiskArbeidssteder.stream()
                .filter(fa -> fa.adresse != null)
                .count();

        return antallFysiskeArbeidssteder < 1 ||
               antallFysiskeArbeidssteder > MAKS_ANTALL_ARBEIDSSTEDER_PLASS_I_BREV;
    }
}