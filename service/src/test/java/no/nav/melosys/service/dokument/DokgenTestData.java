package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.adresse.UstrukturertAdresse;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS;
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.RepresentantIUtlandet;
import no.nav.melosys.domain.person.*;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.service.OrganisasjonDokumentTestFactory;

import static java.util.Collections.singletonList;
import static no.nav.melosys.domain.kodeverk.Mottakerroller.*;

public final class DokgenTestData {
    public static final String FNR_BRUKER = "05058892382";
    public static final String ORGNR = "999999999";
    public static final String SAMMENSATT_NAVN_BRUKER = "Donald Duck";
    public static final String ADRESSELINJE_1_BRUKER = "Andebygata 1";
    public static final String POSTNR_BRUKER = "9999";
    public static final String POSTSTED_BRUKER = "Andeby";
    public static final String SAKSNUMMER = "MEL-123";
    public static final String KONTAKT_NAVN = "Fetter Anton";
    public static final String NAVN_ORG = "Advokatene AS";
    public static final String POSTBOKS_ORG = "POSTBOKS 200";
    public static final String POSTNR_ORG = "9990";
    public static final String REGION = "NEVERLAND";
    public static final LocalDate LOVVALGSPERIODE_FOM = LocalDate.of(2020, 1, 1);
    public static final LocalDate LOVVALGSPERIODE_TOM = LocalDate.of(2021, 1, 1);
    public static final String FNR_REPRESENTANT = "30098000492";
    public static final String ORGNR_REPRESENTANT = "810072512";

    public static Behandling lagBehandling() {
        return lagBehandling(lagFagsak());
    }

