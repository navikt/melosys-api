package no.nav.melosys.service.brev.bestilling

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.buildForTest
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.service.behandling.BehandlingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class HentMuligeProduserbaredokumenterServiceTest {

    @MockK
    private lateinit var behandlingService: BehandlingService

    private lateinit var hentMuligeProduserbaredokumenterService: HentMuligeProduserbaredokumenterService

    private lateinit var behandling: Behandling
    private var BEHANDLING_ID = 123L

    @BeforeEach
    fun setUp() {
        hentMuligeProduserbaredokumenterService = HentMuligeProduserbaredokumenterService(behandlingService)
        behandling = lagBehandling()
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns behandling
    }

    @Test
    fun hentMuligeProduserbaredokumenter_tilBruker_returnererKorrektListe() {
        hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(BEHANDLING_ID, Mottakerroller.BRUKER)
            .shouldNotBeNull()
            .shouldHaveSize(2)
            .shouldContainInOrder(
                Produserbaredokumenter.MANGELBREV_BRUKER,
                Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER,
            )
    }

    @Test
    fun hentMuligeProduserbaredokumenter_tilArbeidsgiver_returnererKorrektListe() {
        hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(BEHANDLING_ID, Mottakerroller.ARBEIDSGIVER)
            .shouldNotBeNull()
            .shouldHaveSize(2)
            .shouldContainInOrder(Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER, Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER)
    }

    @Test
    fun hentMuligeProduserbaredokumenter_tilVirksomhet_returnererKorrektListe() {
        hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(BEHANDLING_ID, Mottakerroller.VIRKSOMHET)
            .shouldNotBeNull()
            .shouldHaveSize(1)
            .shouldContainInOrder(Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET)
    }

    @Test
    fun hentMuligeProduserbaredokumenter_behandlingAvsluttet_returnererTomListe() {
        behandling.status = Behandlingsstatus.AVSLUTTET
        hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(BEHANDLING_ID, Mottakerroller.BRUKER)
            .shouldNotBeNull()
            .shouldBeEmpty()
    }

    @Test
    fun hentMuligeProduserbaredokumenter_behandlingErFørstegangMedSakstemaMedlemskapLovvalg_returnererForventetSaksbehandlingstidMalITillegg() {
        behandling.type = Behandlingstyper.FØRSTEGANG
        behandling.fagsak.tema = Sakstemaer.MEDLEMSKAP_LOVVALG


        hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(BEHANDLING_ID, Mottakerroller.BRUKER)
            .shouldNotBeNull()
            .shouldHaveSize(3)
            .shouldContainInOrder(
                Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                Produserbaredokumenter.MANGELBREV_BRUKER,
                Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER,
            )
    }

    @Test
    fun hentMuligeProduserbaredokumenter_behandlingErNyVurderingMedSakstemaMedlemskapLovvalg_returnererForventetSaksbehandlingstidMalITillegg() {
        behandling.type = Behandlingstyper.NY_VURDERING
        behandling.fagsak.tema = Sakstemaer.MEDLEMSKAP_LOVVALG


        hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(BEHANDLING_ID, Mottakerroller.BRUKER)
            .shouldNotBeNull()
            .shouldHaveSize(3)
            .shouldContainInOrder(
                Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                Produserbaredokumenter.MANGELBREV_BRUKER,
                Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER
            )
    }

    @Test
    fun hentMuligeProduserbaredokumenter_bruker_behandlingErManglendeInnbetalingTrygdavgift_returnererKunFritekstbrev() {
        behandling.type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        behandling.fagsak.tema = Sakstemaer.MEDLEMSKAP_LOVVALG


        hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(BEHANDLING_ID, Mottakerroller.BRUKER)
            .shouldNotBeNull()
            .shouldHaveSize(1)
            .shouldContainInOrder(Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER)
    }

    @Test
    fun hentMuligeProduserbaredokumenter_arbeidsgiver_behandlingErManglendeInnbetalingTrygdavgift_returnererKunFritekstbrev() {
        behandling.type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        behandling.fagsak.tema = Sakstemaer.MEDLEMSKAP_LOVVALG


        hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(BEHANDLING_ID, Mottakerroller.ARBEIDSGIVER)
            .shouldNotBeNull()
            .shouldHaveSize(1)
            .shouldContainInOrder(Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER)
    }

    @Test
    fun hentMuligeProduserbaredokumenter_annenOrganisasjon_behandlingErManglendeInnbetalingTrygdavgift_returnererKunFritekstbrev() {
        behandling.type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        behandling.fagsak.aktører.add(Aktoer().apply { rolle = Aktoersroller.BRUKER })


        hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(BEHANDLING_ID, Mottakerroller.ANNEN_ORGANISASJON)
            .shouldNotBeNull()
            .shouldHaveSize(1)
            .shouldContainInOrder(Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET)
    }

    @Test
    fun hentMuligeProduserbaredokumenter_annenOrganisasjon_virksomhetErHovedpart_returnererKunFritekstbrev() {
        behandling.fagsak.aktører.add(Aktoer().apply { rolle = Aktoersroller.VIRKSOMHET })


        hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(BEHANDLING_ID, Mottakerroller.ANNEN_ORGANISASJON)
            .shouldNotBeNull()
            .shouldHaveSize(1)
            .shouldContainInOrder(Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET)
    }

    @Test
    fun hentMuligeProduserbaredokumenter_behandlingErKlage_returnererKorrekt() {
        behandling.type = Behandlingstyper.KLAGE


        hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(BEHANDLING_ID, Mottakerroller.BRUKER)
            .shouldNotBeNull()
            .shouldHaveSize(2)
            .shouldContainInOrder(
                Produserbaredokumenter.MANGELBREV_BRUKER,
                Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER
            )
    }

    @Test
    fun `Mulige produserbare dokumenter skal inkludere INNHENTING_AV_INNTEKTSOPPLYSNINGER for behandlinger av typen Årsavregning`() {
        behandling.type = Behandlingstyper.ÅRSAVREGNING

        hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(BEHANDLING_ID, Mottakerroller.BRUKER)
            .shouldNotBeNull()
            .shouldHaveSize(3)
            .shouldContainInOrder(
                Produserbaredokumenter.MANGELBREV_BRUKER,
                Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER,
                Produserbaredokumenter.INNHENTING_AV_INNTEKTSOPPLYSNINGER,
            )
    }

    private fun lagBehandling(): Behandling = Behandling.buildForTest { fagsak = FagsakTestFactory.lagFagsak() }
}
