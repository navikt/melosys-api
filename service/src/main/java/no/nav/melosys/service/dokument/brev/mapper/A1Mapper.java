package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000067.LovvalgsperiodeType;
import no.nav.dok.melosysbrev._000067.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.*;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.adresse.UstrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.util.LandkoderUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.DummyArbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.IkkeFysiskArbeidssted;
import org.apache.commons.lang3.StringUtils;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagBostedsadresse;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagPersonnavn;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;

class A1Mapper {
    private static final int MAKS_ANTALL_ARBEIDSSTEDER_PLASS_I_BREV = 3;
    private static final int ANTALL_PÅKREVDE_FELTER_I_LISTE_5_1 = 15;
    private static final int ANTALL_PÅKREVDE_FELTER_I_LISTE_5_2 = 13;

    private BrevDataA1 brevData;

    public A1 mapA1(Behandling behandling, Behandlingsresultat resultat, BrevDataA1 brevData) throws TekniskException {
        this.brevData = brevData;

        A1 a1 = new A1();
        a1.setSerienummer(behandling.getFagsak().getSaksnummer() + behandling.getId());

        a1.setOpprettelsesDato(convertToXMLGregorianCalendarRemoveTimezone(Instant.now()));

        a1.setPerson(mapPerson(brevData.person));

        a1.setLovvalgsperiode(mapLovvalgsperiode(resultat.hentValidertLovvalgsperiode()));

        a1.setYrkesgruppe(YrkesgruppeKode.valueOf(brevData.yrkesgruppe.name()));

        a1.setHovedvirksomhet(mapHovedvirksomhet(brevData.hovedvirksomhet));

        a1.setBivirksomhetListe(mapBivirksomheter(brevData.bivirksomheter));

        a1.setFysiskArbeidsstedAdresseListe(mapFysiskeAdresser(brevData.arbeidssteder));

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
            throw new TekniskException("Konferteringsfeil ved konvertering av lovvalgsperiode", e);
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

    private FysiskArbeidsstedAdresseListeType mapFysiskeAdresser(List<Arbeidssted> arbeidssteder) {
        arbeidssteder = fyllMinimumAntallArbeidsstederMedDummyVerdier(arbeidssteder);

        FysiskArbeidsstedAdresseListeType fysiskeAdresserBrev = new FysiskArbeidsstedAdresseListeType();
        for (Arbeidssted arbeidssted : arbeidssteder) {
            if (arbeidssted.erFysisk()) {
                fysiskeAdresserBrev.getAdresse().add(mapFysiskArbeidssted((FysiskArbeidssted)arbeidssted));
            }
            else {
                fysiskeAdresserBrev.getAdresse().add(mapIkkeFysiskArbeidssted((IkkeFysiskArbeidssted)arbeidssted));
            }
        }
        return fysiskeAdresserBrev;
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

    private List<Arbeidssted> fyllMinimumAntallArbeidsstederMedDummyVerdier(Collection<Arbeidssted> arbeidssteder) {
        List<Arbeidssted> utfylltListe = new ArrayList<>(arbeidssteder);
        int antallAdresserIListe = arbeidssteder.size();
        int gjenståendeAdresser = ANTALL_PÅKREVDE_FELTER_I_LISTE_5_2 - antallAdresserIListe;
        for (int i = 0; i < gjenståendeAdresser; i++) {
            utfylltListe.add(new DummyArbeidssted());
        }
        return utfylltListe;
    }

    private AdresseType mapFysiskArbeidssted(FysiskArbeidssted fysiskArbeidssted) {
        AdresseType adresseType = new AdresseType();
        adresseType.setNavn(fysiskArbeidssted.getForetakNavn());
        UstrukturertAdresse adresse = UstrukturertAdresse.av(fysiskArbeidssted.getAdresse());
        adresseType.setAdresselinje1(adresse.getAdresselinje(1));
        adresseType.setAdresselinje2(adresse.getAdresselinje(2));
        adresseType.setAdresselinje3(adresse.getAdresselinje(3));
        adresseType.setAdresselinje4(adresse.getAdresselinje(4));
        adresseType.setLand(adresse.landkode);
        return adresseType;
    }

    private AdresseType mapIkkeFysiskArbeidssted(IkkeFysiskArbeidssted ikkeFysiskArbeidssted) {
        AdresseType adresseType = new AdresseType();
        adresseType.setNavn(ikkeFysiskArbeidssted.getEnhetNavn());
        adresseType.setLand(ikkeFysiskArbeidssted.getOmråde());
        return adresseType;
    }

    /**
     * Ikke fast Arbeidssted er definert som flere enn 3 arbeidssteder eller ingen arbeidssteder.
     */
    private boolean harIkkeFastArbeidssted(List<Arbeidssted> fysiskArbeidssteder) {
        long antallArbeidssteder = fysiskArbeidssteder.size();

        return antallArbeidssteder < 1 ||
               antallArbeidssteder > MAKS_ANTALL_ARBEIDSSTEDER_PLASS_I_BREV;
    }
}