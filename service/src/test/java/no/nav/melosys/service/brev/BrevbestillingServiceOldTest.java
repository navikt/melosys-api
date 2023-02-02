package no.nav.melosys.service.brev;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.FastMottakerMedOrgnr;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.brev.FastMottakerMedOrgnr.SKATTEETATEN;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Deprecated(since = "Tas vekk sammen med melosys.MEL-4835.refactor1 unleash toggle, og tas vekk med BrevbestillingService")
@ExtendWith(MockitoExtension.class)
class BrevbestillingServiceOldTest {

    private final Behandling behandling = lagBehandling();

    @Mock
    private DokumentServiceFasade dokServiceFasade;
    @Mock
    private BrevmottakerService brevmottakerService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private EregFasade eregFasade;
    @Mock
    private KontaktopplysningService kontaktopplysningService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private DokumentNavnService dokumentNavnService;
    @Mock

    private UtenlandskMyndighetService utenlandskMyndighetService;
    private BrevbestillingServiceOld brevbestillingServiceOld;

    @BeforeEach
    void init() {
        brevbestillingServiceOld = new BrevbestillingServiceOld(brevmottakerService, dokServiceFasade, behandlingService, eregFasade,
            kontaktopplysningService, persondataFasade, dokumentNavnService, utenlandskMyndighetService);
    }

