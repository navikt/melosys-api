package no.nav.melosys.service.brev;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.FastMottaker;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.MuligMottakerDto;
import no.nav.melosys.service.dokument.MuligeMottakereDto;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysninger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrevbestillingServiceTest {

    @Mock
    private DokumentServiceFasade mockDokServiceFasade;
    @Mock
    private BrevmottakerService mockBrevmottakerService;
    @Mock
    private PersondataFasade mockPersondataFasade;
    @Mock
    private EregFasade mockEregFasade;
    @Mock
    private KontaktopplysningService mockKontaktopplysningService;
    @Mock
    private KodeverkService mockKodeverkService;
    private final Behandling behandling = lagBehandling();
    private final FakeUnleash fakeUnleash = new FakeUnleash();

    private BrevbestillingService brevbestillingService;

    @BeforeEach
    void init() {
        brevbestillingService = new BrevbestillingService(mockBrevmottakerService, mockDokServiceFasade, mockEregFasade,
                mockKodeverkService, mockKontaktopplysningService, mockPersondataFasade, fakeUnleash);
    }

    @Test
    void hentMuligeMottakere_hovedMottakerBruker_returnererBrukerSomHovedMottaker() {
        when(mockBrevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, behandling))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(BRUKER).build());
        when(mockBrevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)))
            .thenReturn(lagAktoer(BRUKER, null));

        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, behandling, null);

        assertThat(muligeMottakere.getHovedMottaker())
            .extracting(
                MuligMottakerDto::getDokumentNavn,
                MuligMottakerDto::getMottakerNavn,
                MuligMottakerDto::getRolle,
                MuligMottakerDto::getAktørId,
                MuligMottakerDto::getOrgnr)
            .containsExactly(MANGELBREV_BRUKER.getBeskrivelse(), "Ola Nordmann", BRUKER, null, null);
        assertThat(muligeMottakere)
            .extracting(MuligeMottakereDto::getKopiMottakere, MuligeMottakereDto::getFasteMottakere)
            .containsExactly(emptyList(), emptyList());
    }

    @Test
    void hentMuligeMottakere_hovedMottakerBrukerMedFullmektig_returnererFullmektigSomHovedMottaker() {
        when(mockBrevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, behandling))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(BRUKER).build());
        when(mockBrevmottakerService.avklarMottaker(any(Produserbaredokumenter.class), any(), eq(behandling)))
            .thenReturn(lagAktoer(REPRESENTANT, "orgnr"));
        mockHentOrganisasjon("orgnr", "Fullmektig virksomhet");

        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, behandling, null);

        assertThat(muligeMottakere.getHovedMottaker())
            .extracting(
                MuligMottakerDto::getDokumentNavn,
                MuligMottakerDto::getMottakerNavn,
                MuligMottakerDto::getRolle,
                MuligMottakerDto::getAktørId,
                MuligMottakerDto::getOrgnr)
            .containsExactly(MANGELBREV_BRUKER.getBeskrivelse(), "Fullmektig virksomhet", BRUKER, null, null);
        assertThat(muligeMottakere)
            .extracting(MuligeMottakereDto::getKopiMottakere, MuligeMottakereDto::getFasteMottakere)
            .containsExactly(emptyList(), emptyList());
    }

    @Test
    void hentMuligeMottakere_hovedMottakerArbeidsgiver_returnererArbeidsgiverSomHovedMottaker() {
        when(mockBrevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, behandling))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(ARBEIDSGIVER).build());
        mockHentOrganisasjon("orgnr", "Ola Nordmann Rørleggerfirma");

        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, behandling, "orgnr");

        assertThat(muligeMottakere.getHovedMottaker())
            .extracting(
                MuligMottakerDto::getDokumentNavn,
                MuligMottakerDto::getMottakerNavn,
                MuligMottakerDto::getRolle,
                MuligMottakerDto::getAktørId,
                MuligMottakerDto::getOrgnr)
            .containsExactly(MANGELBREV_BRUKER.getBeskrivelse(), "Ola Nordmann Rørleggerfirma", ARBEIDSGIVER, null, null);
        assertThat(muligeMottakere)
            .extracting(MuligeMottakereDto::getKopiMottakere, MuligeMottakereDto::getFasteMottakere)
            .containsExactly(emptyList(), emptyList());
    }

    @Test
    void hentMuligeMottakere_kopiTilBruker_returnererBrukerSomKopi() {
        when(mockBrevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, behandling))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(ARBEIDSGIVER).medKopiMottaker(BRUKER).build());
        mockHentOrganisasjon("orgnr", "Ola Nordmann Rørleggerfirma");

        when(mockBrevmottakerService.avklarMottaker(any(Produserbaredokumenter.class), any(), eq(behandling)))
            .thenReturn(lagAktoer(BRUKER, null));

        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, behandling, "orgnr");

        assertThat(muligeMottakere.getKopiMottakere())
            .flatExtracting(
                MuligMottakerDto::getDokumentNavn,
                MuligMottakerDto::getMottakerNavn,
                MuligMottakerDto::getRolle,
                MuligMottakerDto::getAktørId,
                MuligMottakerDto::getOrgnr)
            .containsExactly("Kopi til bruker", "Ola Nordmann", BRUKER, "aktørId", null);
    }

    @Test
    void hentMuligeMottakere_kopiTilBrukerMedFullmektig_returnererFullmektigSomKopi() {
        when(mockBrevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, behandling))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(ARBEIDSGIVER).medKopiMottaker(BRUKER).build());
        when(mockBrevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)))
            .thenReturn(lagAktoer(REPRESENTANT, "orgnrTilFullmektig"));
        mockHentOrganisasjon("orgnr", "Ola Nordmann Rørleggerfirma");
        mockHentOrganisasjon("orgnrTilFullmektig", "Fullmektig Virksomhet");


        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, behandling, "orgnr");

        assertThat(muligeMottakere.getKopiMottakere())
            .flatExtracting(
                MuligMottakerDto::getDokumentNavn,
                MuligMottakerDto::getMottakerNavn,
                MuligMottakerDto::getRolle,
                MuligMottakerDto::getAktørId,
                MuligMottakerDto::getOrgnr)
            .containsExactly("Kopi til brukers fullmektig", "Fullmektig Virksomhet", REPRESENTANT, null, "orgnrTilFullmektig");
    }

    @Test
    void hentMuligeMottakere_kopiTilBrukerMedFullmektigNårHovedMottakerErBruker_returnererBrukerSomKopi() {
        when(mockBrevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, behandling))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(BRUKER).medKopiMottaker(BRUKER).build());
        when(mockBrevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)))
            .thenReturn(lagAktoer(REPRESENTANT, "orgnrTilFullmektig"));
        mockHentOrganisasjon("orgnrTilFullmektig", "Fullmektig Virksomhet");

        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, behandling, null);

        assertThat(muligeMottakere.getHovedMottaker())
            .extracting(
                MuligMottakerDto::getDokumentNavn,
                MuligMottakerDto::getMottakerNavn,
                MuligMottakerDto::getRolle,
                MuligMottakerDto::getAktørId,
                MuligMottakerDto::getOrgnr)
            .containsExactly(MANGELBREV_BRUKER.getBeskrivelse(), "Fullmektig Virksomhet", BRUKER, null, null);
        assertThat(muligeMottakere.getKopiMottakere())
            .flatExtracting(
                MuligMottakerDto::getDokumentNavn,
                MuligMottakerDto::getMottakerNavn,
                MuligMottakerDto::getRolle,
                MuligMottakerDto::getAktørId,
                MuligMottakerDto::getOrgnr)
            .containsExactly("Kopi til bruker", "Ola Nordmann", BRUKER, "aktørId", null);
    }

    @Test
    void hentMuligeMottakere_kopiTilArbeidsgiver_returnererArbeidsgiverSomKopi() {
        when(mockBrevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, behandling))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(BRUKER).medKopiMottaker(ARBEIDSGIVER).build());
        when(mockBrevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)))
            .thenReturn(lagAktoer(BRUKER, null));
        when(mockBrevmottakerService.avklarMottakere(eq(MANGELBREV_BRUKER), any(), eq(behandling), anyBoolean(), anyBoolean()))
            .thenReturn(List.of(lagAktoer(ARBEIDSGIVER, "orgnr1"), lagAktoer(ARBEIDSGIVER, "orgnr2")));
        mockHentOrganisasjon("orgnr1", "Arbeidsgiver 1");
        mockHentOrganisasjon("orgnr2", "Arbeidsgiver 2");


        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, behandling, null);

        assertThat(muligeMottakere.getKopiMottakere())
            .flatExtracting(
                MuligMottakerDto::getDokumentNavn,
                MuligMottakerDto::getMottakerNavn,
                MuligMottakerDto::getRolle,
                MuligMottakerDto::getAktørId,
                MuligMottakerDto::getOrgnr)
            .containsExactly(
                "Kopi til arbeidsgiver", "Arbeidsgiver 1", ARBEIDSGIVER, null, "orgnr1",
                "Kopi til arbeidsgiver", "Arbeidsgiver 2", ARBEIDSGIVER, null, "orgnr2");
    }

    @Test
    void hentMuligeMottakere_kopiTilArbeidsgiverMedFullmektig_returnererFullmektigSomKopi() {
        when(mockBrevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, behandling))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(BRUKER).medKopiMottaker(ARBEIDSGIVER).build());
        when(mockBrevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)))
            .thenReturn(lagAktoer(BRUKER, null));
        when(mockBrevmottakerService.avklarMottakere(eq(MANGELBREV_BRUKER), any(), eq(behandling), anyBoolean(), anyBoolean()))
            .thenReturn(List.of(lagAktoer(REPRESENTANT, "orgnr")));
        mockHentOrganisasjon("orgnr", "Fullmektig Virksomhet");


        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, behandling, null);

        assertThat(muligeMottakere.getKopiMottakere())
            .flatExtracting(
                MuligMottakerDto::getDokumentNavn,
                MuligMottakerDto::getMottakerNavn,
                MuligMottakerDto::getRolle,
                MuligMottakerDto::getAktørId,
                MuligMottakerDto::getOrgnr)
            .containsExactly(
                "Kopi til arbeidsgivers fullmektig", "Fullmektig Virksomhet", REPRESENTANT, null, "orgnr");
    }

    @Test
    void hentMuligeMottakere_fastTilSkatt_returnererSkattSomFast() {
        when(mockBrevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, behandling))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(BRUKER).medFastMottaker(FastMottaker.SKATT).build());
        when(mockBrevmottakerService.avklarMottaker(MANGELBREV_BRUKER, Mottaker.av(BRUKER), behandling))
            .thenReturn(lagAktoer(BRUKER, null));
        when(mockBrevmottakerService.avklarMottaker(MANGELBREV_BRUKER, FastMottaker.av(FastMottaker.SKATT), behandling))
            .thenReturn(FastMottaker.av(FastMottaker.SKATT).getAktør());
        mockHentOrganisasjon("974761076", "Skatteetaten");

        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, behandling, null);

        assertThat(muligeMottakere.getFasteMottakere())
            .flatExtracting(
                MuligMottakerDto::getDokumentNavn,
                MuligMottakerDto::getMottakerNavn,
                MuligMottakerDto::getRolle,
                MuligMottakerDto::getAktørId,
                MuligMottakerDto::getOrgnr)
            .containsExactly("Kopi til Skatteetaten", "Skatteetaten", MYNDIGHET, null, "974761076");
    }

    @Test
    void hentBrevMaler_behandlingIkkeAvsluttet_returnererMaler() {
        List<Produserbaredokumenter> brevMaler = brevbestillingService.hentMuligeProduserbaredokumenter(new Behandling());

        assertThat(brevMaler)
            .hasSize(2)
            .containsExactlyInAnyOrder(MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER);
    }

    @Test
    void hentBrevMaler_behandlingAvsluttet_returnererTomListe() {
        var behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        List<Produserbaredokumenter> brevMaler = brevbestillingService.hentMuligeProduserbaredokumenter(behandling);

        assertThat(brevMaler).isEmpty();
    }

    @Test
    void hentBrevMaler_behandlingErSoeknad_returnererSoeknadMalITillegg() {
        var behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        List<Produserbaredokumenter> brevMaler = brevbestillingService.hentMuligeProduserbaredokumenter(behandling);

        assertThat(brevMaler)
            .hasSize(3)
            .containsExactlyInAnyOrder(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER);
    }

    @Test
    void hentBrevMaler_behandlingErKlage_returnererKlageMalITillegg() {
        var behandling = new Behandling();
        behandling.setType(Behandlingstyper.KLAGE);
        List<Produserbaredokumenter> brevMaler = brevbestillingService.hentMuligeProduserbaredokumenter(behandling);

        assertThat(brevMaler)
            .hasSize(3)
            .containsExactlyInAnyOrder(MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE, MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER);
    }

    @Test
    @Deprecated
    void hentBrevAdresseTilMottakere_brukerSomMottaker_returnererBrukeradresse() {
        when(mockBrevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoer(Aktoersroller.BRUKER, null)));
        when(mockPersondataFasade.hentPersonFraTps(any(), eq(Informasjonsbehov.STANDARD)))
            .thenReturn(lagPERSOPLSaksopplysning());
        when(mockKodeverkService.dekod(eq(FellesKodeverk.POSTNUMMER), anyString())).thenReturn("Oslo");

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(MANGELBREV_BRUKER, Aktoersroller.BRUKER, behandling);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
            .extracting(BrevAdresse::getMottakerNavn, BrevAdresse::getOrgnr, BrevAdresse::getAdresselinjer, BrevAdresse::getPostnr, BrevAdresse::getPoststed, BrevAdresse::getLand)
            .containsExactly("Ola Nordmann", null, List.of("Gateadresse 43A"), "0123", "Oslo", "NO");
    }

    @Test
    void hentBrevAdresseTilMottakereFraPdl_brukerSomMottaker_returnererBrukeradresse() {
        fakeUnleash.enable("melosys.brev.adresser.pdl");
        when(mockBrevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoer(Aktoersroller.BRUKER, null)));
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(lagPersondata());
        when(mockKodeverkService.dekod(eq(FellesKodeverk.POSTNUMMER), anyString())).thenReturn("Oslo");

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(MANGELBREV_BRUKER, Aktoersroller.BRUKER, behandling);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
            .extracting(BrevAdresse::getMottakerNavn, BrevAdresse::getOrgnr, BrevAdresse::getAdresselinjer, BrevAdresse::getPostnr, BrevAdresse::getPoststed, BrevAdresse::getLand)
            .containsExactly("Nordmann Ola", null, List.of("gatenavnKontaktadressePDL"), "0123", "Oslo", "NO");
        //fakeUnleash.disableAll();
    }

    @Test
    void hentBrevAdresseTilMottakere_brukersFullmaktSomMottaker_returnererFullmektigsAdresse() {
        var behandling = new Behandling();
        behandling.setFagsak(new Fagsak());

        when(mockBrevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoer(Aktoersroller.REPRESENTANT, "orgNr")));
        when(mockEregFasade.hentOrganisasjon("orgNr")).thenReturn(lagORGSaksopplysning("orgNr", "Ola Nordmann Fullmektig"));

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(MANGELBREV_BRUKER, Aktoersroller.BRUKER, behandling);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
            .extracting(BrevAdresse::getMottakerNavn, BrevAdresse::getOrgnr, BrevAdresse::getAdresselinjer, BrevAdresse::getPostnr, BrevAdresse::getPoststed, BrevAdresse::getLand)
            .containsExactly("Ola Nordmann Fullmektig", "orgNr", List.of("Gateadresse 43A"), "0123", "Oslo", Land.NORGE);
    }

    @Test
    void hentBrevAdresseTilMottakere_arbeidsgiverSomMottaker_returnererArbeidsgiverAdresser() {
        var behandling = new Behandling();
        behandling.setFagsak(new Fagsak());

        when(mockBrevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.ARBEIDSGIVER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoer(Aktoersroller.ARBEIDSGIVER, "orgNr1"), lagAktoer(Aktoersroller.ARBEIDSGIVER, "orgNr2")));
        when(mockEregFasade.hentOrganisasjon("orgNr1")).thenReturn(lagORGSaksopplysning("orgNr1", "Ola Nordmann Rørleggerfirma"));
        when(mockEregFasade.hentOrganisasjon("orgNr2")).thenReturn(lagORGSaksopplysning("orgNr2", "Ida Nordmann Rørleggerfirma"));

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(MANGELBREV_ARBEIDSGIVER, Aktoersroller.ARBEIDSGIVER, behandling);

        assertThat(brevAdresser).hasSize(2);
        assertThat(brevAdresser.get(0))
            .extracting(BrevAdresse::getMottakerNavn, BrevAdresse::getOrgnr, BrevAdresse::getAdresselinjer, BrevAdresse::getPostnr, BrevAdresse::getPoststed, BrevAdresse::getLand)
            .containsExactly("Ola Nordmann Rørleggerfirma", "orgNr1", List.of("Gateadresse 43A"), "0123", "Oslo", Land.NORGE);
        assertThat(brevAdresser.get(1))
            .extracting(BrevAdresse::getMottakerNavn, BrevAdresse::getOrgnr, BrevAdresse::getAdresselinjer, BrevAdresse::getPostnr, BrevAdresse::getPoststed, BrevAdresse::getLand)
            .containsExactly("Ida Nordmann Rørleggerfirma", "orgNr2", List.of("Gateadresse 43A"), "0123", "Oslo", Land.NORGE);
    }

    @Test
    void hentBrevAdresseTilMottakere_arbeidsgiverSomMottakerMenIngenArbeidsgivere_returnererTomListe() {
        when(mockBrevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.ARBEIDSGIVER)), any(), eq(false), eq(false)))
            .thenReturn(emptyList());

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(MANGELBREV_ARBEIDSGIVER, Aktoersroller.ARBEIDSGIVER, new Behandling());

        assertThat(brevAdresser).isEmpty();
    }

    @Test
    void hentBrevAdresseTilMottakere_arbeidsgiversFullmaktSomMottaker_returnererFullmektigsAdresse() {
        var behandling = new Behandling();
        behandling.setFagsak(new Fagsak());

        when(mockBrevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.ARBEIDSGIVER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoer(Aktoersroller.REPRESENTANT, "orgNr")));
        when(mockEregFasade.hentOrganisasjon("orgNr")).thenReturn(lagORGSaksopplysning("orgNr", "Ola Nordmann Fullmektig"));

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(MANGELBREV_ARBEIDSGIVER, Aktoersroller.ARBEIDSGIVER, behandling);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
            .extracting(BrevAdresse::getMottakerNavn, BrevAdresse::getOrgnr, BrevAdresse::getAdresselinjer, BrevAdresse::getPostnr, BrevAdresse::getPoststed, BrevAdresse::getLand)
            .containsExactly("Ola Nordmann Fullmektig", "orgNr", List.of("Gateadresse 43A"), "0123", "Oslo", Land.NORGE);
    }

    @Test
    void skalBestilleProduseringAvBrev() {
        BrevbestillingRequest brevbestillingRequest = new BrevbestillingRequest.Builder().medProduserbardokument(MANGELBREV_BRUKER).build();
        brevbestillingService.produserBrev(123L, brevbestillingRequest);

        verify(mockDokServiceFasade).produserDokument(anyLong(), any(BrevbestillingRequest.class));
    }

    @Test
    void skalReturnereUtkast() {
        byte[] pdf = "UTKAST".getBytes(StandardCharsets.UTF_8);
        when(mockDokServiceFasade.produserUtkast(anyLong(), any())).thenReturn(pdf);
        BrevbestillingRequest brevbestillingRequest = new BrevbestillingRequest.Builder().medProduserbardokument(MANGELBREV_BRUKER).build();

        byte[] utkast = brevbestillingService.produserUtkast(123L, brevbestillingRequest);

        assertThat(utkast).isEqualTo(pdf);
        verify(mockDokServiceFasade).produserUtkast(123L, brevbestillingRequest);
    }

    private Aktoer lagAktoer(Aktoersroller aktoersroller, String orgNummer) {
        var aktoer = new Aktoer();
        aktoer.setRolle(aktoersroller);
        aktoer.setOrgnr(orgNummer);
        return aktoer;
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setFagsak(lagFagsak());
        behandling.getSaksopplysninger().add(lagPERSOPLSaksopplysning());
        return behandling;
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        Aktoer bruker = new Aktoer();
        bruker.setRolle(BRUKER);
        bruker.setAktørId("aktørId");
        fagsak.getAktører().add(bruker);
        return fagsak;
    }

    private void mockHentOrganisasjon(String orgnr, String navn) {
        when(mockEregFasade.hentOrganisasjon(orgnr)).thenReturn(lagORGSaksopplysning(orgnr, navn));
    }

    private Saksopplysning lagORGSaksopplysning(String orgNummer, String navn) {
        var geogragiskAdresse = new SemistrukturertAdresse();
        geogragiskAdresse.setAdresselinje1("Gateadresse 43A");
        geogragiskAdresse.setPostnr("0123");
        geogragiskAdresse.setPoststed("Oslo");
        geogragiskAdresse.setLandkode(Land.NORGE);
        geogragiskAdresse.setGyldighetsperiode(new Periode(LocalDate.MIN, LocalDate.MAX));
        var organisasjonsDetaljer = new OrganisasjonsDetaljer();
        organisasjonsDetaljer.postadresse.add(geogragiskAdresse);
        var dokument = new OrganisasjonDokument();
        dokument.setOrganisasjonDetaljer(organisasjonsDetaljer);
        dokument.setNavn(List.of(navn));
        dokument.setOrgnummer(orgNummer);
        var saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(dokument);
        saksopplysning.setType(SaksopplysningType.ORG);
        return saksopplysning;
    }

    private Saksopplysning lagPERSOPLSaksopplysning() {
        var dokument = new PersonDokument();
        dokument.setFnr("12345678910");
        dokument.setSammensattNavn("Ola Nordmann");
        dokument.getGjeldendePostadresse().adresselinje1 = "Gateadresse 43A";
        dokument.getGjeldendePostadresse().postnr = "0123";
        dokument.getGjeldendePostadresse().land = Land.av(Land.NORGE);
        var saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(dokument);
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        return saksopplysning;
    }

    private Persondata lagPersondata() {
        return lagPersonopplysninger();
    }
}
