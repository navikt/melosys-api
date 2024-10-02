package no.nav.melosys.service.avgift.aarsavregning

import io.getunleash.FakeUnleash
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.aarsavregning.Skattehendelse
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import no.nav.melosys.service.sak.FagsakService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith


@ExtendWith(MockKExtension::class)
class SkattehendelserConsumerTest {

    @MockK
    private lateinit var prosessinstansService: ProsessinstansService

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var trygdeavgiftService: TrygdeavgiftService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandslingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var persondataService: PersondataService

    @MockK
    private lateinit var årsavregningService: ÅrsavregningService

    private lateinit var skattehendelserConsumer: SkattehendelserConsumer


    @BeforeEach
    fun setUp() {
        skattehendelserConsumer = SkattehendelserConsumer(
            mockk(),
            FakeUnleash().apply { enableAll() },
            fagsakService,
            trygdeavgiftService,
            behandlingService,
            behandslingsresultatService,
            årsavregningService
        )
    }


    @Test
    fun `oppdater behandling ved skatteoppgjør med endring i tidligere skatteoppgjør og ikke avsluttet ennå med overlapp`() {
        val behandling = lagBehandling {
            id = 1
            type = Behandlingstyper.ÅRSAVREGNING
        }
        val fagsak = lagFagsak {
            this.leggTilBehandling(behandling)
        }

        val behandlingsresultat = Behandlingsresultat().apply {
            this.behandling = behandling
            id = 2
            type = Behandlingsresultattyper.IKKE_FASTSATT
            årsavregning = Årsavregning().apply {
                aar = GJELDER_ÅR
            }
        }

        every { trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(SAKSNUMMER) } returns true
        every { persondataService.hentAktørIdForIdent(any()) } returns AKTØR_ID
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak
        every {
            årsavregningService.hentSisteBehandlingsresultatMedInnvilgetMedlemskapsperiodeOgAvgiftsgrunnlag(
                fagsak.saksnummer,
                any()
            )
        } returns behandlingsresultat

        every { behandslingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat
        every { årsavregningService.opprettÅrsavregning(any(), any()) } returns ÅrsavregningModel(
            2023,
            null,
            emptyList(),
            null,
            emptyList(),
            null,
            null,
            null
        )
        val behandlingSlot = slot<Behandling>()
        every { behandlingService.lagre(capture(behandlingSlot)) } returns Unit

        val consumerRecord = ConsumerRecord(
            "topic", 1, 1, "key", Skattehendelse(
                gjelderPeriode = GJELDER_ÅR.toString(),
                identifikator = AKTØR_ID,
                hendelsetype = "ny"
            )
        )
        skattehendelserConsumer.lesSkattehendelser(consumerRecord)

        verify(exactly = 0) { prosessinstansService.opprettArsavregningsBehandlingProsessflyt(any(), any()) }
        verify { behandlingService.lagre(behandling) }
        verify { årsavregningService wasNot Called }
        behandlingSlot.captured.status shouldBe Behandlingsstatus.VURDER_DOKUMENT
    }

    @Test
    fun `ikke opprette ny behandling ved skatteoppgjør uten overlappende medlemskapsperiode`() {
        val fagsak = lagFagsak()

        every { trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(SAKSNUMMER) } returns true
        every { persondataService.hentAktørIdForIdent(any()) } returns AKTØR_ID
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak
        every {
            årsavregningService.hentSisteBehandlingsresultatMedInnvilgetMedlemskapsperiodeOgAvgiftsgrunnlag(
                fagsak.saksnummer,
                any()
            )
        } returns null

        val consumerRecord = ConsumerRecord(
            "topic", 1, 1, "key", Skattehendelse(
                gjelderPeriode = GJELDER_ÅR.toString(),
                identifikator = AKTØR_ID,
                hendelsetype = "ny"
            )
        )
        skattehendelserConsumer.lesSkattehendelser(consumerRecord)


        verify(exactly = 0) { prosessinstansService.opprettArsavregningsBehandlingProsessflyt(any(), any()) }
        verify(exactly = 0) { behandlingService.nyBehandling(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { årsavregningService.opprettÅrsavregning(any(), any()) }
    }


    private fun lagFagsak(block: Fagsak.() -> Unit = {}): Fagsak = Fagsak(
        SAKSNUMMER,
        123L,
        Sakstyper.EU_EOS,
        Sakstemaer.MEDLEMSKAP_LOVVALG,
        Saksstatuser.OPPRETTET,
        mutableSetOf(lagBruker()),
        mutableListOf()
    ).apply {
        block()
    }

    fun lagBruker(): Aktoer {
        val aktoer = Aktoer()
        aktoer.rolle = Aktoersroller.BRUKER
        aktoer.aktørId = "aktørID"
        return aktoer
    }

    private fun lagBehandling(block: Behandling.() -> Unit = {}): Behandling = lagBehandling().apply {
        block()
    }

    fun lagBehandling(): Behandling {
        val behandling = Behandling()
        behandling.id = 1L
        behandling.type = Behandlingstyper.FØRSTEGANG
        behandling.tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        behandling.fagsak = lagFagsak()
        behandling.saksopplysninger = setOf(lagPersonopplysning())
        return behandling
    }

    fun lagPersonopplysning(): Saksopplysning {
        val saksopplysning = Saksopplysning()
        saksopplysning.type = SaksopplysningType.PERSOPL
        val personDokument = PersonDokument()
        personDokument.fnr = "99887766554"
        saksopplysning.dokument = personDokument
        return saksopplysning
    }

    fun lagFagsak(saksnummer: String?): Fagsak {
        return Fagsak(
            saksnummer!!,
            123L,
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Saksstatuser.OPPRETTET,
            mutableSetOf(lagBruker()),
            mutableListOf()
        )
    }

    companion object {
        const val AKTØR_ID = "456789123"
        const val GJELDER_ÅR = 2023
        const val SAKSNUMMER = "MEL-test"
    }
}
