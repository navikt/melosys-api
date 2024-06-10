package no.nav.melosys.saksflyt.steg.arsavregning

import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Aarsavregning
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.saksflyt.TestdataFactory
import no.nav.melosys.saksflyt.TestdataFactory.lagBruker
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.persondata.PersondataService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.TrygdeavgiftOppsummeringService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate


@ExtendWith(MockKExtension::class)
class OpprettArsavregningTest {
    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandslingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    @MockK
    private lateinit var trygdeavgiftOppsummeringService: TrygdeavgiftOppsummeringService

    @MockK
    private lateinit var medlemskapsperiodeService: MedlemskapsperiodeService

    @MockK
    private lateinit var persondataService: PersondataService

    @MockK
    private lateinit var årsavregningService: ÅrsavregningService

    private lateinit var opprettArsavregning: OpprettArsavregning

    @BeforeEach
    fun setUp() {
        opprettArsavregning = OpprettArsavregning(
            fagsakService,
            persondataService,
            trygdeavgiftOppsummeringService,
            behandlingService,
            lovvalgsperiodeService,
            medlemskapsperiodeService,
            behandslingsresultatService,
            årsavregningService
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
            id = 2
            type = Behandlingsresultattyper.IKKE_FASTSATT
        }

        every { persondataService.hentAktørIdForIdent(any()) } returns AKTØR_ID
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every { trygdeavgiftOppsummeringService.harFagsakBehandlingerMedTrygdeavgift(fagsak.saksnummer) } returns true
        every { trygdeavgiftOppsummeringService.hentTrygdeavgiftBehandlinger(fagsak.saksnummer) } returns listOf(behandling)
        every { lovvalgsperiodeService.hentLovvalgsperioder(behandling.id) } returns listOf(Lovvalgsperiode().apply {
            fom = LocalDate.of(2023, 1, 1)
            tom = LocalDate.of(2023, 10, 10)
        })
        every { medlemskapsperiodeService.hentMedlemskapsperioder(behandling.id) } returns listOf(Medlemskapsperiode().apply {
            fom = LocalDate.of(2023, 1, 1)
            tom = LocalDate.of(2023, 10, 10)
        })

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
        every { årsavregningService.oppretteÅrsavregning(any(), any()) } returns Unit

        opprettArsavregning.utfør(prosessinstans)


        verify { årsavregningService.oppretteÅrsavregning(behandlingsresultat, 2023) }


        prosessinstans.behandling.id shouldBe 2
    }

    @Test
    fun `ikke opprette ny behandling ved skatteoppgjør uten fakturert trygdeavgift`() {
        val prosessinstans = lagProsessInstans()
        val fagsak = lagFagsak()

        every { persondataService.hentAktørIdForIdent(any()) } returns AKTØR_ID
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every { trygdeavgiftOppsummeringService.harFagsakBehandlingerMedTrygdeavgift(fagsak.saksnummer) } returns false


        opprettArsavregning.utfør(prosessinstans)


        verify { årsavregningService wasNot Called }
    }

    @Test
    fun `ikke opprette ny behandling ved skatteoppgjør uten overlappende medlemskapsperiode`() {
        val prosessinstans = lagProsessInstans()
        val fagsak = lagFagsak()
        val behandling = lagBehandling()

        every { persondataService.hentAktørIdForIdent(any()) } returns AKTØR_ID
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every { trygdeavgiftOppsummeringService.harFagsakBehandlingerMedTrygdeavgift(fagsak.saksnummer) } returns true
        every { trygdeavgiftOppsummeringService.hentTrygdeavgiftBehandlinger(fagsak.saksnummer) } returns listOf(behandling)
        every { lovvalgsperiodeService.hentLovvalgsperioder(behandling.id) } returns listOf(Lovvalgsperiode().apply {
            fom = LocalDate.of(2022, 1, 1)
            tom = LocalDate.of(2022, 10, 10)
        })
        every { medlemskapsperiodeService.hentMedlemskapsperioder(behandling.id) } returns listOf(Medlemskapsperiode().apply {
            fom = LocalDate.of(2022, 1, 1)
            tom = LocalDate.of(2022, 10, 10)
        })


        opprettArsavregning.utfør(prosessinstans)


        verify { årsavregningService wasNot Called }
    }


    @Test
    fun `opprette ny behandling ved skatteoppgjør med endring i tidligere skatteoppgjør og ikke avsluttet ennå med overlapp`() {
        val prosessinstans = lagProsessInstans()

        val behandling = lagBehandling {
            id = 1
            type = Behandlingstyper.ÅRSAVREGNING
        }
        val fagsak = lagFagsak {
            this.leggTilBehandling(behandling)
        }

        val behandlingsresultat = Behandlingsresultat().apply {
            id = 2
            type = Behandlingsresultattyper.IKKE_FASTSATT
            aarsavregning = Aarsavregning().apply {
                aar = GJELDER_ÅR
            }
        }

        every { persondataService.hentAktørIdForIdent(any()) } returns AKTØR_ID
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every { trygdeavgiftOppsummeringService.harFagsakBehandlingerMedTrygdeavgift(fagsak.saksnummer) } returns true
        every { trygdeavgiftOppsummeringService.hentTrygdeavgiftBehandlinger(fagsak.saksnummer) } returns listOf(behandling)

        every { behandslingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat
        every { årsavregningService.oppretteÅrsavregning(any(), any()) } returns Unit
        every { behandlingService.lagre(any()) } returns Unit

        opprettArsavregning.utfør(prosessinstans)


        verify { behandlingService.lagre(behandling) }
        verify { årsavregningService wasNot Called }
    }


    @Test
    fun `ikke opprette ny behandling ved skatteoppgjør med tidligere årsavregningsbehandling som ikke er avsluttet`() {

    }

    @Test
    fun `opprette ny behandling ved skatteoppgjør uten tidligere årsavregningsbehandling eller avsluttet årsavregningsbehandling`() {

    }

    private fun lagBehandling(block: Behandling.() -> Unit = {}): Behandling = TestdataFactory.lagBehandling().apply {
        block()
    }

    private fun lagFagsak(block: Fagsak.() -> Unit = {}): Fagsak = Fagsak(
        "MEL-test",
        123L,
        Sakstyper.EU_EOS,
        Sakstemaer.MEDLEMSKAP_LOVVALG,
        Saksstatuser.OPPRETTET,
        mutableSetOf(lagBruker()),
        mutableListOf()
    ).apply {
        block()
    }


    private fun lagProsessInstans(block: Prosessinstans.() -> Unit = {}): Prosessinstans = Prosessinstans().apply {
        setData(ProsessDataKey.GJELDER_PERIODE, GJELDER_ÅR)
        setData(ProsessDataKey.AKTØR_ID, AKTØR_ID)
        block()
    }

    companion object {
        const val AKTØR_ID = "456789123"
        const val GJELDER_ÅR = 2023
    }
}
