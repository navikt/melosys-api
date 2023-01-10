package no.nav.melosys.tjenester.gui;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.BrevbestillingService;
import no.nav.melosys.service.brev.DokumentNavnService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.tjenester.gui.dto.brev.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.brev.Distribusjonstype.*;
import static no.nav.melosys.tjenester.gui.dto.brev.FeltvalgAlternativKode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrevmalListeByggerTest {

    @Mock
    private BehandlingService mockBehandlingService;
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
    private DokumentNavnService mockDokumentNavnService;

    private final FakeUnleash unleash = new FakeUnleash();
    private BrevmalListeBygger brevmalListeBygger;


    @BeforeEach
    void init() {
        unleash.enable("melosys.behandle_alle_saker");
        BrevbestillingService brevbestillingService = new BrevbestillingService(mockBrevmottakerService,
            mockDokServiceFasade, mockBehandlingService, mockEregFasade, mockKontaktopplysningService,
            mockPersondataFasade, mockDokumentNavnService, unleash);
        brevmalListeBygger = new BrevmalListeBygger(brevbestillingService, mockBehandlingService, unleash);
    }

    @Test
    void byggBrevmalDtoListe_brukerErHovedpart_returnererTilgjengeligeMaler() {
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling(Behandlingstyper.FØRSTEGANG));
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(Behandlingstyper.FØRSTEGANG));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());


        List<BrevmalDto> tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L);


        assertThat(tilgjengeligeMaler)
            .hasSize(3)
            .extracting(brevmalDto -> brevmalDto.getMottaker().getType())
            .contains(
                MottakerType.BRUKER_ELLER_BRUKERS_FULLMEKTIG.getBeskrivelse(),
                MottakerType.ARBEIDSGIVER_ELLER_ARBEIDSGIVERS_FULLMEKTIG.getBeskrivelse(),
                MottakerType.ANNEN_ORGANISASJON.getBeskrivelse());

        assertThat(tilgjengeligeMaler.get(0).getBrevTyper())
            .hasSize(3)
            .extracting(BrevmalTypeDto::getType)
            .contains(
                Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                Produserbaredokumenter.MANGELBREV_BRUKER,
                Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER);

        assertThat(tilgjengeligeMaler.get(1).getBrevTyper())
            .hasSize(2)
            .extracting(BrevmalTypeDto::getType)
            .contains(
                Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER,
                Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER);

        assertThat(tilgjengeligeMaler.get(2).getBrevTyper())
            .hasSize(2)
            .extracting(BrevmalTypeDto::getType)
            .contains(
                Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER,
                Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER);
    }

    @Test
    void byggBrevmalDtoListe_virksomhetErHovedpart_returnererTilgjengeligeMaler() {
        Aktoer virksomhet = new Aktoer();
        virksomhet.setRolle(Aktoersroller.VIRKSOMHET);
        var behandling = lagBehandling(Behandlingstyper.FØRSTEGANG);
        behandling.getFagsak().setAktører(Set.of(virksomhet));
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());


        List<BrevmalDto> tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L);


        assertThat(tilgjengeligeMaler)
            .hasSize(2)
            .extracting(brevmalDto -> brevmalDto.getMottaker().getType())
            .contains(
                MottakerType.VIRKSOMHET.getBeskrivelse(),
                MottakerType.ANNEN_ORGANISASJON.getBeskrivelse());

        assertThat(tilgjengeligeMaler.get(0).getBrevTyper())
            .hasSize(1)
            .extracting(BrevmalTypeDto::getType)
            .contains(
                Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET);

        assertThat(tilgjengeligeMaler.get(1).getBrevTyper())
            .hasSize(1)
            .extracting(BrevmalTypeDto::getType)
            .contains(
                Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET);
    }

    @Test
    void byggBrevmalDtoListe_behandlingHarTomFlyt_returnererIkkeArbeidsgiverArbeidsgiversFullmektig() {
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling(Behandlingstyper.HENVENDELSE));
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(Behandlingstyper.HENVENDELSE));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());


        List<BrevmalDto> tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L);


        assertThat(tilgjengeligeMaler)
            .hasSize(2)
            .extracting(brevmalDto -> brevmalDto.getMottaker().getType())
            .contains(
                MottakerType.BRUKER_ELLER_BRUKERS_FULLMEKTIG.getBeskrivelse(),
                MottakerType.ANNEN_ORGANISASJON.getBeskrivelse())
            .doesNotContain(
                MottakerType.ARBEIDSGIVER_ELLER_ARBEIDSGIVERS_FULLMEKTIG.getBeskrivelse());
    }

    @Test
    void byggBrevmalDtoListe_behandlingErFørstegangMedSakstemaMedlemskapLovvalg_returnererSoeknadMal() {
        var behandling = lagBehandling(Behandlingstyper.FØRSTEGANG);
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());


        List<BrevmalDto> tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L);


        assertThat(tilgjengeligeMaler).hasSize(3);

        assertThat(tilgjengeligeMaler.get(0).getBrevTyper())
            .hasSize(3)
            .extracting(BrevmalTypeDto::getType)
            .contains(
                Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                Produserbaredokumenter.MANGELBREV_BRUKER,
                Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER);

        assertThat(tilgjengeligeMaler.get(0).getBrevTyper().get(0))
            .extracting(
                BrevmalTypeDto::getType,
                BrevmalTypeDto::getFelter)
            .containsExactly(
                Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                null);
    }

    @Test
    void byggBrevmalDtoListe_behandlingErKlage_returnererKlageMal() {
        unleash.disableAll();
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling(Behandlingstyper.KLAGE));
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(Behandlingstyper.KLAGE));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());


        List<BrevmalDto> tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L);


        assertThat(tilgjengeligeMaler).hasSize(2);

        assertThat(tilgjengeligeMaler.get(0).getBrevTyper())
            .hasSize(3)
            .extracting(BrevmalTypeDto::getType)
            .contains(
                Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE,
                Produserbaredokumenter.MANGELBREV_BRUKER,
                Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER);

        assertThat(tilgjengeligeMaler.get(0).getBrevTyper().get(0))
            .extracting(
                BrevmalTypeDto::getType,
                BrevmalTypeDto::getFelter)
            .containsExactly(
                Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE,
                null);
    }

    @Test
    void byggBrevmalDtoListe_brukerAdresseNull_returnererMalMedFeilmelding() {
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling(Behandlingstyper.FØRSTEGANG));
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(Behandlingstyper.FØRSTEGANG));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());


        List<BrevmalDto> tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L);


        assertThat(tilgjengeligeMaler).hasSize(3);
        assertThat(tilgjengeligeMaler.get(0).getMottaker())
            .extracting(
                MottakerDto::getType,
                MottakerDto::getFeilmelding)
            .containsExactly(
                MottakerType.BRUKER_ELLER_BRUKERS_FULLMEKTIG.getBeskrivelse(),
                Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE.getBeskrivelse());
    }

    @Test
    void byggBrevmalDtoListe_registerOpplysningerIkkeHentet_returnererMalMedFeilmelding() {
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling(Behandlingstyper.FØRSTEGANG));
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(Behandlingstyper.FØRSTEGANG));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean()))
            .thenThrow(new TekniskException("Finner ikke arbeidsforholddokument"));


        List<BrevmalDto> tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L);


        assertThat(tilgjengeligeMaler).hasSize(3);
        assertThat(tilgjengeligeMaler.get(1).getMottaker())
            .extracting(
                MottakerDto::getType,
                MottakerDto::getFeilmelding)
            .containsExactly(
                MottakerType.ARBEIDSGIVER_ELLER_ARBEIDSGIVERS_FULLMEKTIG.getBeskrivelse(),
                Kontroll_begrunnelser.INGEN_ARBEIDSGIVERE.getBeskrivelse());
    }

    @Test
    void byggBrevmalDtoListe_mangelbrev_lagerRiktigeValg() {
        var behandling = lagBehandling(Behandlingstyper.FØRSTEGANG);
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());


        List<BrevmalDto> tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L);


        var mangelbrevMal = tilgjengeligeMaler.get(0).getBrevTyper().get(1);
        assertThat(mangelbrevMal.getType()).isEqualTo(Produserbaredokumenter.MANGELBREV_BRUKER);
        assertThat(mangelbrevMal.getFelter()).hasSize(2);

        var innledningFritekstFelt = mangelbrevMal.getFelter().get(0);
        assertThat(innledningFritekstFelt)
            .extracting(
                BrevmalFeltDto::getKode,
                BrevmalFeltDto::getFeltType,
                BrevmalFeltDto::isPaakrevd,
                BrevmalFeltDto::getHjelpetekst,
                BrevmalFeltDto::getTegnBegrensning)
            .containsExactly(
                BrevmalFeltKode.INNLEDNING_FRITEKST.getKode(),
                FeltType.FRITEKST,
                true,
                null,
                null);
        assertThat(innledningFritekstFelt.getValg().getValgType()).isEqualTo(FeltValgType.RADIO);
        assertThat(innledningFritekstFelt.getValg().getValgAlternativer())
            .hasSize(2)
            .flatExtracting(
                FeltvalgAlternativDto::getKode,
                FeltvalgAlternativDto::isVisFelt)
            .containsExactly(
                STANDARD.getKode(),
                false,
                FRITEKST.getKode(),
                true);

        assertThat(mangelbrevMal.getFelter().get(1))
            .extracting(
                BrevmalFeltDto::getKode,
                BrevmalFeltDto::getFeltType,
                BrevmalFeltDto::isPaakrevd,
                BrevmalFeltDto::getValg,
                BrevmalFeltDto::getHjelpetekst,
                BrevmalFeltDto::getTegnBegrensning)
            .containsExactly(
                BrevmalFeltKode.MANGLER_FRITEKST.getKode(),
                FeltType.FRITEKST,
                true,
                null,
                null,
                null);
    }

    @Test
    void byggBrevmalDtoListe_mangelbrevKlage_lagerRiktigeValg() {
        var behandling = lagBehandling(Behandlingstyper.KLAGE);
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());


        List<BrevmalDto> tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L);


        var mangelbrevMal = tilgjengeligeMaler.get(0).getBrevTyper().get(0);
        assertThat(mangelbrevMal.getType()).isEqualTo(Produserbaredokumenter.MANGELBREV_BRUKER);
        assertThat(mangelbrevMal.getFelter()).hasSize(2);

        var innledningFritekstFelt = mangelbrevMal.getFelter().get(0);
        assertThat(innledningFritekstFelt)
            .extracting(
                BrevmalFeltDto::getKode,
                BrevmalFeltDto::getFeltType,
                BrevmalFeltDto::isPaakrevd,
                BrevmalFeltDto::getHjelpetekst,
                BrevmalFeltDto::getTegnBegrensning)
            .containsExactly(
                BrevmalFeltKode.INNLEDNING_FRITEKST.getKode(),
                FeltType.FRITEKST,
                true,
                null,
                null);
        assertThat(innledningFritekstFelt.getValg().getValgType()).isEqualTo(FeltValgType.RADIO);
        assertThat(innledningFritekstFelt.getValg().getValgAlternativer())
            .hasSize(1)
            .flatExtracting(
                FeltvalgAlternativDto::getKode,
                FeltvalgAlternativDto::isVisFelt)
            .containsExactly(
                FRITEKST.getKode(),
                true);

        assertThat(mangelbrevMal.getFelter().get(1))
            .extracting(
                BrevmalFeltDto::getKode,
                BrevmalFeltDto::getFeltType,
                BrevmalFeltDto::isPaakrevd,
                BrevmalFeltDto::getValg,
                BrevmalFeltDto::getHjelpetekst,
                BrevmalFeltDto::getTegnBegrensning)
            .containsExactly(
                BrevmalFeltKode.MANGLER_FRITEKST.getKode(),
                FeltType.FRITEKST,
                true,
                null,
                null,
                null);
    }

    @Test
    void byggBrevmalDtoListe_EUEØS_lagerRiktigeTittelValgForFritekstbrev() {
        Behandling behandlingEUEOS = lagBehandling(Behandlingstyper.FØRSTEGANG);
        behandlingEUEOS.getFagsak().setType(Sakstyper.EU_EOS);
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandlingEUEOS);
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandlingEUEOS);
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());


        List<BrevmalDto> tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L);


        assertThat(tilgjengeligeMaler).hasSize(3);
        assertThat(tilgjengeligeMaler.get(0).getBrevTyper().get(2).getFelter().get(1).getValg().getValgAlternativer())
            .hasSize(2)
            .flatExtracting(
                FeltvalgAlternativDto::getKode,
                FeltvalgAlternativDto::isVisFelt)
            .containsExactly(
                HENVENDELSE_OM_TRYGDETILHØRLIGHET.getKode(),
                false,
                FRITEKST.getKode(),
                true);
    }

    @Test
    void byggBrevmalDtoListe_EUEØS_lagerRiktigeDistribusjonstyperForFritekstbrev() {
        Behandling behandlingEUEOS = lagBehandling(Behandlingstyper.FØRSTEGANG);
        behandlingEUEOS.getFagsak().setType(Sakstyper.EU_EOS);
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandlingEUEOS);
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandlingEUEOS);
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L);

        assertThat(tilgjengeligeMaler).hasSize(3);
        assertThat(tilgjengeligeMaler.get(0).getBrevTyper().get(2).getFelter().get(0).getValg().getValgAlternativer())
            .hasSize(3)
            .flatExtracting(
                FeltvalgAlternativDto::getKode,
                FeltvalgAlternativDto::isVisFelt)
            .containsExactly(
                VEDTAK.getKode(),
                false,
                VIKTIG.getKode(),
                false,
                ANNET.getKode(),
                false);
    }

    @Test
    void byggBrevmalDtoListe_FTRL_lagerRiktigeTittelValgForFritekstbrev() {
        Behandling behandlingFTRL = lagBehandling(Behandlingstyper.FØRSTEGANG);
        behandlingFTRL.getFagsak().setType(Sakstyper.FTRL);
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandlingFTRL);
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandlingFTRL);
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L);


        assertThat(tilgjengeligeMaler).hasSize(3);
        assertThat(tilgjengeligeMaler.get(0).getBrevTyper().get(2).getFelter().get(1).getValg().getValgAlternativer())
            .hasSize(4)
            .flatExtracting(
                FeltvalgAlternativDto::getKode,
                FeltvalgAlternativDto::isVisFelt)
            .containsExactly(
                CONFIRMATION_OF_MEMBERSHIP.getKode(),
                false,
                BEKREFTELSE_PÅ_MEDLEMSKAP.getKode(),
                false,
                HENVENDELSE_OM_MEDLEMSKAP.getKode(),
                false,
                FRITEKST.getKode(),
                true);
    }

    @Test
    void byggBrevmalDtoListe_FTRL_lagerRiktigeDistribusjonstyperForFritekstbrev() {
        Behandling behandlingFTRL = lagBehandling(Behandlingstyper.FØRSTEGANG);
        behandlingFTRL.getFagsak().setType(Sakstyper.FTRL);
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandlingFTRL);
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandlingFTRL);
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L);

        assertThat(tilgjengeligeMaler).hasSize(3);
        assertThat(tilgjengeligeMaler.get(0).getBrevTyper().get(2).getFelter().get(0).getValg().getValgAlternativer())
            .hasSize(3)
            .flatExtracting(
                FeltvalgAlternativDto::getKode,
                FeltvalgAlternativDto::isVisFelt)
            .containsExactly(
                VEDTAK.getKode(),
                false,
                VIKTIG.getKode(),
                false,
                ANNET.getKode(),
                false);
    }

    @Test
    void byggBrevmalDtoListe_trygdeavtale_lagerRiktigeTittelValgForFritekstbrev() {
        Behandling behandlingTrygdeavtale = lagBehandling(Behandlingstyper.FØRSTEGANG);
        behandlingTrygdeavtale.getFagsak().setType(Sakstyper.TRYGDEAVTALE);
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandlingTrygdeavtale);
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandlingTrygdeavtale);
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L);


        assertThat(tilgjengeligeMaler).hasSize(3);
        assertThat(tilgjengeligeMaler.get(0).getBrevTyper().get(2).getFelter().get(1).getValg().getValgAlternativer())
            .hasSize(2)
            .flatExtracting(
                FeltvalgAlternativDto::getKode,
                FeltvalgAlternativDto::isVisFelt)
            .containsExactly(
                HENVENDELSE_OM_MEDLEMSKAP.getKode(),
                false,
                FRITEKST.getKode(),
                true);
    }

    @Test
    void byggBrevmalDtoListe_trygdeavtale_lagerRiktigeDistribusjonstyperForFritekstbrev() {
        Behandling behandlingTrygdeavtale = lagBehandling(Behandlingstyper.FØRSTEGANG);
        behandlingTrygdeavtale.getFagsak().setType(Sakstyper.TRYGDEAVTALE);
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandlingTrygdeavtale);
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandlingTrygdeavtale);
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L);

        assertThat(tilgjengeligeMaler).hasSize(3);
        assertThat(tilgjengeligeMaler.get(0).getBrevTyper().get(2).getFelter().get(0).getValg().getValgAlternativer())
            .hasSize(3)
            .flatExtracting(
                FeltvalgAlternativDto::getKode,
                FeltvalgAlternativDto::isVisFelt)
            .containsExactly(
                VEDTAK.getKode(),
                false,
                VIKTIG.getKode(),
                false,
                ANNET.getKode(),
                false);
    }

    private Behandling lagBehandling(Behandlingstyper type) {
        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        var fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        fagsak.setAktører(Set.of(bruker));
        var behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setType(type);
        return behandling;
    }
}
