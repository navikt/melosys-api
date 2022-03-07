package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadTrygdeavtale;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Soeknadsland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.RepresentantIUtlandet;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.adresse.Gateadresse;
import no.nav.melosys.domain.dokument.person.adresse.UstrukturertAdresse;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.person.KjoennType;
import no.nav.melosys.domain.person.Master;
import no.nav.melosys.domain.person.Navn;
import no.nav.melosys.domain.person.Personopplysninger;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;

public final class DokgenTestData {
    public static final String FNR_BRUKER = "05058892382";
    public static final String SAMMENSATT_NAVN_BRUKER = "Donald Duck";
    public static final String ADRESSELINJE_1_BRUKER = "Andebygata 1";
    public static final String POSTNR_BRUKER = "9999";
    public static final String POSTSTED_BRUKER = "Andeby";
    public static final String SAKSNUMMER = "MEL-123";
    public static final String KONTAKT_NAVN = "Fetter Anton";
    public static final String NAVN_ORG = "Advokatene AS";
    public static final String POSTBOKS_ORG = "POSTBOKS 200";
    public static final String POSTNR_ORG = "9990";
    public static final String HUSNUMMER = "3";
    public static final String REGION = "NEVERLAND";
    public static final LocalDate LOVVALGSPERIODE_FOM = LocalDate.of(2020, 1, 1);
    public static final LocalDate LOVVALGSPERIODE_TOM = LocalDate.of(2021, 1, 1);

    public static Behandling lagBehandling() {
        return lagBehandling(lagFagsak());
    }

    public static Behandling lagBehandling(Fagsak fagsak) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setSaksopplysninger(singleton(lagPersonopplysning()));
        behandling.setFagsak(fagsak);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setBehandlingsgrunnlag(lagBehandlingsgrunnlag());
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
        fagsak.setEndretAv("L12345");
        fagsak.setSaksnummer(SAKSNUMMER);
        Aktoer bruker = new Aktoer();
        bruker.setRolle(BRUKER);
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

