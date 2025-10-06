package no.nav.melosys.saksflyt.steg.arsavregning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsaarsak
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.saksflyt.TestdataFactory.lagPersonopplysning
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.avgift.aarsavregning.GjeldendeBehandlingsresultater
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
    fun `opprett ny behandling ved skatteoppgjør med overlappende medlemskapsperiode og fakturert trygdeavgift`() {
        val prosessinstans = lagProsessInstans {
            medData(ProsessDataKey.ÅRSAK_TYPE, Behandlingsaarsaktyper.MELDING_FRA_SKATT)
        }

        val eksisterendeÅrsavregningsBehandling = lagBehandling {
            id = 1
            type = Behandlingstyper.ÅRSAVREGNING
        }

        setupMock(eksisterendeÅrsavregningsBehandling, Behandlingsaarsaktyper.MELDING_FRA_SKATT)


        opprettÅrsavregningBehandling.utfør(prosessinstans)


        verify {
            årsavregningService.opprettÅrsavregning(Behandlingsresultat().apply {
                behandling = eksisterendeÅrsavregningsBehandling
                id = 2
                type = Behandlingsresultattyper.IKKE_FASTSATT
            }.id, GJELDER_ÅR)
        }

        prosessinstans.hentBehandling.run {
            id shouldBe 2
            behandlingsårsak.shouldNotBeNull().type shouldBe Behandlingsaarsaktyper.MELDING_FRA_SKATT
        }
    }

    @Test
    fun `opprett ny behandling ved ikke skattepliktig`() {
        val prosessinstans = lagProsessInstans {
            medData(ProsessDataKey.ÅRSAK_TYPE, Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE)
        }

        val eksisterendeÅrsavregningsBehandling = lagBehandling {
            id = 1
            type = Behandlingstyper.ÅRSAVREGNING
        }

        setupMock(eksisterendeÅrsavregningsBehandling, Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE)


        opprettÅrsavregningBehandling.utfør(prosessinstans)


        verify {
            årsavregningService.opprettÅrsavregning(Behandlingsresultat().apply {
                behandling = eksisterendeÅrsavregningsBehandling
                id = 2
                type = Behandlingsresultattyper.IKKE_FASTSATT
            }.id, GJELDER_ÅR)
        }


        prosessinstans.hentBehandling.run {
            id shouldBe 2
            behandlingsårsak.shouldNotBeNull().type shouldBe Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE
        }
    }

    @Test
    fun `sjekk at vi ikke får opprette prosessinstans ved feil type`() {
        val prosessinstans = lagProsessInstans {
            medData(ProsessDataKey.ÅRSAK_TYPE, Behandlingsaarsaktyper.ANNET)
        }

        shouldThrow<IllegalStateException> {
            opprettÅrsavregningBehandling.utfør(prosessinstans)
        }.message shouldBe "Ugyldig årsak for opprettelse av årsavregning: ANNET"
    }

    private fun setupMock(eksisterendeÅrsavregningsBehandling: Behandling, behandlingsaarsaktyper: Behandlingsaarsaktyper) {
        val fagsak = lagFagsak()
        every { persondataService.hentAktørIdForIdent(any()) } returns AKTØR_ID
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak
        every {
            årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(
                fagsak.saksnummer,
                GJELDER_ÅR
            )
        } returns GjeldendeBehandlingsresultater(
            sisteBehandlingsresultatMedAvgift = Behandlingsresultat().apply {
                behandling = eksisterendeÅrsavregningsBehandling
                id = 2
                type = Behandlingsresultattyper.IKKE_FASTSATT
            },
        )

        every {
            behandlingService.nyBehandling(
                fagsak,
                Behandlingsstatus.OPPRETTET,
                Behandlingstyper.ÅRSAVREGNING,
                Behandlingstema.UTSENDT_ARBEIDSTAKER,
                null,
                null,
                any(),
                behandlingsaarsaktyper,
                null
            )
        } returns Behandling.forTest {
            id = 2
            fagsak { }
            type = Behandlingstyper.ÅRSAVREGNING
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            behandlingsårsak = Behandlingsaarsak().apply {
                type = behandlingsaarsaktyper
            }
            saksopplysninger = mutableSetOf(lagPersonopplysning())
        }

        every { behandslingsresultatService.hentBehandlingsresultat(eksisterendeÅrsavregningsBehandling.id) } returns Behandlingsresultat().apply {
            this.behandling = eksisterendeÅrsavregningsBehandling
            this.id = 2
            this.type = Behandlingsresultattyper.IKKE_FASTSATT
        }
        every { årsavregningService.opprettÅrsavregning(any(), any()) } returns ÅrsavregningModel(
            årsavregningID = 112,
            år = 2023,
            tidligereAvgift = emptyList(),
            endeligAvgift = emptyList(),
            harSkjoennsfastsattInntektsgrunnlag = false,
        )

        every { mottatteOpplysningerService.opprettMottatteopplysningerForAarsavregning(any()) } just runs
    }

    private fun lagBehandling(block: Behandling.Builder.() -> Unit = {}): Behandling = Behandling.forTest {
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        saksopplysninger = mutableSetOf(lagPersonopplysning())
        block()
    }

    private fun lagFagsak(block: FagsakTestFactory.Builder.() -> Unit = {}): Fagsak = Fagsak.forTest {
        type = Sakstyper.EU_EOS
        tema = Sakstemaer.MEDLEMSKAP_LOVVALG
        status = Saksstatuser.OPPRETTET
        medBruker()
        block()
    }

    private fun lagProsessInstans(block: Prosessinstans.Builder.() -> Unit = {}): Prosessinstans = Prosessinstans.forTest {
        medData(ProsessDataKey.GJELDER_ÅR, GJELDER_ÅR)
        medData(ProsessDataKey.AKTØR_ID, AKTØR_ID)
        medData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER)
        block()
    }

    companion object {
        const val AKTØR_ID = "456789123"
        const val GJELDER_ÅR = 2023
        const val SAKSNUMMER = "MEL-test"
    }
}
