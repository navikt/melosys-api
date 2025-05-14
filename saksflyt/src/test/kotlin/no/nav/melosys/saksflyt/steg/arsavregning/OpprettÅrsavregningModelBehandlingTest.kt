package no.nav.melosys.saksflyt.steg.arsavregning

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.saksflyt.TestdataFactory
import no.nav.melosys.saksflyt.TestdataFactory.lagBruker
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningModel
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.persondata.PersondataService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith


@ExtendWith(MockKExtension::class)
class OpprettÅrsavregningModelBehandlingTest {
    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandslingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var persondataService: PersondataService

    @MockK
    private lateinit var årsavregningService: ÅrsavregningService

    @MockK
    private lateinit var mottatteOpplysningerService: MottatteOpplysningerService

    private lateinit var opprettÅrsavregningBehandling: OpprettÅrsavregningBehandling

    @BeforeEach
    fun setUp() {
        opprettÅrsavregningBehandling = OpprettÅrsavregningBehandling(
            fagsakService,
            behandlingService,
            årsavregningService,
            mottatteOpplysningerService
        )
    }


    @Test
    fun `opprette ny behandling ved skatteoppgjør med overlappende medlemskapsperiode og fakturert trygdeavgift`() {
        val prosessinstans = lagProsessInstans()

        val fagsak = lagFagsak()
        val behandling = lagBehandling()
        val årsavregningsBehandling = lagBehandling {
            id = 2
            type = Behandlingstyper.ÅRSAVREGNING
        }

        val behandlingsresultat = Behandlingsresultat().apply {
            this.behandling = behandling
            id = 2
            type = Behandlingsresultattyper.IKKE_FASTSATT
        }

        every { persondataService.hentAktørIdForIdent(any()) } returns AKTØR_ID
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak
        every {
            årsavregningService.hentSisteBehandlingsresultatMedInnvilgetMedlemskapsperiodeOgAvgiftsgrunnlag(
                fagsak.saksnummer,
                any()
            )
        } returns behandlingsresultat

        every {
            behandlingService.nyBehandling(
                fagsak,
                Behandlingsstatus.VURDER_DOKUMENT,
                Behandlingstyper.ÅRSAVREGNING,
                Behandlingstema.UTSENDT_ARBEIDSTAKER,
                null,
                null,
                any(),
                Behandlingsaarsaktyper.MELDING_FRA_SKATT,
                null
            )
        } returns årsavregningsBehandling

        every { behandslingsresultatService.hentBehandlingsresultat(årsavregningsBehandling.id) } returns behandlingsresultat
        every { årsavregningService.opprettÅrsavregning(any(), any()) } returns ÅrsavregningModel(
            112,
            2023,
            null,
            emptyList(),
            null,
            emptyList(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )

        every { mottatteOpplysningerService.opprettMottatteopplysningerForAarsavregning(any()) } just runs

        opprettÅrsavregningBehandling.utfør(prosessinstans)


        verify { årsavregningService.opprettÅrsavregning(behandlingsresultat.id, 2023) }


        prosessinstans.behandling.id shouldBe 2
    }

    private fun lagBehandling(block: Behandling.() -> Unit = {}): Behandling = TestdataFactory.lagBehandling().apply {
        block()
    }

    private fun lagFagsak(block: Fagsak.() -> Unit = {}): Fagsak = Fagsak(
        SAKSNUMMER,
        123L,
        Sakstyper.EU_EOS,
        Sakstemaer.MEDLEMSKAP_LOVVALG,
        Saksstatuser.OPPRETTET,
        null,
        mutableSetOf(lagBruker()),
        mutableListOf()
    ).apply {
        block()
    }

    private fun lagProsessInstans(block: Prosessinstans.() -> Unit = {}): Prosessinstans = Prosessinstans().apply {
        setData(ProsessDataKey.GJELDER_ÅR, GJELDER_ÅR)
        setData(ProsessDataKey.AKTØR_ID, AKTØR_ID)
        setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER)
        block()
    }

    companion object {
        const val AKTØR_ID = "456789123"
        const val GJELDER_ÅR = 2023
        const val SAKSNUMMER = "MEL-test"
    }
}
