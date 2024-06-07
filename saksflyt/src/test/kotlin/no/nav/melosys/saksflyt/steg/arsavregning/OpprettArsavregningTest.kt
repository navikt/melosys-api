package no.nav.melosys.saksflyt.steg.arsavregning

import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.saksflyt.TestdataFactory
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
        val prosessinstans = Prosessinstans().apply {
            setData(ProsessDataKey.GJELDER_PERIODE, "2023")
            setData(ProsessDataKey.AKTØR_ID, "456789123")
        }

        val fagsak = TestdataFactory.lagFagsak()
        val behandling = TestdataFactory.lagBehandling()
        val årsavregningsBehandling = TestdataFactory.lagBehandling().apply {
            id = 2
            type = Behandlingstyper.ÅRSAVREGNING
        }

        val behandlingsresultat = Behandlingsresultat().apply {
            id = 2
            type = Behandlingsresultattyper.IKKE_FASTSATT
        }

        every { persondataService.hentAktørIdForIdent(any()) } returns "789"
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, "789") } returns listOf(fagsak)
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
                Behandlingsaarsaktyper.ANNET,
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
    fun `ikke opprette ny behandling ved skatteoppgjør ved ikke overlappende medlemskapsperiode`() {
        val prosessinstans = Prosessinstans().apply {
            setData(ProsessDataKey.GJELDER_PERIODE, "2023")
            setData(ProsessDataKey.AKTØR_ID, "456789123")
        }

        val fagsak = TestdataFactory.lagFagsak()
        val behandling = TestdataFactory.lagBehandling()

        every { persondataService.hentAktørIdForIdent(any()) } returns "789"
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, "789") } returns listOf(fagsak)
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
    fun `ikke opprette ny behandling ved skatteoppgjør uten fakturert trygdeavgift`() {

    }

    @Test
    fun `ikke opprette ny behandling ved skatteoppgjør uten overlappende medlemskapsperiode`() {

    }

    @Test
    fun `opprette ny behandling ved skatteoppgjør for person med endret fnr fra dnr`() {

    }

    @Test
    fun `ignorere melding ved skatteoppgjør for fnr uten sak i Melosys`() {

    }

    @Test
    fun `ikke opprette ny behandling ved skatteoppgjør med tidligere årsavregningsbehandling som ikke er avsluttet`() {

    }

    @Test
    fun `opprette ny behandling ved skatteoppgjør uten tidligere årsavregningsbehandling eller avsluttet årsavregningsbehandling`() {

    }
}
