package no.nav.melosys.service.utpeking

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.repository.UtpekingsperiodeRepository
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.vedtak.VedtaksfattingFasade.FRIST_KLAGE_UKER
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.event.ApplicationEventMulticaster
import java.time.LocalDate

class UtpekingServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val behandlingsresultatService = mockk<BehandlingsresultatService>()
    private val eessiService = mockk<EessiService>()
    private val landvelgerService = mockk<LandvelgerService>()
    private val lovvalgsperiodeService = mockk<LovvalgsperiodeService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val prosessinstansService = mockk<ProsessinstansService>()
    private val utpekingsperiodeRepository = mockk<UtpekingsperiodeRepository>()
    private val ferdigbehandlingKontrollFacade = mockk<FerdigbehandlingKontrollFacade>()
    private val melosysEventMulticaster = mockk<ApplicationEventMulticaster>()

    private val lovvalgsperiodeCaptor = mutableListOf<Collection<Lovvalgsperiode>>()
    private val landkoderCaptor = mutableListOf<Collection<Land_iso2>>()

    private lateinit var utpekingService: UtpekingService
    private lateinit var behandling: Behandling
    private val behandlingsresultat = Behandlingsresultat()
    private lateinit var fagsak: Fagsak

    @BeforeEach
    fun setup() {
        clearAllMocks()
        lovvalgsperiodeCaptor.clear()
        landkoderCaptor.clear()

        utpekingService = UtpekingService(
            behandlingService, behandlingsresultatService, eessiService, landvelgerService,
            lovvalgsperiodeService, oppgaveService, prosessinstansService, utpekingsperiodeRepository,
            ferdigbehandlingKontrollFacade, melosysEventMulticaster
        )

        behandling = Behandling.forTest {
            id = BEHANDLING_ID
            status = Behandlingsstatus.UNDER_BEHANDLING
            fagsak { }
        }
        fagsak = behandling.fagsak

        behandlingsresultat.apply {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND
        }

        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
    }

    @Test
    fun `utpekLovvalgsland harUtpekingsperiode lovvalgsperiodeOgProsessinstansOpprettes`() {
        behandling.tema = Behandlingstema.ARBEID_FLERE_LAND
        val utpekingsperiode = Utpekingsperiode(
            LocalDate.MIN, LocalDate.MAX, Land_iso2.SE,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, null
        ).apply {
            id = 1111L
        }
        behandlingsresultat.utpekingsperioder.add(utpekingsperiode)

        val mottakerInstitusjoner = setOf("SE:123")
        every {
            eessiService.validerOgAvklarMottakerInstitusjonerForBuc(
                mottakerInstitusjoner,
                setOf(Land_iso2.SE),
                BucType.LA_BUC_02
            )
        } returns mottakerInstitusjoner

        every {
            lovvalgsperiodeService.lagreLovvalgsperioder(
                BEHANDLING_ID,
                capture(lovvalgsperiodeCaptor)
            )
        } returns listOf(Lovvalgsperiode())

        every { landvelgerService.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID) } returns setOf(Land_iso2.SE)
        every { ferdigbehandlingKontrollFacade.kontroller(BEHANDLING_ID, any(), null) } returns emptyList()
        every { behandlingsresultatService.lagre(behandlingsresultat) } returns behandlingsresultat
        every { melosysEventMulticaster.multicastEvent(any()) } just Runs
        every {
            prosessinstansService.opprettProsessinstansUtpekAnnetLand(
                behandling,
                Land_iso2.SE,
                mottakerInstitusjoner,
                null,
                null
            )
        } just Runs
        every { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) } just Runs

        utpekingService.utpekLovvalgsland(fagsak, mottakerInstitusjoner, null, null)

        verify { lovvalgsperiodeService.lagreLovvalgsperioder(BEHANDLING_ID, any()) }
        verify {
            prosessinstansService.opprettProsessinstansUtpekAnnetLand(
                behandling,
                Land_iso2.SE,
                mottakerInstitusjoner,
                null,
                null
            )
        }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify { ferdigbehandlingKontrollFacade.kontroller(BEHANDLING_ID, behandlingsresultat.hentType(), null) }

        behandlingsresultat.run {
            type shouldBe Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND
            begrunnelseFritekst shouldBe null
            fastsattAvLand shouldBe Land_iso2.NO
            nyVurderingBakgrunn shouldBe null
            vedtakMetadata.shouldNotBeNull().run {
                vedtakstype shouldBe Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtakKlagefrist shouldBe LocalDate.now().plusWeeks(FRIST_KLAGE_UKER.toLong())
            }
        }

        val lagretLovvalgsperioder = lovvalgsperiodeCaptor.first()
        lagretLovvalgsperioder.shouldNotBeEmpty()
        lagretLovvalgsperioder shouldHaveSize 1

        val lovvalgsperiode = lagretLovvalgsperioder.first()
        lovvalgsperiode.run {
            bestemmelse shouldBe utpekingsperiode.bestemmelse
            fom shouldBe utpekingsperiode.fom
            tom shouldBe utpekingsperiode.tom
            innvilgelsesresultat shouldBe InnvilgelsesResultat.INNVILGET
            dekning shouldBe Trygdedekninger.UTEN_DEKNING
            tilleggsbestemmelse shouldBe utpekingsperiode.tilleggsbestemmelse
        }
    }

    @Test
    fun `utpekLovvalgsland feilBehandlingstema kasterException`() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        val utpekingsperiode = Utpekingsperiode(
            LocalDate.now(), LocalDate.now(), Land_iso2.SE,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, null
        )
        behandlingsresultat.utpekingsperioder.add(utpekingsperiode)
        val mottakerInstitusjoner = setOf("SE:123")

        shouldThrow<FunksjonellException> {
            utpekingService.utpekLovvalgsland(fagsak, mottakerInstitusjoner, null, null)
        }
    }

    @Test
    fun `utpekLovvalgsland lovvalgslandValideres`() {
        behandling.tema = Behandlingstema.ARBEID_FLERE_LAND
        val utpekingsperiode = Utpekingsperiode(
            LocalDate.MIN, LocalDate.MAX, Land_iso2.SE,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, null
        ).apply {
            id = 111L
        }
        behandlingsresultat.utpekingsperioder.add(utpekingsperiode)

        val mottakerInstitusjoner = setOf("SE:123", "DK:321", "FI:111")
        every {
            eessiService.validerOgAvklarMottakerInstitusjonerForBuc(
                mottakerInstitusjoner,
                capture(landkoderCaptor),
                BucType.LA_BUC_02
            )
        } returns mottakerInstitusjoner

        every {
            lovvalgsperiodeService.lagreLovvalgsperioder(BEHANDLING_ID, any())
        } returns listOf(Lovvalgsperiode())

        every { landvelgerService.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID) } returns listOf(
            Land_iso2.SE,
            Land_iso2.DK,
            Land_iso2.FI
        )
        every { ferdigbehandlingKontrollFacade.kontroller(BEHANDLING_ID, any(), null) } returns emptyList()
        every { behandlingsresultatService.lagre(behandlingsresultat) } returns behandlingsresultat
        every { melosysEventMulticaster.multicastEvent(any()) } just Runs
        every {
            prosessinstansService.opprettProsessinstansUtpekAnnetLand(any(), any(), any(), any(), any())
        } just Runs
        every { oppgaveService.ferdigstillOppgaveMedBehandlingID(any()) } just Runs

        utpekingService.utpekLovvalgsland(fagsak, mottakerInstitusjoner, null, null)

        verify {
            eessiService.validerOgAvklarMottakerInstitusjonerForBuc(
                mottakerInstitusjoner,
                any(),
                BucType.LA_BUC_02
            )
        }
        landkoderCaptor.first() shouldContainExactlyInAnyOrder listOf(Land_iso2.SE, Land_iso2.DK, Land_iso2.FI)

        behandlingsresultat.run {
            type shouldBe Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND
            begrunnelseFritekst shouldBe null
            fastsattAvLand shouldBe Land_iso2.NO
            nyVurderingBakgrunn shouldBe null
            vedtakMetadata.shouldNotBeNull().run {
                vedtakstype shouldBe Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtakKlagefrist shouldBe LocalDate.now().plusWeeks(FRIST_KLAGE_UKER.toLong())
            }
        }
    }

    @Test
    fun `avvisUtpeking utpekingAvAnnetLand oppdaterUtfallRegistreringUnntak`() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND

        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.SEDOPPL
            dokument = SedDokument().apply {
                rinaSaksnummer = "123"
                sedType = SedType.A003
                lovvalgslandKode = Landkoder.NO
            }
        }
        behandling.saksopplysninger = mutableSetOf(saksopplysning)

        every {
            behandlingsresultatService.settUtfallRegistreringUnntakOgType(
                BEHANDLING_ID,
                Utfallregistreringunntak.IKKE_GODKJENT
            )
        } just Runs
        every { prosessinstansService.opprettProsessinstansAvvisUtpeking(behandling, any()) } just Runs
        every { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) } just Runs

        utpekingService.avvisUtpeking(BEHANDLING_ID, lagUtpekingAvvis())

        verify {
            behandlingsresultatService.settUtfallRegistreringUnntakOgType(
                BEHANDLING_ID,
                Utfallregistreringunntak.IKKE_GODKJENT
            )
        }
        verify { prosessinstansService.opprettProsessinstansAvvisUtpeking(behandling, any()) }
    }

    @Test
    fun `avvisUtpeking utpekingAvNorge oppdaterUtfallUtpeking`() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE

        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.SEDOPPL
            dokument = SedDokument().apply {
                rinaSaksnummer = "123"
                sedType = SedType.A003
                lovvalgslandKode = Landkoder.NO
            }
        }
        behandling.saksopplysninger = mutableSetOf(saksopplysning)

        every {
            behandlingsresultatService.oppdaterBehandlingsresultattype(
                BEHANDLING_ID,
                Behandlingsresultattyper.UTPEKING_NORGE_AVVIST
            )
        } just Runs
        every {
            behandlingsresultatService.oppdaterUtfallUtpeking(
                BEHANDLING_ID,
                Utfallregistreringunntak.IKKE_GODKJENT
            )
        } just Runs
        every { prosessinstansService.opprettProsessinstansAvvisUtpeking(behandling, any()) } just Runs
        every { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) } just Runs

        utpekingService.avvisUtpeking(BEHANDLING_ID, lagUtpekingAvvis())

        verify { behandlingsresultatService.oppdaterUtfallUtpeking(BEHANDLING_ID, Utfallregistreringunntak.IKKE_GODKJENT) }
        verify { prosessinstansService.opprettProsessinstansAvvisUtpeking(behandling, any()) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
    }

    @Test
    fun `avvisUtpeking utpekingAvNorge A003 med behandlingsresultatType UTPEKT_NORGE_AVVIST`() {
        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.SEDOPPL
            dokument = SedDokument().apply {
                rinaSaksnummer = "123"
                sedType = SedType.A003
                lovvalgslandKode = Landkoder.NO
            }
        }
        behandling.saksopplysninger = mutableSetOf(saksopplysning)
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE

        every {
            behandlingsresultatService.oppdaterBehandlingsresultattype(
                BEHANDLING_ID,
                Behandlingsresultattyper.UTPEKING_NORGE_AVVIST
            )
        } just Runs
        every {
            behandlingsresultatService.oppdaterUtfallUtpeking(
                BEHANDLING_ID,
                Utfallregistreringunntak.IKKE_GODKJENT
            )
        } just Runs
        every { prosessinstansService.opprettProsessinstansAvvisUtpeking(behandling, any()) } just Runs
        every { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) } just Runs

        utpekingService.avvisUtpeking(BEHANDLING_ID, lagUtpekingAvvis())

        verify {
            behandlingsresultatService.oppdaterBehandlingsresultattype(
                BEHANDLING_ID,
                Behandlingsresultattyper.UTPEKING_NORGE_AVVIST
            )
        }
    }

    @Test
    fun `avvisUtpeking utsendtArbeidtaker ikkeStøttetKasterException`() {
        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.SEDOPPL
            dokument = SedDokument().apply {
                rinaSaksnummer = "123"
                sedType = SedType.A003
                lovvalgslandKode = Landkoder.NO
            }
        }
        behandling.saksopplysninger = mutableSetOf(saksopplysning)
        behandling.tema = Behandlingstema.UTSENDT_ARBEIDSTAKER

        val exception = shouldThrow<FunksjonellException> {
            utpekingService.avvisUtpeking(BEHANDLING_ID, lagUtpekingAvvis())
        }
        exception.message shouldContain "Kan ikke avvise utpeking for en behandling med tema"
    }

    @Test
    fun `avvisUtpeking utenBegrunnelse begrunnelsePåkrevdKasterException`() {
        val exception = shouldThrow<FunksjonellException> {
            utpekingService.avvisUtpeking(BEHANDLING_ID, UtpekingAvvis())
        }
        exception.message shouldContain "Du må oppgi en begrunnelse for å kunne avslå en utpeking"
    }

    @Test
    fun `avvisUtpeking utenEtterspørInformasjon etterspørInfoPåkrevdKasterException`() {
        val utpekingAvvis = UtpekingAvvis().apply {
            begrunnelse = "fordi og derfor"
        }

        val exception = shouldThrow<FunksjonellException> {
            utpekingService.avvisUtpeking(BEHANDLING_ID, utpekingAvvis)
        }
        exception.message shouldContain "Du må oppgi om forespørsel om mer informasjon vil bli sendt"
    }

    @Test
    fun `avvisUtpeking behandlingInaktiv kasterException`() {
        behandling.status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING

        val exception = shouldThrow<FunksjonellException> {
            utpekingService.avvisUtpeking(BEHANDLING_ID, lagUtpekingAvvis())
        }
        exception.message shouldContain "er ikke aktiv"
    }

    @Test
    fun `avvisUtpeking bucKanIkkeOppretteSed kasterException`() {
        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.SEDOPPL
            dokument = SedDokument().apply {
                rinaSaksnummer = "123"
                sedType = SedType.A004
            }
        }
        behandling.saksopplysninger = mutableSetOf(saksopplysning)

        every { eessiService.kanOppretteSedTyperPåBuc("123", SedType.A004) } returns false

        val exception = shouldThrow<FunksjonellException> {
            utpekingService.avvisUtpeking(BEHANDLING_ID, lagUtpekingAvvis())
        }
        exception.message shouldContain "Kan ikke opprette SedType A004 på rinaSaknummer: 123"
    }

    @Test
    fun `oppdaterSendtUtland ikkeSattFraFør oppdateres`() {
        val utpekingsperiode = Utpekingsperiode().apply {
            id = 1L
        }

        every { utpekingsperiodeRepository.save(utpekingsperiode) } returns utpekingsperiode

        utpekingService.oppdaterSendtUtland(utpekingsperiode)

        verify { utpekingsperiodeRepository.save(utpekingsperiode) }
        utpekingsperiode.sendtUtland shouldNotBe null
    }

    @Test
    fun `oppdaterSendtUtland ikkePersistert kasterException`() {
        val exception = shouldThrow<TekniskException> {
            utpekingService.oppdaterSendtUtland(Utpekingsperiode())
        }
        exception.message shouldContain "Forsøk på å oppdatere en ikke-persistert utpekingsperiode"
    }

    @Test
    fun `oppdaterSendtUtland alleredeSendtUtland kasterException`() {
        val utpekingsperiode = Utpekingsperiode().apply {
            id = 1L
            sendtUtland = LocalDate.now()
        }

        val exception = shouldThrow<FunksjonellException> {
            utpekingService.oppdaterSendtUtland(utpekingsperiode)
        }
        exception.message shouldContain "er allerede markert som sendtUtland"
    }

    @Test
    fun `lagreUtpekingsperioder gyldigePerioder lagres`() {
        val nyeUtpekingsperioder = listOf(
            Utpekingsperiode(
                LocalDate.now(), LocalDate.now().plusDays(30), Land_iso2.SE,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, null
            )
        )

        every { utpekingsperiodeRepository.findByBehandlingsresultat_Id(BEHANDLING_ID) } returns emptyList()
        every { utpekingsperiodeRepository.deleteByBehandlingsresultat(any()) } returns emptyList()
        every { utpekingsperiodeRepository.flush() } returns Unit
        every { utpekingsperiodeRepository.saveAll(nyeUtpekingsperioder) } returns nyeUtpekingsperioder

        val resultat = utpekingService.lagreUtpekingsperioder(BEHANDLING_ID, nyeUtpekingsperioder)

        verify { utpekingsperiodeRepository.deleteByBehandlingsresultat(behandlingsresultat) }
        verify { utpekingsperiodeRepository.saveAll(nyeUtpekingsperioder) }
        resultat shouldBe nyeUtpekingsperioder
    }

    @Test
    fun `lagreUtpekingsperioder eksisterendeUtpekingsperiodeUtenBestemmelse kasterException`() {
        val eksisterendeUtpekingsperiode = Utpekingsperiode().apply {
            bestemmelse = null
            lovvalgsland = Land_iso2.SE
        }

        every { utpekingsperiodeRepository.findByBehandlingsresultat_Id(BEHANDLING_ID) } returns listOf(eksisterendeUtpekingsperiode)

        val exception = shouldThrow<FunksjonellException> {
            utpekingService.lagreUtpekingsperioder(BEHANDLING_ID, emptyList())
        }
        exception.message shouldContain "Kan ikke oppdatere utpekingsperiode uten bestemmelse for behandlingID: "
    }

    @Test
    fun `lagreUtpekingsperioder eksisterendeUtpekingsperiodeUtenLovvalgsland kasterException`() {
        val eksisterendeUtpekingsperiode = Utpekingsperiode().apply {
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1
            lovvalgsland = null
        }

        every { utpekingsperiodeRepository.findByBehandlingsresultat_Id(BEHANDLING_ID) } returns listOf(eksisterendeUtpekingsperiode)

        val exception = shouldThrow<FunksjonellException> {
            utpekingService.lagreUtpekingsperioder(BEHANDLING_ID, emptyList())
        }
        exception.message shouldContain "Kan ikke oppdatere utpekingsperiode uten lovvalgsland for behandlingID: "
    }

    @Test
    fun `lagreUtpekingsperioder eksisterendeUtpekingsperiodeSendtUtland kasterException`() {
        val eksisterendeUtpekingsperiode = Utpekingsperiode().apply {
            sendtUtland = LocalDate.now()
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1
            lovvalgsland = Land_iso2.SE
        }

        every { utpekingsperiodeRepository.findByBehandlingsresultat_Id(BEHANDLING_ID) } returns listOf(eksisterendeUtpekingsperiode)

        val exception = shouldThrow<FunksjonellException> {
            utpekingService.lagreUtpekingsperioder(BEHANDLING_ID, emptyList())
        }
        exception.message shouldContain "Kan ikke oppdatere utpekingsperiode etter at A003 er sendt for behandlingID: "
    }

    private fun lagUtpekingAvvis() = UtpekingAvvis().apply {
        begrunnelse = "taddaaa"
        etterspørInformasjon = true
    }

    companion object {
        private const val BEHANDLING_ID = 431L
    }
}
