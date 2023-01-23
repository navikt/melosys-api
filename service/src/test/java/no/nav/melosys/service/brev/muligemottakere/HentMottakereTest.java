package no.nav.melosys.service.brev.muligemottakere;

import no.nav.melosys.domain.*;
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
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.DokumentNavnService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.brev.FastMottakerMedOrgnr.SKATTEETATEN;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_BRUKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HentMottakereTest {


    private final Behandling behandling = lagBehandling();


    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BrevmottakerService brevmottakerService;
    @Mock
    private DokumentNavnService dokumentNavnService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private EregFasade eregFasade;
    @Mock
    private KontaktopplysningService kontaktopplysningService;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;

    private HentMottakere hentMottakere;


    @BeforeEach
    void init() {
        hentMottakere = new HentMottakere(behandlingService, brevmottakerService, dokumentNavnService, persondataFasade,
                eregFasade, kontaktopplysningService, utenlandskMyndighetService);
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

        var requestData = new HentMottakere.RequestData(MANGELBREV_BRUKER, 123L, null);


        var muligeMottakere = hentMottakere.hentMuligeMottakere(requestData);


        assertThat(muligeMottakere.hovedMottaker())
                .extracting(
                        MuligMottakerDto::getDokumentNavn,
                        MuligMottakerDto::getMottakerNavn,
                        MuligMottakerDto::getRolle,
                        MuligMottakerDto::getAktørId,
                        MuligMottakerDto::getOrgnr)
                .containsExactly(MANGELBREV_BRUKER.getBeskrivelse(), "Ola Nordmann", BRUKER, null, null);
        assertThat(muligeMottakere)
                .extracting(HentMottakere.ResponseData::kopiMottakere, HentMottakere.ResponseData::fasteMottakere)
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

        var requestData = new HentMottakere.RequestData(MANGELBREV_BRUKER, 123L, null);


        var muligeMottakere = hentMottakere.hentMuligeMottakere(requestData);


        assertThat(muligeMottakere.hovedMottaker())
                .extracting(
                        MuligMottakerDto::getDokumentNavn,
                        MuligMottakerDto::getMottakerNavn,
                        MuligMottakerDto::getRolle,
                        MuligMottakerDto::getAktørId,
                        MuligMottakerDto::getOrgnr)
                .containsExactly(MANGELBREV_BRUKER.getBeskrivelse(), "Fullmektig virksomhet", BRUKER, null, null);
        assertThat(muligeMottakere)
                .extracting(HentMottakere.ResponseData::kopiMottakere, HentMottakere.ResponseData::fasteMottakere)
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

        var requestData = new HentMottakere.RequestData(MANGELBREV_BRUKER, 123L, null);


        var muligeMottakere = hentMottakere.hentMuligeMottakere(requestData);


        assertThat(muligeMottakere.hovedMottaker())
                .extracting(
                        MuligMottakerDto::getDokumentNavn,
                        MuligMottakerDto::getMottakerNavn,
                        MuligMottakerDto::getRolle,
                        MuligMottakerDto::getAktørId,
                        MuligMottakerDto::getOrgnr)
                .containsExactly(MANGELBREV_BRUKER.getBeskrivelse(), "Ola Nordmann", BRUKER, null, null);
        assertThat(muligeMottakere)
                .extracting(HentMottakere.ResponseData::kopiMottakere, HentMottakere.ResponseData::fasteMottakere)
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

        var requestData = new HentMottakere.RequestData(GENERELT_FRITEKSTBREV_VIRKSOMHET, 123L, "orgnr");


        var muligeMottakere = hentMottakere.hentMuligeMottakere(requestData);


        assertThat(muligeMottakere.hovedMottaker())
                .extracting(
                        MuligMottakerDto::getDokumentNavn,
                        MuligMottakerDto::getMottakerNavn,
                        MuligMottakerDto::getRolle,
                        MuligMottakerDto::getAktørId,
                        MuligMottakerDto::getOrgnr)
                .containsExactly(GENERELT_FRITEKSTBREV_VIRKSOMHET.getBeskrivelse(), "Equinor AS", VIRKSOMHET, null, null);
        assertThat(muligeMottakere)
                .extracting(HentMottakere.ResponseData::kopiMottakere, HentMottakere.ResponseData::fasteMottakere)
                .containsExactly(emptyList(), emptyList());
    }

    @Test
    void hentMuligeMottakere_hovedMottakerArbeidsgiver_returnererArbeidsgiverSomHovedMottaker() {
        when(brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123))
                .thenReturn(new Mottakerliste.Builder().medHovedMottaker(ARBEIDSGIVER).build());
        mockFinnOrganisasjon("orgnr", "Ola Nordmann Rørleggerfirma");
        when(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(null, MANGELBREV_BRUKER, ARBEIDSGIVER))
                .thenReturn(MANGELBREV_BRUKER.getBeskrivelse());

        var requestData = new HentMottakere.RequestData(MANGELBREV_BRUKER, 123L, "orgnr");


        var muligeMottakere = hentMottakere.hentMuligeMottakere(requestData);


        assertThat(muligeMottakere.hovedMottaker())
                .extracting(
                        MuligMottakerDto::getDokumentNavn,
                        MuligMottakerDto::getMottakerNavn,
                        MuligMottakerDto::getRolle,
                        MuligMottakerDto::getAktørId,
                        MuligMottakerDto::getOrgnr)
                .containsExactly(MANGELBREV_BRUKER.getBeskrivelse(), "Ola Nordmann Rørleggerfirma", ARBEIDSGIVER, null, null);
        assertThat(muligeMottakere)
                .extracting(HentMottakere.ResponseData::kopiMottakere, HentMottakere.ResponseData::fasteMottakere)
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

        var requestData = new HentMottakere.RequestData(MANGELBREV_BRUKER, 123L, "orgnr");


        var muligeMottakere = hentMottakere.hentMuligeMottakere(requestData);


        assertThat(muligeMottakere.kopiMottakere())
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

        var requestData = new HentMottakere.RequestData(MANGELBREV_BRUKER, 123L, "orgnr");


        var muligeMottakere = hentMottakere.hentMuligeMottakere(requestData);


        assertThat(muligeMottakere.kopiMottakere())
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

        var requestData = new HentMottakere.RequestData(MANGELBREV_BRUKER, 123L, null);


        var muligeMottakere = hentMottakere.hentMuligeMottakere(requestData);


        assertThat(muligeMottakere.hovedMottaker())
                .extracting(
                        MuligMottakerDto::getDokumentNavn,
                        MuligMottakerDto::getMottakerNavn,
                        MuligMottakerDto::getRolle,
                        MuligMottakerDto::getAktørId,
                        MuligMottakerDto::getOrgnr)
                .containsExactly(MANGELBREV_BRUKER.getBeskrivelse(), "Fullmektig Virksomhet", BRUKER, null, null);
        assertThat(muligeMottakere.kopiMottakere())
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

        var requestData = new HentMottakere.RequestData(MANGELBREV_BRUKER, 123L, null);


        var muligeMottakere = hentMottakere.hentMuligeMottakere(requestData);


        assertThat(muligeMottakere.kopiMottakere())
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

        var requestData = new HentMottakere.RequestData(MANGELBREV_BRUKER, 123L, null);


        var muligeMottakere = hentMottakere.hentMuligeMottakere(requestData);


        assertThat(muligeMottakere.kopiMottakere())
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

        var requestData = new HentMottakere.RequestData(MANGELBREV_BRUKER, 123L, null);


        var muligeMottakere = hentMottakere.hentMuligeMottakere(requestData);


        assertThat(muligeMottakere.fasteMottakere())
                .flatExtracting(
                        MuligMottakerDto::getDokumentNavn,
                        MuligMottakerDto::getMottakerNavn,
                        MuligMottakerDto::getRolle,
                        MuligMottakerDto::getAktørId,
                        MuligMottakerDto::getOrgnr)
                .containsExactly("Kopi til Skatteetaten", "Skatteetaten", TRYGDEMYNDIGHET, null, "974761076");
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
