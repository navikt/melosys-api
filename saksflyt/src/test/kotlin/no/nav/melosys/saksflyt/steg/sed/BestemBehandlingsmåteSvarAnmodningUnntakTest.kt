package no.nav.melosys.saksflyt.steg.sed

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.eessi.SvarAnmodningUnntak
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.exception.ValideringException
import no.nav.melosys.exception.validering.KontrollfeilDto
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import no.nav.melosys.service.vedtak.FattVedtakRequest
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class BestemBehandlingsmåteSvarAnmodningUnntakTest {
    @MockK
    private lateinit var anmodningsperiodeService: AnmodningsperiodeService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var vedtakService: VedtaksfattingFasade

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    @MockK
    private lateinit var ferdigbehandlingKontrollFacade: FerdigbehandlingKontrollFacade

    private lateinit var bestemBehandlingsmåteSvarAnmodningUnntak: BestemBehandlingsmåteSvarAnmodningUnntak

    private val lovvalgsperiodeSlot = slot<Collection<Lovvalgsperiode>>()

    private val anmodningsperiode = Anmodningsperiode()

    @BeforeEach
    fun setUp() {
        val anmodningsperiodeSvar = AnmodningsperiodeSvar().apply {
            anmodningsperiode = this@BestemBehandlingsmåteSvarAnmodningUnntakTest.anmodningsperiode
        }
        anmodningsperiode.anmodningsperiodeSvar = anmodningsperiodeSvar
        bestemBehandlingsmåteSvarAnmodningUnntak = BestemBehandlingsmåteSvarAnmodningUnntak(
            anmodningsperiodeService,
            behandlingService,
            behandlingsresultatService,
            vedtakService,
            lovvalgsperiodeService,
            ferdigbehandlingKontrollFacade
        )
        every { anmodningsperiodeService.hentAnmodningsperioder(any()) } returns Collections.singleton(anmodningsperiode)
        every { ferdigbehandlingKontrollFacade.kontroller(any(), any(), any(), any()) } returns emptyList()
    }

    @Test
    fun `utfør skal sette status til svar anmodning mottatt når anmodningsperiode ikke er innvilget`() {
        anmodningsperiode.anmodningsperiodeSvar.anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding().apply {
                ytterligereInformasjon = "hei"
                svarAnmodningUnntak = SvarAnmodningUnntak().apply {
                    beslutning = SvarAnmodningUnntak.Beslutning.AVSLAG
                }
            })
            behandling {
                id = 123L
                status = Behandlingsstatus.ANMODNING_UNNTAK_SENDT
            }
        }

        every { behandlingService.endreStatus(any<Long>(), any()) } just Runs
        every { lovvalgsperiodeService.lagreLovvalgsperioder(any(), capture(lovvalgsperiodeSlot)) } returns emptyList()


        bestemBehandlingsmåteSvarAnmodningUnntak.utfør(prosessinstans)


        verify { behandlingService.endreStatus(any<Long>(), Behandlingsstatus.SVAR_ANMODNING_MOTTATT) }
        verify { lovvalgsperiodeService.lagreLovvalgsperioder(any(), any()) }

        val lagredeLovvalgsperioder = lovvalgsperiodeSlot.captured
        lagredeLovvalgsperioder shouldHaveSize 1

        val lovvalgsperiode = lagredeLovvalgsperioder.iterator().next()
        lovvalgsperiode.innvilgelsesresultat shouldBe InnvilgelsesResultat.AVSLAATT
    }

    @Test
    fun `utfør skal fatte vedtak når anmodningsperiode er innvilget og status er aou sendt`() {
        anmodningsperiode.anmodningsperiodeSvar.anmodningsperiodeSvarType = Anmodningsperiodesvartyper.INNVILGELSE
        val melosysEessiMelding = MelosysEessiMelding().apply {
            svarAnmodningUnntak = SvarAnmodningUnntak().apply {
                beslutning = SvarAnmodningUnntak.Beslutning.INNVILGELSE
            }
        }

        val behandling = lagBehandling(Behandlingsstatus.ANMODNING_UNNTAK_SENDT)
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
            medBehandling(behandling)
        }

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandling.id) } returns behandling
        every { vedtakService.fattVedtak(any(), any()) } just Runs
        every { behandlingsresultatService.oppdaterBehandlingsMaate(any(), any()) } just Runs
        every { lovvalgsperiodeService.lagreLovvalgsperioder(any(), capture(lovvalgsperiodeSlot)) } returns emptyList()


        bestemBehandlingsmåteSvarAnmodningUnntak.utfør(prosessinstans)


        verify {
            vedtakService.fattVedtak(
                behandling.id,
                match<FattVedtakRequest> { req ->
                    req.behandlingsresultatTypeKode == Behandlingsresultattyper.FASTSATT_LOVVALGSLAND &&
                        req.vedtakstype == Vedtakstyper.FØRSTEGANGSVEDTAK
                }
            )
        }

        verify { behandlingsresultatService.oppdaterBehandlingsMaate(any(), any()) }
        verify { lovvalgsperiodeService.lagreLovvalgsperioder(any(), any()) }

        lovvalgsperiodeSlot.captured
            .shouldHaveSize(1)
            .single()
            .innvilgelsesresultat shouldBe InnvilgelsesResultat.INNVILGET
    }

    @Test
    fun `utfør skal sette status til svar anmodning mottatt når anmodningsperiode er innvilget men status ikke er aou sendt`() {
        anmodningsperiode.anmodningsperiodeSvar.anmodningsperiodeSvarType = Anmodningsperiodesvartyper.INNVILGELSE
        val melosysEessiMelding = MelosysEessiMelding().apply {
            svarAnmodningUnntak = SvarAnmodningUnntak().apply {
                beslutning = SvarAnmodningUnntak.Beslutning.INNVILGELSE
            }
        }

        val behandling = lagBehandling(Behandlingsstatus.VURDER_DOKUMENT)
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
            this.behandling = behandling
        }

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandling.id) } returns behandling
        every { behandlingService.endreStatus(any<Long>(), any()) } just Runs
        every { lovvalgsperiodeService.lagreLovvalgsperioder(any(), any()) } returns emptyList()

        bestemBehandlingsmåteSvarAnmodningUnntak.utfør(prosessinstans)

        verify(exactly = 0) { vedtakService.fattVedtak(any(), any<FattVedtakRequest>()) }
        verify { behandlingService.endreStatus(any<Long>(), Behandlingsstatus.SVAR_ANMODNING_MOTTATT) }
    }

    @Test
    fun `utfør skal sette status til svar anmodning mottatt når anmodningsperiode er innvilget med ytterligere info`() {
        anmodningsperiode.anmodningsperiodeSvar.anmodningsperiodeSvarType = Anmodningsperiodesvartyper.INNVILGELSE
        val melosysEessiMelding = MelosysEessiMelding().apply {
            ytterligereInformasjon = "hei"
            svarAnmodningUnntak = SvarAnmodningUnntak().apply {
                beslutning = SvarAnmodningUnntak.Beslutning.INNVILGELSE
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
            behandling {
                id = 123L
                status = Behandlingsstatus.ANMODNING_UNNTAK_SENDT
            }
        }

        every { behandlingService.endreStatus(any<Long>(), any()) } just Runs
        every { lovvalgsperiodeService.lagreLovvalgsperioder(any(), capture(lovvalgsperiodeSlot)) } returns emptyList()

        bestemBehandlingsmåteSvarAnmodningUnntak.utfør(prosessinstans)

        verify { behandlingService.endreStatus(any<Long>(), Behandlingsstatus.SVAR_ANMODNING_MOTTATT) }
        verify { lovvalgsperiodeService.lagreLovvalgsperioder(any(), any()) }

        lovvalgsperiodeSlot.captured
            .shouldHaveSize(1)
            .single()
            .innvilgelsesresultat shouldBe InnvilgelsesResultat.INNVILGET
    }

    @Test
    fun `utfør skal sette status til svar anmodning mottatt ved valideringsfeil i fatt vedtak`() {
        val kontrollfeilDto = KontrollfeilDto().apply {
            kode = Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER.kode
        }

        every { vedtakService.fattVedtak(any(), any<FattVedtakRequest>()) } throws ValideringException(
            "Kunne ikke fatte vedtak",
            setOf(kontrollfeilDto)
        )

        anmodningsperiode.anmodningsperiodeSvar.anmodningsperiodeSvarType = Anmodningsperiodesvartyper.INNVILGELSE
        val melosysEessiMelding = MelosysEessiMelding().apply {
            svarAnmodningUnntak = SvarAnmodningUnntak().apply {
                beslutning = SvarAnmodningUnntak.Beslutning.INNVILGELSE
            }
        }

        val behandling = lagBehandling()
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
            this.behandling = behandling
        }

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandling.id) } returns behandling
        every { behandlingService.endreStatus(any<Long>(), any()) } just Runs
        every { lovvalgsperiodeService.lagreLovvalgsperioder(any(), capture(lovvalgsperiodeSlot)) } returns emptyList()


        bestemBehandlingsmåteSvarAnmodningUnntak.utfør(prosessinstans)


        verify { vedtakService.fattVedtak(behandling.id, any<FattVedtakRequest>()) }

        verify(exactly = 0) { behandlingsresultatService.oppdaterBehandlingsMaate(123L, Behandlingsmaate.DELVIS_AUTOMATISERT) }
        verify { behandlingService.endreStatus(any<Long>(), Behandlingsstatus.SVAR_ANMODNING_MOTTATT) }
        verify { lovvalgsperiodeService.lagreLovvalgsperioder(any(), any()) }

        lovvalgsperiodeSlot.captured
            .shouldHaveSize(1)
            .single()
            .innvilgelsesresultat shouldBe InnvilgelsesResultat.INNVILGET
    }

    private fun lagBehandling() = Behandling.forTest {
        id = 123L
        status = Behandlingsstatus.ANMODNING_UNNTAK_SENDT
    }

    private fun lagBehandling(behandlingsstatus: Behandlingsstatus) = Behandling.forTest {
        id = 123L
        status = behandlingsstatus
    }
}
