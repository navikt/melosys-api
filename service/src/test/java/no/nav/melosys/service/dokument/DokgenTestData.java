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
    public static final String FNR_FULLMEKTIG = "30098000492";
    public static final String ORGNR_FULLMEKTIG = "810072512";

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

    public static Fagsak lagFagsak(boolean medFullmektig) {
        Fagsak fagsak = FagsakTestFactory.builder()
            .saksnummer(SAKSNUMMER)
            .behandlinger(lagBehandlinger())
            .type(Sakstyper.FTRL)
            .tema(Sakstemaer.UNNTAK)
            .medBruker()
            .build();

        fagsak.setRegistrertDato(Instant.now());
        fagsak.setEndretAv("L12345");
        if (medFullmektig) {
            Aktoer fullmektig = new Aktoer();
            fullmektig.setRolle(Aktoersroller.FULLMEKTIG);
            fullmektig.setFullmaktstype(Fullmaktstype.FULLMEKTIG_SØKNAD);
            fagsak.leggTilAktør(fullmektig);
        }
        return fagsak;
    }
    public static Persondata lagPersondata() {
        return lagPersondata(null);
    }

    public static Persondata lagPersondata(LocalDate fødselsdato) {
        final var bostedsadresse = new Bostedsadresse(
            new StrukturertAdresse(ADRESSELINJE_1_BRUKER, "42 C", POSTNR_BRUKER, null, null, Landkoder.NO.getKode()),
            null, null, null, "PDL", null, false);

        final var kontaktadresse = new Kontaktadresse(
            new StrukturertAdresse(ADRESSELINJE_1_BRUKER, null, POSTNR_BRUKER, POSTSTED_BRUKER, null, "NO"),
            null, null, null, null, "PDL", null, null,
            false);

        return new Personopplysninger(Collections.emptyList(), bostedsadresse, null, null,
            new Foedselsdato(fødselsdato, null),
            new Folkeregisteridentifikator(FNR_BRUKER), null,

            // For å få testene til å funke som med brukt med PersonDokument må fornavn og etternavn bytte plass.
            // Dette er nå en "feil" i prod og blir en egen oppgave å fikse.
            List.of(kontaktadresse), new Navn("Duck", null, "Donald"), Collections.emptyList(), Collections.emptyList());
    }

    public static UstrukturertAdresse lagAdresse() {
        UstrukturertAdresse ustrukturertAdresse = new UstrukturertAdresse();
        ustrukturertAdresse.setAdresselinje1(ADRESSELINJE_1_BRUKER);
        ustrukturertAdresse.setPostnr(POSTNR_BRUKER);
        ustrukturertAdresse.setPoststed(POSTSTED_BRUKER);
        ustrukturertAdresse.setLand(new Land(Land.NORGE));
        return ustrukturertAdresse;
    }

    public static Kontaktopplysning lagKontaktOpplysning() {
        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktNavn(KONTAKT_NAVN);
        return kontaktopplysning;
    }

    public static OrganisasjonDokument lagOrg() {
        return OrganisasjonDokumentTestFactory.builder()
            .orgnummer(ORGNR)
            .navn(NAVN_ORG)
            .organisasjonsDetaljer(lagOrgDetaljer())
            .build();
    }

    public static OrganisasjonDokument lagOrg(Landkoder landkoder) {
        SemistrukturertAdresse semistrukturertAdresse = new SemistrukturertAdresse();
        semistrukturertAdresse.setLandkode(landkoder.getKode());
        semistrukturertAdresse.setGyldighetsperiode(new Periode(LocalDate.now(), LocalDate.now()));
        semistrukturertAdresse.setPostnr(POSTNR_ORG);
        OrganisasjonsDetaljer organisasjonsDetaljer = OrganisasjonsDetaljerTestFactory.builder()
            .forretningsadresse(semistrukturertAdresse)
            .build();
        return OrganisasjonDokumentTestFactory.builder()
            .orgnummer(ORGNR)
            .navn(NAVN_ORG)
            .organisasjonsDetaljer(organisasjonsDetaljer)
            .build();
    }

    private static List<Behandling> lagBehandlinger() {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setRegistrertDato(Instant.now());

        return singletonList(behandling);
    }

    private static MottatteOpplysninger lagMottatteOpplysninger() {
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(lagMottatteOpplysningerdata());
        return mottatteOpplysninger;
    }

    private static MottatteOpplysningerData lagMottatteOpplysningerdata() {
        MottatteOpplysningerData mottatteOpplysningerData = new MottatteOpplysningerData();
        mottatteOpplysningerData.soeknadsland = new Soeknadsland(List.of("AT"), false);
        return mottatteOpplysningerData;
    }

    private static OrganisasjonsDetaljer lagOrgDetaljer() {
        return OrganisasjonsDetaljerTestFactory.builder()
            .postadresse(lagOrgAdresse())
            .build();
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
        behandling.getMottatteOpplysninger().setMottatteOpplysningerData(mottatteOpplysningerData);
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
                mottaker.setOrgnr(ORGNR_FULLMEKTIG);
            }
            default -> throw new IllegalArgumentException("Støtter ikke mottakerrolle " + rolle.getKode());
        }
        return mottaker;
    }

    public static Mottaker lagMottakerFullmektig(Aktoertype aktoertype) {
        Mottaker fullmektig = new Mottaker();
        switch (aktoertype) {
            case PERSON -> fullmektig.setPersonIdent(FNR_FULLMEKTIG);
            case ORGANISASJON -> fullmektig.setOrgnr(ORGNR_FULLMEKTIG);
            default -> throw new IllegalArgumentException("Fullmektig må være person eller organisasjon");
        }
        fullmektig.setRolle(FULLMEKTIG);
        return fullmektig;
    }
}