    @Deprecated(since = "Tas vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    @Test
    void hentMuligeMottakere_hovedMottakerBruker_returnererBrukerSomHovedMottaker() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123L))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(BRUKER).build());
        when(brevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)))
            .thenReturn(lagAktoerPerson(BRUKER, null));
        when(persondataFasade.hentSammensattNavn(anyString())).thenReturn("Ola Nordmann");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, MANGELBREV_BRUKER, BRUKER)).thenReturn(MANGELBREV_BRUKER.getBeskrivelse());

        var muligeMottakere = brevbestillingServiceOld.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, null);

        assertThat(muligeMottakere.getHovedMottaker())
            .extracting(
                Brevmottaker::getDokumentNavn,
                Brevmottaker::getMottakerNavn,
                Brevmottaker::getRolle,
                Brevmottaker::getAktørId,
                Brevmottaker::getOrgnr)
            .containsExactly(MANGELBREV_BRUKER.getBeskrivelse(), "Ola Nordmann", BRUKER, null, null);
        assertThat(muligeMottakere)
            .extracting(MuligeBrevmottakereDto::getKopiMottakere, MuligeBrevmottakereDto::getFasteMottakere)
            .containsExactly(emptyList(), emptyList());
    }

    @Deprecated(since = "Erstattes av HentMottakereTest. Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    @Test
    void hentMuligeMottakere_hovedMottakerBrukerMedFullmektigOrganisasjon_returnererFullmektigOrganisasjonSomHovedMottaker() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(BRUKER).build());
        when(brevmottakerService.avklarMottaker(any(Produserbaredokumenter.class), any(), eq(behandling)))
            .thenReturn(lagAktoerOrg(REPRESENTANT, "orgnr"));
        mockHentOrganisasjon("orgnr", "Fullmektig virksomhet");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, MANGELBREV_BRUKER, BRUKER)).thenReturn(MANGELBREV_BRUKER.getBeskrivelse());

        var muligeMottakere = brevbestillingServiceOld.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, null);

        assertThat(muligeMottakere.getHovedMottaker())
            .extracting(
                Brevmottaker::getDokumentNavn,
                Brevmottaker::getMottakerNavn,
                Brevmottaker::getRolle,
                Brevmottaker::getAktørId,
                Brevmottaker::getOrgnr)
            .containsExactly(MANGELBREV_BRUKER.getBeskrivelse(), "Fullmektig virksomhet", BRUKER, null, null);
        assertThat(muligeMottakere)
            .extracting(MuligeBrevmottakereDto::getKopiMottakere, MuligeBrevmottakereDto::getFasteMottakere)
            .containsExactly(emptyList(), emptyList());
    }

    @Deprecated(since = "Erstattes av HentMottakereTest. Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    @Test
    void hentMuligeMottakere_hovedMottakerBrukerMedFullmektigPerson_returnererFullmektigPersonSomHovedMottaker() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(BRUKER).build());
        when(brevmottakerService.avklarMottaker(any(Produserbaredokumenter.class), any(), eq(behandling)))
            .thenReturn(lagAktoerPerson(REPRESENTANT, "fnr"));
        when(persondataFasade.hentSammensattNavn("fnr")).thenReturn("Ola Nordmann");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, MANGELBREV_BRUKER, BRUKER)).thenReturn(MANGELBREV_BRUKER.getBeskrivelse());

        var muligeMottakere = brevbestillingServiceOld.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, null);

        assertThat(muligeMottakere.getHovedMottaker())
            .extracting(
                Brevmottaker::getDokumentNavn,
                Brevmottaker::getMottakerNavn,
                Brevmottaker::getRolle,
                Brevmottaker::getAktørId,
                Brevmottaker::getOrgnr)
            .containsExactly(MANGELBREV_BRUKER.getBeskrivelse(), "Ola Nordmann", BRUKER, null, null);
        assertThat(muligeMottakere)
            .extracting(MuligeBrevmottakereDto::getKopiMottakere, MuligeBrevmottakereDto::getFasteMottakere)
            .containsExactly(emptyList(), emptyList());
    }

    @Deprecated(since = "Erstattes av HentMottakereTest. Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    @Test
    void hentMuligeMottakere_hovedMottakerVirksomhet_returnererVirksomhetSomHovedMottaker() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(GENERELT_FRITEKSTBREV_VIRKSOMHET, 123L))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(VIRKSOMHET).build());
        mockFinnOrganisasjon("orgnr", "Equinor AS");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, GENERELT_FRITEKSTBREV_VIRKSOMHET, VIRKSOMHET))
            .thenReturn(GENERELT_FRITEKSTBREV_VIRKSOMHET.getBeskrivelse());

        var muligeMottakere = brevbestillingServiceOld.hentMuligeMottakere(GENERELT_FRITEKSTBREV_VIRKSOMHET, 123L, "orgnr");

        assertThat(muligeMottakere.getHovedMottaker())
            .extracting(
                Brevmottaker::getDokumentNavn,
                Brevmottaker::getMottakerNavn,
                Brevmottaker::getRolle,
                Brevmottaker::getAktørId,
                Brevmottaker::getOrgnr)
            .containsExactly(GENERELT_FRITEKSTBREV_VIRKSOMHET.getBeskrivelse(), "Equinor AS", VIRKSOMHET, null, null);
        assertThat(muligeMottakere)
            .extracting(MuligeBrevmottakereDto::getKopiMottakere, MuligeBrevmottakereDto::getFasteMottakere)
            .containsExactly(emptyList(), emptyList());
    }

    @Deprecated(since = "Erstattes av HentMottakereTest. Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    @Test
    void hentMuligeMottakere_hovedMottakerArbeidsgiver_returnererArbeidsgiverSomHovedMottaker() {
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(ARBEIDSGIVER).build());
        mockFinnOrganisasjon("orgnr", "Ola Nordmann Rørleggerfirma");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(null, MANGELBREV_BRUKER, ARBEIDSGIVER))
            .thenReturn(MANGELBREV_BRUKER.getBeskrivelse());

        var muligeMottakere = brevbestillingServiceOld.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, "orgnr");

        assertThat(muligeMottakere.getHovedMottaker())
            .extracting(
                Brevmottaker::getDokumentNavn,
                Brevmottaker::getMottakerNavn,
                Brevmottaker::getRolle,
                Brevmottaker::getAktørId,
                Brevmottaker::getOrgnr)
            .containsExactly(MANGELBREV_BRUKER.getBeskrivelse(), "Ola Nordmann Rørleggerfirma", ARBEIDSGIVER, null, null);
        assertThat(muligeMottakere)
            .extracting(MuligeBrevmottakereDto::getKopiMottakere, MuligeBrevmottakereDto::getFasteMottakere)
            .containsExactly(emptyList(), emptyList());
    }

    @Deprecated(since = "Erstattes av HentMottakereTest. Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    @Test
    void hentMuligeMottakere_kopiTilBruker_returnererBrukerSomKopi() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(ARBEIDSGIVER).medKopiMottaker(BRUKER).build());
        when(persondataFasade.hentSammensattNavn(anyString())).thenReturn("Ola Nordmann");
        mockFinnOrganisasjon("orgnr", "Ola Nordmann Rørleggerfirma");

        when(brevmottakerService.avklarMottaker(any(Produserbaredokumenter.class), any(), eq(behandling)))
            .thenReturn(lagAktoerOrg(BRUKER, null));

        var muligeMottakere = brevbestillingServiceOld.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, "orgnr");

        assertThat(muligeMottakere.getKopiMottakere())
            .flatExtracting(
                Brevmottaker::getDokumentNavn,
                Brevmottaker::getMottakerNavn,
                Brevmottaker::getRolle,
                Brevmottaker::getAktørId,
                Brevmottaker::getOrgnr)
            .containsExactly("Kopi til bruker", "Ola Nordmann", BRUKER, "aktørId", null);
    }

    @Deprecated(since = "Erstattes av HentMottakereTest. Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    @Test
    void hentMuligeMottakere_kopiTilBrukerMedFullmektig_returnererFullmektigSomKopi() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(ARBEIDSGIVER).medKopiMottaker(BRUKER).build());
        when(brevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)))
            .thenReturn(lagAktoerOrg(REPRESENTANT, "orgnrTilFullmektig"));
        mockFinnOrganisasjon("orgnr", "Ola Nordmann Rørleggerfirma");
        mockHentOrganisasjon("orgnrTilFullmektig", "Fullmektig Virksomhet");


        var muligeMottakere = brevbestillingServiceOld.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, "orgnr");

        assertThat(muligeMottakere.getKopiMottakere())
            .flatExtracting(
                Brevmottaker::getDokumentNavn,
                Brevmottaker::getMottakerNavn,
                Brevmottaker::getRolle,
                Brevmottaker::getAktørId,
                Brevmottaker::getOrgnr)
            .containsExactly("Kopi til brukers fullmektig", "Fullmektig Virksomhet", REPRESENTANT, null, "orgnrTilFullmektig");
    }

    @Deprecated(since = "Erstattes av HentMottakereTest. Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    @Test
    void hentMuligeMottakere_kopiTilBrukerMedFullmektigNårHovedMottakerErBruker_returnererBrukerSomKopi() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(BRUKER).medKopiMottaker(BRUKER).build());
        when(brevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)))
            .thenReturn(lagAktoerOrg(REPRESENTANT, "orgnrTilFullmektig"));
        when(persondataFasade.hentSammensattNavn(anyString())).thenReturn("Ola Nordmann");
        mockHentOrganisasjon("orgnrTilFullmektig", "Fullmektig Virksomhet");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, MANGELBREV_BRUKER, BRUKER)).thenReturn(MANGELBREV_BRUKER.getBeskrivelse());

        var muligeMottakere = brevbestillingServiceOld.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, null);

        assertThat(muligeMottakere.getHovedMottaker())
            .extracting(
                Brevmottaker::getDokumentNavn,
                Brevmottaker::getMottakerNavn,
                Brevmottaker::getRolle,
                Brevmottaker::getAktørId,
                Brevmottaker::getOrgnr)
            .containsExactly(MANGELBREV_BRUKER.getBeskrivelse(), "Fullmektig Virksomhet", BRUKER, null, null);
        assertThat(muligeMottakere.getKopiMottakere())
            .flatExtracting(
                Brevmottaker::getDokumentNavn,
                Brevmottaker::getMottakerNavn,
                Brevmottaker::getRolle,
                Brevmottaker::getAktørId,
                Brevmottaker::getOrgnr)
            .containsExactly("Kopi til bruker", "Ola Nordmann", BRUKER, "aktørId", null);
    }

    @Deprecated(since = "Erstattes av HentMottakereTest. Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    @Test
    void hentMuligeMottakere_kopiTilArbeidsgiver_returnererArbeidsgiverSomKopi() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(BRUKER).medKopiMottaker(ARBEIDSGIVER).build());
        when(brevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)))
            .thenReturn(lagAktoerOrg(BRUKER, null));
        Aktoer orgnr1 = lagAktoerOrg(ARBEIDSGIVER, "orgnr1");
        Aktoer orgnr2 = lagAktoerOrg(ARBEIDSGIVER, "orgnr2");
        when(brevmottakerService.avklarMottakere(eq(MANGELBREV_BRUKER), any(), eq(behandling), anyBoolean(), anyBoolean()))
            .thenReturn(List.of(orgnr1, orgnr2));
        mockHentOrganisasjon("orgnr1", "Arbeidsgiver 1");
        mockHentOrganisasjon("orgnr2", "Arbeidsgiver 2");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, MANGELBREV_BRUKER, BRUKER)).thenReturn(MANGELBREV_BRUKER.getBeskrivelse());
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, MANGELBREV_BRUKER, orgnr1, "Kopi til arbeidsgiver")).thenReturn("Kopi til arbeidsgiver");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, MANGELBREV_BRUKER, orgnr2, "Kopi til arbeidsgiver")).thenReturn("Kopi til arbeidsgiver");


        var muligeMottakere = brevbestillingServiceOld.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, null);

        assertThat(muligeMottakere.getKopiMottakere())
            .flatExtracting(
                Brevmottaker::getDokumentNavn,
                Brevmottaker::getMottakerNavn,
                Brevmottaker::getRolle,
                Brevmottaker::getAktørId,
                Brevmottaker::getOrgnr)
            .containsExactly(
                "Kopi til arbeidsgiver", "Arbeidsgiver 1", ARBEIDSGIVER, null, "orgnr1",
                "Kopi til arbeidsgiver", "Arbeidsgiver 2", ARBEIDSGIVER, null, "orgnr2");
    }

    @Deprecated(since = "Erstattes av HentMottakereTest. Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    @Test
    void hentMuligeMottakere_kopiTilArbeidsgiverMedFullmektig_returnererFullmektigSomKopi() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(BRUKER).medKopiMottaker(ARBEIDSGIVER).build());
        when(brevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)))
            .thenReturn(lagAktoerOrg(BRUKER, null));
        Aktoer orgnr = lagAktoerOrg(REPRESENTANT, "orgnr");
        when(brevmottakerService.avklarMottakere(eq(MANGELBREV_BRUKER), any(), eq(behandling), anyBoolean(), anyBoolean()))
            .thenReturn(List.of(orgnr));
        mockHentOrganisasjon("orgnr", "Fullmektig Virksomhet");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, MANGELBREV_BRUKER, BRUKER)).thenReturn(MANGELBREV_BRUKER.getBeskrivelse());
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, MANGELBREV_BRUKER, orgnr, "Kopi til arbeidsgivers fullmektig")).thenReturn("Kopi til arbeidsgivers fullmektig");


        var muligeMottakere = brevbestillingServiceOld.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, null);

        assertThat(muligeMottakere.getKopiMottakere())
            .flatExtracting(
                Brevmottaker::getDokumentNavn,
                Brevmottaker::getMottakerNavn,
                Brevmottaker::getRolle,
                Brevmottaker::getAktørId,
                Brevmottaker::getOrgnr)
            .containsExactly(
                "Kopi til arbeidsgivers fullmektig", "Fullmektig Virksomhet", REPRESENTANT, null, "orgnr");
    }

    @Deprecated(since = "Tas vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    @Test
    void hentMuligeMottakere_fastTilSkatt_returnererSkattSomFast() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(BRUKER).medFastMottaker(SKATTEETATEN).build());
        when(brevmottakerService.avklarMottaker(MANGELBREV_BRUKER, Mottaker.av(BRUKER), behandling))
            .thenReturn(lagAktoerOrg(BRUKER, null));
        Aktoer skatteetaten = FastMottakerMedOrgnr.av(SKATTEETATEN).getAktør();
        when(brevmottakerService.avklarMottaker(MANGELBREV_BRUKER, FastMottakerMedOrgnr.av(SKATTEETATEN), behandling))
            .thenReturn(skatteetaten);
        mockHentOrganisasjon("974761076", "Skatteetaten");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, MANGELBREV_BRUKER, BRUKER)).thenReturn(MANGELBREV_BRUKER.getBeskrivelse());
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, MANGELBREV_BRUKER, skatteetaten, "Kopi til Skatteetaten")).thenReturn("Kopi til Skatteetaten");

        var muligeMottakere = brevbestillingServiceOld.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, null);

        assertThat(muligeMottakere.getFasteMottakere())
            .flatExtracting(
                Brevmottaker::getDokumentNavn,
                Brevmottaker::getMottakerNavn,
                Brevmottaker::getRolle,
                Brevmottaker::getAktørId,
                Brevmottaker::getOrgnr)
            .containsExactly("Kopi til Skatteetaten", "Skatteetaten", TRYGDEMYNDIGHET, null, "974761076");
    }


    @Deprecated(since = "Tas vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    @Test
    void hentMuligeMottakere_hovedMottakerBruker_storbritanniaArtikkelUlik82() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(TRYGDEAVTALE_GB, 123L))
            .thenReturn(new Mottakerliste.Builder()
                .medHovedMottaker(BRUKER)
                .medKopiMottaker(ARBEIDSGIVER)
                .medKopiMottaker(TRYGDEMYNDIGHET)
                .medFastMottaker(SKATTEETATEN)
                .build());
        when(brevmottakerService.avklarMottaker(eq(TRYGDEAVTALE_GB), any(), eq(behandling)))
            .thenReturn(lagAktoerOrg(BRUKER, null));
        Aktoer arbeidsgiver = lagAktoerOrg(ARBEIDSGIVER, "123");
        when(brevmottakerService.avklarMottakere(TRYGDEAVTALE_GB, Mottaker.av(ARBEIDSGIVER), behandling, false, true))
            .thenReturn(List.of(arbeidsgiver));
        Aktoer trygdemyndighet = lagAktoerOrg(TRYGDEMYNDIGHET, "456");
        when(brevmottakerService.avklarMottakere(TRYGDEAVTALE_GB, Mottaker.av(TRYGDEMYNDIGHET), behandling))
            .thenReturn(List.of(trygdemyndighet));
        Aktoer skatteetaten = lagAktoerOrg(TRYGDEMYNDIGHET, "974761076");
        when(brevmottakerService.avklarMottaker(TRYGDEAVTALE_GB, FastMottakerMedOrgnr.av(SKATTEETATEN), behandling))
            .thenReturn(skatteetaten);
        when(persondataFasade.hentSammensattNavn(anyString())).thenReturn("Ola Nordmann");
        mockHentOrganisasjon("123", "Ståle Stål");
        mockHentOrganisasjon("974761076", "Skatt");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, TRYGDEAVTALE_GB, BRUKER)).thenReturn("Vedtak om medlemskap, Attest for utsendt arbeidstaker");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, TRYGDEAVTALE_GB, arbeidsgiver, "Kopi til arbeidsgiver")).thenReturn("Kopi av vedtak om medlemskap, Attest for utsendt arbeidstaker");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, TRYGDEAVTALE_GB, trygdemyndighet, "Kopi til utenlandsk trygdemyndighet")).thenReturn("Attest for utsendt arbeidstaker", "Utenlandsk trygdemyndighet");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, TRYGDEAVTALE_GB, skatteetaten, "Kopi til Skatt")).thenReturn("Kopi av vedtak om medlemskap");

        var muligeMottakere = brevbestillingServiceOld.hentMuligeMottakere(TRYGDEAVTALE_GB, 123L, null);

        assertThat(muligeMottakere.getHovedMottaker())
            .extracting(
                Brevmottaker::getDokumentNavn,
                Brevmottaker::getMottakerNavn,
                Brevmottaker::getRolle,
                Brevmottaker::getAktørId,
                Brevmottaker::getOrgnr)
            .containsExactly("Vedtak om medlemskap, Attest for utsendt arbeidstaker", "Ola Nordmann", BRUKER, null, null);

        assertThat(muligeMottakere.getKopiMottakere())
            .hasSize(2)
            .extracting(
                Brevmottaker::getDokumentNavn,
                Brevmottaker::getMottakerNavn
            )
            .containsExactlyInAnyOrder(
                tuple("Kopi av vedtak om medlemskap, Attest for utsendt arbeidstaker", "Ståle Stål"),
                tuple("Attest for utsendt arbeidstaker", "Utenlandsk trygdemyndighet")
            );

        assertThat(muligeMottakere.getFasteMottakere())
            .hasSize(1)
            .extracting(
                Brevmottaker::getDokumentNavn,
                Brevmottaker::getMottakerNavn
            )
            .containsExactly(
                tuple("Kopi av vedtak om medlemskap", "Skatt")
            );
    }

    @Deprecated(since = "Tas vekk sammen med melosys.MEL-4835.refactor1 toggle")
    @Test
    void skalBestilleProduseringAvBrev() {
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(MANGELBREV_BRUKER);


        brevbestillingServiceOld.produserBrev(333L, brevbestillingDto);


        verify(dokServiceFasade).produserDokument(anyLong(), any(BrevbestillingDto.class));
    }

    @Deprecated(since = "Tas vekk sammen med melosys.MEL-4835.refactor1 toggle")
    @Test
    void produserBrev_InnvilgelseFtrl_skalIkkeTillates() {
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(INNVILGELSE_FOLKETRYGDLOVEN_2_8);


        assertThatThrownBy(() -> brevbestillingServiceOld.produserBrev(333L, brevbestillingDto))
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("Manuell bestilling av INNVILGELSE_FOLKETRYGDLOVEN_2_8 er ikke støttet.");
    }

    @Deprecated(since = "Tas vekk sammen med melosys.MEL-4835.refactor1 toggle")
    @Test
    void skalReturnereUtkast() {
        byte[] pdf = "UTKAST".getBytes(StandardCharsets.UTF_8);
        when(dokServiceFasade.produserUtkast(anyLong(), any())).thenReturn(pdf);
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(MANGELBREV_BRUKER);
        
        byte[] utkast = brevbestillingServiceOld.produserUtkast(333L, brevbestillingDto);

        assertThat(utkast).isEqualTo(pdf);
        verify(dokServiceFasade).produserUtkast(333L, brevbestillingDto);
    }


    private Aktoer lagAktoerOrg(Aktoersroller aktoersroller, String orgNummer) {
        var aktoer = new Aktoer();
        aktoer.setRolle(aktoersroller);
        aktoer.setOrgnr(orgNummer);
        return aktoer;
    }

    private Aktoer lagAktoerPerson(Aktoersroller aktoersroller, String personIdent) {
        var aktoer = new Aktoer();
        aktoer.setRolle(aktoersroller);
        aktoer.setPersonIdent(personIdent);
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
        when(eregFasade.hentOrganisasjon(orgnr)).thenReturn(lagOrgSaksopplysning(orgnr, navn));
    }

    private void mockFinnOrganisasjon(String orgnr, String navn) {
        when(eregFasade.finnOrganisasjon(orgnr)).thenReturn(Optional.of(lagOrgSaksopplysning(orgnr, navn)));
    }

    private Saksopplysning lagOrgSaksopplysning(String orgNummer, String navn) {
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
}
