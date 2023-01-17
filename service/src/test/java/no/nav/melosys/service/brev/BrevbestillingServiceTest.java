package no.nav.melosys.service.brev;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.brev.FastMottakerMedOrgnr;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.person.Navn;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.Personopplysninger;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.MuligMottakerDto;
import no.nav.melosys.service.dokument.MuligeMottakereDto;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static no.nav.melosys.domain.brev.FastMottakerMedOrgnr.HELFO;
import static no.nav.melosys.domain.brev.FastMottakerMedOrgnr.SKATTEETATEN;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysningerUtenOppholdsadresseOgKontaktadresse;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrevbestillingServiceTest {

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
    private BrevbestillingService brevbestillingService;

    @BeforeEach
    void init() {
        brevbestillingService = new BrevbestillingService(brevmottakerService, dokServiceFasade, behandlingService, eregFasade,
            kontaktopplysningService, persondataFasade, dokumentNavnService, utenlandskMyndighetService);
    }

    @Test
    void hentMuligeMottakere_hovedMottakerBruker_returnererBrukerSomHovedMottaker() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123L))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(BRUKER).build());
        when(brevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)))
            .thenReturn(lagAktoerPerson(BRUKER, null));
        when(persondataFasade.hentSammensattNavn(anyString())).thenReturn("Ola Nordmann");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, MANGELBREV_BRUKER, BRUKER)).thenReturn(MANGELBREV_BRUKER.getBeskrivelse());

        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, null);

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
    void hentMuligeMottakere_hovedMottakerBrukerMedFullmektigOrganisasjon_returnererFullmektigOrganisasjonSomHovedMottaker() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(BRUKER).build());
        when(brevmottakerService.avklarMottaker(any(Produserbaredokumenter.class), any(), eq(behandling)))
            .thenReturn(lagAktoerOrg(REPRESENTANT, "orgnr"));
        mockHentOrganisasjon("orgnr", "Fullmektig virksomhet");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, MANGELBREV_BRUKER, BRUKER)).thenReturn(MANGELBREV_BRUKER.getBeskrivelse());

        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, null);

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
    void hentMuligeMottakere_hovedMottakerBrukerMedFullmektigPerson_returnererFullmektigPersonSomHovedMottaker() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(BRUKER).build());
        when(brevmottakerService.avklarMottaker(any(Produserbaredokumenter.class), any(), eq(behandling)))
            .thenReturn(lagAktoerPerson(REPRESENTANT, "fnr"));
        when(persondataFasade.hentSammensattNavn("fnr")).thenReturn("Ola Nordmann");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, MANGELBREV_BRUKER, BRUKER)).thenReturn(MANGELBREV_BRUKER.getBeskrivelse());

        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, null);

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
    void hentMuligeMottakere_hovedMottakerVirksomhet_returnererVirksomhetSomHovedMottaker() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(GENERELT_FRITEKSTBREV_VIRKSOMHET, 123L))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(VIRKSOMHET).build());
        mockFinnOrganisasjon("orgnr", "Equinor AS");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, GENERELT_FRITEKSTBREV_VIRKSOMHET, VIRKSOMHET))
            .thenReturn(GENERELT_FRITEKSTBREV_VIRKSOMHET.getBeskrivelse());

        var muligeMottakere = brevbestillingService.hentMuligeMottakere(GENERELT_FRITEKSTBREV_VIRKSOMHET, 123L, "orgnr");

        assertThat(muligeMottakere.getHovedMottaker())
            .extracting(
                MuligMottakerDto::getDokumentNavn,
                MuligMottakerDto::getMottakerNavn,
                MuligMottakerDto::getRolle,
                MuligMottakerDto::getAktørId,
                MuligMottakerDto::getOrgnr)
            .containsExactly(GENERELT_FRITEKSTBREV_VIRKSOMHET.getBeskrivelse(), "Equinor AS", VIRKSOMHET, null, null);
        assertThat(muligeMottakere)
            .extracting(MuligeMottakereDto::getKopiMottakere, MuligeMottakereDto::getFasteMottakere)
            .containsExactly(emptyList(), emptyList());
    }

    @Test
    void hentMuligeMottakere_hovedMottakerArbeidsgiver_returnererArbeidsgiverSomHovedMottaker() {
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(ARBEIDSGIVER).build());
        mockFinnOrganisasjon("orgnr", "Ola Nordmann Rørleggerfirma");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(null, MANGELBREV_BRUKER, ARBEIDSGIVER))
            .thenReturn(MANGELBREV_BRUKER.getBeskrivelse());

        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, "orgnr");

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
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(ARBEIDSGIVER).medKopiMottaker(BRUKER).build());
        when(persondataFasade.hentSammensattNavn(anyString())).thenReturn("Ola Nordmann");
        mockFinnOrganisasjon("orgnr", "Ola Nordmann Rørleggerfirma");

        when(brevmottakerService.avklarMottaker(any(Produserbaredokumenter.class), any(), eq(behandling)))
            .thenReturn(lagAktoerOrg(BRUKER, null));

        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, "orgnr");

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
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(ARBEIDSGIVER).medKopiMottaker(BRUKER).build());
        when(brevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)))
            .thenReturn(lagAktoerOrg(REPRESENTANT, "orgnrTilFullmektig"));
        mockFinnOrganisasjon("orgnr", "Ola Nordmann Rørleggerfirma");
        mockHentOrganisasjon("orgnrTilFullmektig", "Fullmektig Virksomhet");


        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, "orgnr");

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
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123))
            .thenReturn(new Mottakerliste.Builder().medHovedMottaker(BRUKER).medKopiMottaker(BRUKER).build());
        when(brevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)))
            .thenReturn(lagAktoerOrg(REPRESENTANT, "orgnrTilFullmektig"));
        when(persondataFasade.hentSammensattNavn(anyString())).thenReturn("Ola Nordmann");
        mockHentOrganisasjon("orgnrTilFullmektig", "Fullmektig Virksomhet");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, MANGELBREV_BRUKER, BRUKER)).thenReturn(MANGELBREV_BRUKER.getBeskrivelse());

        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, null);

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


        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, null);

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


        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, null);

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

        var muligeMottakere = brevbestillingService.hentMuligeMottakere(MANGELBREV_BRUKER, 123L, null);

        assertThat(muligeMottakere.getFasteMottakere())
            .flatExtracting(
                MuligMottakerDto::getDokumentNavn,
                MuligMottakerDto::getMottakerNavn,
                MuligMottakerDto::getRolle,
                MuligMottakerDto::getAktørId,
                MuligMottakerDto::getOrgnr)
            .containsExactly("Kopi til Skatteetaten", "Skatteetaten", TRYGDEMYNDIGHET, null, "974761076");
    }

    @Test
    void hentBrevMaler_tilBruker_returnererKorrektListe() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);

        List<Produserbaredokumenter> brevMaler = brevbestillingService.hentMuligeProduserbaredokumenter(123L, BRUKER);

        assertThat(brevMaler)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                MANGELBREV_BRUKER,
                GENERELT_FRITEKSTBREV_BRUKER
            );
    }

    @Test
    void hentBrevMaler_tilArbeidsgiver_returnererKorrektListe() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);

        List<Produserbaredokumenter> brevMaler = brevbestillingService.hentMuligeProduserbaredokumenter(123L, ARBEIDSGIVER);

        assertThat(brevMaler)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                MANGELBREV_ARBEIDSGIVER,
                GENERELT_FRITEKSTBREV_ARBEIDSGIVER
            );
    }

    @Test
    void hentBrevMaler_tilVirksomhet_returnererKorrektListe() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);

        List<Produserbaredokumenter> brevMaler = brevbestillingService.hentMuligeProduserbaredokumenter(123L, VIRKSOMHET);

        assertThat(brevMaler).hasSize(1).containsExactly(GENERELT_FRITEKSTBREV_VIRKSOMHET);
    }

    @Test
    void hentBrevMaler_behandlingAvsluttet_returnererTomListe() {
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        when(behandlingService.hentBehandlingMedSaksopplysninger(321L)).thenReturn(behandling);
        List<Produserbaredokumenter> brevMaler = brevbestillingService.hentMuligeProduserbaredokumenter(321L, BRUKER);

        assertThat(brevMaler).isEmpty();
    }

    @Test
    void hentBrevMaler_behandlingErFørstegangMedSakstemaMedlemskapLovvalg_returnererForventetSaksbehandlingstidMalITillegg() {
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.getFagsak().setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        when(behandlingService.hentBehandlingMedSaksopplysninger(321L)).thenReturn(behandling);
        List<Produserbaredokumenter> brevMaler = brevbestillingService.hentMuligeProduserbaredokumenter(321L, BRUKER);

        assertThat(brevMaler)
            .hasSize(3)
            .containsExactlyInAnyOrder(
                MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                MANGELBREV_BRUKER,
                GENERELT_FRITEKSTBREV_BRUKER
            );
    }

    @Test
    void hentBrevMaler_behandlingErKlage_returnererKorrekt() {
        behandling.setType(Behandlingstyper.KLAGE);
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        List<Produserbaredokumenter> brevMaler = brevbestillingService.hentMuligeProduserbaredokumenter(123L, BRUKER);

        assertThat(brevMaler)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                MANGELBREV_BRUKER,
                GENERELT_FRITEKSTBREV_BRUKER
            );
    }

    @Test
    void hentBrevAdresseTilMottakere_brukerSomMottaker_returnererBrukeradresse() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoerOrg(Aktoersroller.BRUKER, null)));
        when(persondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysningerUtenOppholdsadresseOgKontaktadresse());

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(Aktoersroller.BRUKER, 123);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand)
            .containsExactly(
                "Nordmann Ola",
                null,
                List.of("gatenavnFraBostedsadresse 3"),
                "1234",
                "Oslo",
                "Norge",
                "NO");
    }

    @Test
    void hentBrevAdresseTilMottakere_brukersFullmaktOrganisasjonSomMottaker_returnererFullmektigsAdresse() {
        var behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoerOrg(Aktoersroller.REPRESENTANT, "orgNr")));
        when(eregFasade.hentOrganisasjon("orgNr")).thenReturn(lagOrgSaksopplysning("orgNr", "Ola Nordmann Fullmektig"));

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(Aktoersroller.BRUKER, 123);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand)
            .containsExactly(
                "Ola Nordmann Fullmektig",
                "orgNr",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE);
    }

    @Test
    void hentBrevAdresseTilMottakere_brukersFullmaktPersonSomMottaker_returnererFullmektigsAdresse() {
        var behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoerPerson(Aktoersroller.REPRESENTANT, "fnr")));
        when(persondataFasade.hentPerson("fnr")).thenReturn(lagPersonopplysningerUtenOppholdsadresseOgKontaktadresse());

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(Aktoersroller.BRUKER, 123);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand)
            .containsExactly(
                "Nordmann Ola",
                null,
                List.of("gatenavnFraBostedsadresse 3"),
                "1234",
                "Oslo",
                "Norge",
                "NO");
    }

    @Test
    void hentBrevAdresseTilMottakere_arbeidsgiverSomMottaker_returnererArbeidsgiverAdresser() {
        var behandling = new Behandling();
        behandling.setFagsak(new Fagsak());

        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.ARBEIDSGIVER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoerOrg(Aktoersroller.ARBEIDSGIVER, "orgNr1"), lagAktoerOrg(Aktoersroller.ARBEIDSGIVER, "orgNr2")));
        when(eregFasade.hentOrganisasjon("orgNr1")).thenReturn(lagOrgSaksopplysning("orgNr1", "Ola Nordmann Rørleggerfirma"));
        when(eregFasade.hentOrganisasjon("orgNr2")).thenReturn(lagOrgSaksopplysning("orgNr2", "Ida Nordmann Rørleggerfirma"));

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(Aktoersroller.ARBEIDSGIVER, 123);

        assertThat(brevAdresser).hasSize(2);
        assertThat(brevAdresser.get(0))
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand)
            .containsExactly(
                "Ola Nordmann Rørleggerfirma",
                "orgNr1",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE);
        assertThat(brevAdresser.get(1))
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand)
            .containsExactly(
                "Ida Nordmann Rørleggerfirma",
                "orgNr2",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE);
    }

    @Test
    void hentBrevAdresseTilMottakere_arbeidsgiverSomMottakerMenIngenArbeidsgivere_returnererTomListe() {
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.ARBEIDSGIVER)), any(), eq(false), eq(false)))
            .thenReturn(emptyList());

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(Aktoersroller.ARBEIDSGIVER, 123L);

        assertThat(brevAdresser).isEmpty();
    }

    @Test
    void hentBrevAdresseTilMottakere_arbeidsgiversFullmaktSomMottaker_returnererFullmektigsAdresse() {
        var behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.ARBEIDSGIVER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoerOrg(Aktoersroller.REPRESENTANT, "orgNr")));
        when(eregFasade.hentOrganisasjon("orgNr")).thenReturn(lagOrgSaksopplysning("orgNr", "Ola Nordmann Fullmektig"));

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(Aktoersroller.ARBEIDSGIVER, 123);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr, BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand)
            .containsExactly(
                "Ola Nordmann Fullmektig",
                "orgNr",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE);
    }

    @Test
    void hentBrevAdresseTilMottakere_virksomhetSomMottaker_returnererVirksomhetAdresse() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(VIRKSOMHET)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoerOrg(VIRKSOMHET, "orgNr1")));
        when(eregFasade.hentOrganisasjon("orgNr1")).thenReturn(lagOrgSaksopplysning("orgNr1", "Ola Nordmann Rørleggerfirma"));

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(VIRKSOMHET, 123);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand)
            .containsExactly(
                "Ola Nordmann Rørleggerfirma",
                "orgNr1",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE);
    }

    @Test
    void hentBrevAdresseTilMottakere_returnererAdresseFelterSomNull_nårGjeldendePostadresseErNull() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoerOrg(Aktoersroller.BRUKER, null)));
        Personopplysninger persondata = PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser();
        when(persondataFasade.hentPerson(anyString())).thenReturn(persondata);

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(Aktoersroller.BRUKER, 123L);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand)
            .containsExactly("Nordmann Ola", null, null, null, null, null, null);
    }

    @Test
    void hentBrevAdresseTilMottakere_returnererAdresseMedKorrektAdresselinjer_nårCoAdresseErTomStreng() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoerOrg(Aktoersroller.BRUKER, null)));
        when(persondataFasade.hentPerson(anyString())).thenReturn(lagPersondataMedTomCo());

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(Aktoersroller.BRUKER, 123L);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0).getAdresselinjer()).isEqualTo(List.of("gatenavnFraBostedsadresse 3"));
    }

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

        var muligeMottakere = brevbestillingService.hentMuligeMottakere(TRYGDEAVTALE_GB, 123L, null);

        assertThat(muligeMottakere.getHovedMottaker())
            .extracting(
                MuligMottakerDto::getDokumentNavn,
                MuligMottakerDto::getMottakerNavn,
                MuligMottakerDto::getRolle,
                MuligMottakerDto::getAktørId,
                MuligMottakerDto::getOrgnr)
            .containsExactly("Vedtak om medlemskap, Attest for utsendt arbeidstaker", "Ola Nordmann", BRUKER, null, null);

        assertThat(muligeMottakere.getKopiMottakere())
            .hasSize(2)
            .extracting(
                MuligMottakerDto::getDokumentNavn,
                MuligMottakerDto::getMottakerNavn
            )
            .containsExactlyInAnyOrder(
                tuple("Kopi av vedtak om medlemskap, Attest for utsendt arbeidstaker", "Ståle Stål"),
                tuple("Attest for utsendt arbeidstaker", "Utenlandsk trygdemyndighet")
            );

        assertThat(muligeMottakere.getFasteMottakere())
            .hasSize(1)
            .extracting(
                MuligMottakerDto::getDokumentNavn,
                MuligMottakerDto::getMottakerNavn
            )
            .containsExactly(
                tuple("Kopi av vedtak om medlemskap", "Skatt")
            );
    }

    @Test
    void skalBestilleProduseringAvBrev() {
        BrevbestillingRequest brevbestillingRequest = new BrevbestillingRequest.Builder().medProduserbardokument(MANGELBREV_BRUKER).build();
        brevbestillingService.produserBrev(333L, brevbestillingRequest);

        verify(dokServiceFasade).produserDokument(anyLong(), any(BrevbestillingRequest.class));
    }

    @Test
    void produserBrev_InnvilgelseFtrl_skalIkkeTillates() {
        BrevbestillingRequest brevbestillingRequest = new BrevbestillingRequest.Builder().medProduserbardokument(INNVILGELSE_FOLKETRYGDLOVEN_2_8).build();
        assertThatThrownBy(() -> brevbestillingService.produserBrev(333L, brevbestillingRequest))
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("Manuell bestilling av INNVILGELSE_FOLKETRYGDLOVEN_2_8 er ikke støttet.");
    }

    @Test
    void skalReturnereUtkast() {
        byte[] pdf = "UTKAST".getBytes(StandardCharsets.UTF_8);
        when(dokServiceFasade.produserUtkast(anyLong(), any())).thenReturn(pdf);
        BrevbestillingRequest brevbestillingRequest = new BrevbestillingRequest.Builder().medProduserbardokument(MANGELBREV_BRUKER).build();

        byte[] utkast = brevbestillingService.produserUtkast(333L, brevbestillingRequest);

        assertThat(utkast).isEqualTo(pdf);
        verify(dokServiceFasade).produserUtkast(333L, brevbestillingRequest);
    }

    @Test
    void hentMuligeMottakereEtater_spørEtterSkatteetatenOgHelfo_fårSkatteetatenOgHelfoMottakere() {
        var orgnrEtater = List.of(SKATTEETATEN.getOrgnr(), HELFO.getOrgnr());
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        mockFinnOrganisasjon(SKATTEETATEN.getOrgnr(), "Skatteetaten-NAVNET");
        mockFinnOrganisasjon(HELFO.getOrgnr(), "HELFO-NAVNET");


        var muligMottakerDto = brevbestillingService.hentMuligeMottakereEtater(MANGELBREV_BRUKER, 123L, orgnrEtater);


        assertThat(muligMottakerDto)
            .hasSize(2)
            .first()
            .hasFieldOrPropertyWithValue("mottakerNavn", "Skatteetaten-NAVNET")
            .hasFieldOrPropertyWithValue("rolle", OFFENTLIG_ETAT)
            .hasFieldOrPropertyWithValue("orgnr", SKATTEETATEN.getOrgnr());
        assertThat(muligMottakerDto)
            .last()
            .hasFieldOrPropertyWithValue("mottakerNavn", "HELFO-NAVNET")
            .hasFieldOrPropertyWithValue("rolle", OFFENTLIG_ETAT)
            .hasFieldOrPropertyWithValue("orgnr", HELFO.getOrgnr());
    }

    @Test
    void hentMuligeMottakereEtater_spørEtterSkatteetatenOgUkjentOrgNr_fårIkkeFunnetFeilmelding() {
        var orgnrEtater = List.of("111111111");
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);

        assertThatThrownBy(() -> brevbestillingService.hentMuligeMottakereEtater(MANGELBREV_BRUKER, 123L, orgnrEtater))
            .isInstanceOf(IkkeFunnetException.class);
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

    private Persondata lagPersondataMedTomCo() {
        var bostedsadresse = new Bostedsadresse(
            new StrukturertAdresse("gatenavnFraBostedsadresse 3", null, null, null, null, null),
            "", null, null, null, null, false);
        return new Personopplysninger(
            emptyList(), bostedsadresse, null, emptySet(), null, null,
            null, emptySet(), new Navn(null, null, null), emptySet(), emptySet());
    }
}