    public static Saksopplysning lagPersonopplysning() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        saksopplysning.setDokument(lagPersonDokument());
        return saksopplysning;
    }

    public static Saksopplysning lagPersonopplysningPDL() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PDL_PERSOPL);
        saksopplysning.setDokument(lagPersondata());
        return saksopplysning;
    }

    public static Personopplysninger lagPersondata() {
        return new Personopplysninger(
            Collections.emptyList(),
            lagBostedadresse(),
            null,
            Collections.emptySet(),
            null,
            null,
            KjoennType.UKJENT,
            Collections.singletonList(lagKontaktadresse()),
            new Navn("Donald", null, "Duck"),
            Collections.singletonList(lagOppholdsadresse()),
            Collections.emptyList()
        );
    }

    public static StrukturertAdresse lagStrukturertAdresse() {
        return new StrukturertAdresse(
            ADRESSELINJE_1_BRUKER,
            HUSNUMMER,
            POSTNR_BRUKER,
            POSTSTED_BRUKER,
            REGION,
            Landkoder.SE.name()
        );
    }

    public static Oppholdsadresse lagOppholdsadresse() {
        return new Oppholdsadresse(
            lagStrukturertAdresse(),
            null,
            LocalDate.now().minusYears(2),
            LocalDate.now().plusYears(2),
            Master.PDL.name(),
            Master.PDL.name(),
            LocalDateTime.now(),
            false
        );
    }

    public static Kontaktadresse lagKontaktadresse() {
        return new Kontaktadresse(
            lagStrukturertAdresse(),
            null,
            null,
            LocalDate.now().minusYears(2),
            LocalDate.now().plusYears(2),
            Master.PDL.name(),
            Master.PDL.name(),
            LocalDateTime.now(),
            false
        );
    }

    public static no.nav.melosys.domain.person.adresse.Bostedsadresse lagBostedadresse() {
        return new no.nav.melosys.domain.person.adresse.Bostedsadresse(
            lagStrukturertAdresse(),
            "",
            LocalDate.now().minusYears(2),
            LocalDate.now().plusYears(2),
            Master.PDL.name(),
            "",
            false
        );
    }

    public static PersonDokument lagPersonDokument() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.setFnr(FNR_BRUKER);
        personDokument.setSammensattNavn(SAMMENSATT_NAVN_BRUKER);
        personDokument.setGjeldendePostadresse(lagAdresse());
        Bostedsadresse bostedsadresse = new Bostedsadresse();
        bostedsadresse.setLand(new Land(Land.STORBRITANNIA));
        Gateadresse gateadresse = new Gateadresse();
        gateadresse.setGatenavn("UK Street 48");
        bostedsadresse.setGateadresse(gateadresse);
        personDokument.setBostedsadresse(bostedsadresse);
        return personDokument;
    }

    public static UstrukturertAdresse lagAdresse() {
        UstrukturertAdresse ustrukturertAdresse = new UstrukturertAdresse();
        ustrukturertAdresse.adresselinje1 = ADRESSELINJE_1_BRUKER;
        ustrukturertAdresse.postnr = POSTNR_BRUKER;
        ustrukturertAdresse.poststed = POSTSTED_BRUKER;
        return ustrukturertAdresse;
    }

    public static Kontaktopplysning lagKontaktOpplysning() {
        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktNavn(KONTAKT_NAVN);
        return kontaktopplysning;
    }

    public static OrganisasjonDokument lagOrg() {
        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        organisasjonDokument.setNavn(NAVN_ORG);
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
        organisasjonsDetaljer.forretningsadresse.add(semistrukturertAdresse);
        organisasjonDokument.setOrganisasjonDetaljer(organisasjonsDetaljer);
        return organisasjonDokument;
    }

    private static List<Behandling> lagBehandlinger() {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setRegistrertDato(Instant.now());

        return singletonList(behandling);
    }

    private static Behandlingsgrunnlag lagBehandlingsgrunnlag() {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(lagBehandlingsgrunnlagdata());
        return behandlingsgrunnlag;
    }

    private static BehandlingsgrunnlagData lagBehandlingsgrunnlagdata() {
        BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        behandlingsgrunnlagData.soeknadsland = new Soeknadsland(List.of("AT"), false);
        return behandlingsgrunnlagData;
    }

    private static OrganisasjonsDetaljer lagOrgDetaljer() {
        OrganisasjonsDetaljer organisasjonsDetaljer = new OrganisasjonsDetaljer();
        organisasjonsDetaljer.postadresse = singletonList(lagOrgAdresse());
        return organisasjonsDetaljer;
    }

    private static GeografiskAdresse lagOrgAdresse() {
        SemistrukturertAdresse semistrukturertAdresse = new SemistrukturertAdresse();
        semistrukturertAdresse.setAdresselinje1(POSTBOKS_ORG);
        semistrukturertAdresse.setPostnr(POSTNR_ORG);
        semistrukturertAdresse.setGyldighetsperiode(new Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(2)));
        return semistrukturertAdresse;
    }

    public static Behandling lagTrygdeavtaleBehandling() {
        return lagTrygdeavtaleBehandling(
            RepresentantIUtlandet.av("Foretaksnavn", List.of("Uk address"), Landkoder.GB));
    }

    public static Behandling lagTrygdeavtaleBehandling(RepresentantIUtlandet representantIUtlandet) {
        Behandling behandling = lagBehandling(lagFagsak());
        var behandlingsgrunnlagData = new SoeknadTrygdeavtale();
        behandlingsgrunnlagData.setRepresentantIUtlandet(representantIUtlandet);
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(behandlingsgrunnlagData);
        return behandling;
    }

    public static Lovvalgsperiode lagLovvalgsperiode() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(LOVVALGSPERIODE_FOM);
        lovvalgsperiode.setTom(LOVVALGSPERIODE_TOM);
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_FTRL);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1);
        return lovvalgsperiode;
    }

}