    public static Behandling lagBehandling(Fagsak fagsak) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setMottatteOpplysninger(lagMottatteOpplysninger());
        return behandling;
    }

    public static Fagsak lagFagsak() {
        return lagFagsak(false);
    }

    public static Fagsak lagFagsak(boolean medRepresentant) {
        Fagsak fagsak = new Fagsak();
        fagsak.setRegistrertDato(Instant.now());
        fagsak.setBehandlinger(lagBehandlinger());
        fagsak.setType(Sakstyper.FTRL);
        fagsak.setTema(Sakstemaer.UNNTAK);
        fagsak.setEndretAv("L12345");
        fagsak.setSaksnummer(SAKSNUMMER);
        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId("aktørId");
        fagsak.getAktører().add(bruker);
        if (medRepresentant) {
            Aktoer representant = new Aktoer();
            representant.setRolle(Aktoersroller.REPRESENTANT);
            representant.setRepresenterer(Representerer.BRUKER);
            fagsak.getAktører().add(representant);
        }
        return fagsak;
    }

    public static Persondata lagPersondata() {
        final var bostedsadresse = new Bostedsadresse(
            new StrukturertAdresse(ADRESSELINJE_1_BRUKER, "42 C", POSTNR_BRUKER, null, null, Landkoder.NO.getKode()),
            null, null, null, "PDL", null, false);

        final var kontaktadresse = new Kontaktadresse(
            new StrukturertAdresse(ADRESSELINJE_1_BRUKER, null, POSTNR_BRUKER, POSTSTED_BRUKER, null, "NO"),
            null, null, null, null, "PDL", null, null,
            false);

        return new Personopplysninger(Collections.emptyList(), bostedsadresse, null, null,
            new Foedsel(null, null, null, null),
            new Folkeregisteridentifikator(FNR_BRUKER), null,

            // For å få testene til å funke som med brukt med PersonDokument må fornavn og etternavn bytte plass.
            // Dette er nå en "feil" i prod og blir en egen oppgave å fikse.
            List.of(kontaktadresse), new Navn("Duck", null, "Donald"), Collections.emptyList(), Collections.emptyList());
    }

    public static UstrukturertAdresse lagAdresse() {
        UstrukturertAdresse ustrukturertAdresse = new UstrukturertAdresse();
        ustrukturertAdresse.adresselinje1 = ADRESSELINJE_1_BRUKER;
        ustrukturertAdresse.postnr = POSTNR_BRUKER;
        ustrukturertAdresse.poststed = POSTSTED_BRUKER;
        ustrukturertAdresse.land = new Land(Land.NORGE);
        return ustrukturertAdresse;
    }

    public static Kontaktopplysning lagKontaktOpplysning() {
        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktNavn(KONTAKT_NAVN);
        return kontaktopplysning;
    }

    public static OrganisasjonDokument lagOrg() {
        OrganisasjonDokument organisasjonDokument = OrganisasjonDokumentTestFactory.createOrganisasjonDokumentForTest();
        organisasjonDokument.setNavn(NAVN_ORG);
        organisasjonDokument.setOrgnummer(ORGNR);
        organisasjonDokument.setOrganisasjonDetaljer(lagOrgDetaljer());
        return organisasjonDokument;
    }

    public static OrganisasjonDokument lagOrg(Landkoder landkoder) {
        OrganisasjonDokument organisasjonDokument = lagOrg();
        OrganisasjonsDetaljer organisasjonsDetaljer = new OrganisasjonsDetaljer();
        SemistrukturertAdresse semistrukturertAdresse = new SemistrukturertAdresse();
        semistrukturertAdresse.setLandkode(landkoder.getKode());
        semistrukturertAdresse.setGyldighetsperiode(new Periode(LocalDate.now(), LocalDate.now()));
        semistrukturertAdresse.setPostnr(POSTNR_ORG);
        organisasjonsDetaljer.setForretningsadresse(List.of(semistrukturertAdresse));
        organisasjonDokument.setOrganisasjonDetaljer(organisasjonsDetaljer);
        return organisasjonDokument;
    }

    private static List<Behandling> lagBehandlinger() {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setRegistrertDato(Instant.now());

        return singletonList(behandling);
    }

    private static MottatteOpplysninger lagMottatteOpplysninger() {
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerdata(lagMottatteOpplysningerdata());
        return mottatteOpplysninger;
    }

    private static MottatteOpplysningerData lagMottatteOpplysningerdata() {
        MottatteOpplysningerData mottatteOpplysningerData = new MottatteOpplysningerData();
        mottatteOpplysningerData.soeknadsland = new Soeknadsland(List.of("AT"), false);
        return mottatteOpplysningerData;
    }

    private static OrganisasjonsDetaljer lagOrgDetaljer() {
        OrganisasjonsDetaljer organisasjonsDetaljer = new OrganisasjonsDetaljer();
        organisasjonsDetaljer.setPostadresse(singletonList(lagOrgAdresse()));
        return organisasjonsDetaljer;
    }

    private static GeografiskAdresse lagOrgAdresse() {
        SemistrukturertAdresse semistrukturertAdresse = new SemistrukturertAdresse();
        semistrukturertAdresse.setAdresselinje1(POSTBOKS_ORG);
        semistrukturertAdresse.setPostnr(POSTNR_ORG);
        semistrukturertAdresse.setGyldighetsperiode(new Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(2)));
        semistrukturertAdresse.setLandkode("NO");
        return semistrukturertAdresse;
    }

    public static Behandling lagTrygdeavtaleBehandling() {
        return lagTrygdeavtaleBehandling(
            RepresentantIUtlandet.av("Foretaksnavn", List.of("Uk address"), Landkoder.GB));
    }

    public static Behandling lagTrygdeavtaleBehandling(RepresentantIUtlandet representantIUtlandet) {
        Behandling behandling = lagBehandling(lagFagsak());
        var mottatteOpplysningerData = new SøknadNorgeEllerUtenforEØS();
        mottatteOpplysningerData.setRepresentantIUtlandet(representantIUtlandet);
        behandling.getMottatteOpplysninger().setMottatteOpplysningerdata(mottatteOpplysningerData);
        return behandling;
    }

    public static Lovvalgsperiode lagLovvalgsperiode() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(LOVVALGSPERIODE_FOM);
        lovvalgsperiode.setTom(LOVVALGSPERIODE_TOM);
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_FTRL);
        lovvalgsperiode.setBestemmelse(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1);
        return lovvalgsperiode;
    }

    public static Mottaker lagMottaker(Mottakerroller rolle) {
        Mottaker mottaker = new Mottaker();
        switch (rolle) {
            case BRUKER -> {
                mottaker.setRolle(BRUKER);
                mottaker.setAktørId(FNR_BRUKER);
            }
            case VIRKSOMHET -> {
                mottaker.setRolle(VIRKSOMHET);
                mottaker.setOrgnr(ORGNR);
            }
            case ARBEIDSGIVER -> {
                mottaker.setRolle(ARBEIDSGIVER);
                mottaker.setOrgnr(ORGNR_REPRESENTANT);
            }
            default -> throw new IllegalArgumentException("Støtter ikke mottakerrolle " + rolle.getKode());
        }
        return mottaker;
    }

    public static Mottaker lagMottakerRepresentant(Aktoertype aktoertype) {
        Mottaker representant = new Mottaker();
        switch (aktoertype) {
            case PERSON -> representant.setPersonIdent(FNR_REPRESENTANT);
            case ORGANISASJON -> representant.setOrgnr(ORGNR_REPRESENTANT);
            default -> throw new IllegalArgumentException("Representant må være person eller organisasjon");
        }
        representant.setRolle(FULLMEKTIG);
        return representant;
    }
}
