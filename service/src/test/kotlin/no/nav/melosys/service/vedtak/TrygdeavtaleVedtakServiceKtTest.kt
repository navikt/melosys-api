package no.nav.melosys.service.vedtak

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.brev.StandardvedleggType
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Mottakerroller.*
import no.nav.melosys.domain.kodeverk.Saksstatuser.MEDLEMSKAP_AVKLART
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.Vedtakstyper.*
import no.nav.melosys.domain.kodeverk.begrunnelser.Nyvurderingbakgrunner
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.IVERKSETTER_VEDTAK
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.TRYGDEAVTALE_GB
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.exception.ValideringException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.DokgenService
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.KopiMottakerDto
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TrygdeavtaleVedtakServiceKtTest {

    companion object {
        private const val BEHANDLING_ID = 123L
    }

    @RelaxedMockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    private lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    private lateinit var prosessinstansService: ProsessinstansService

    @RelaxedMockK
    private lateinit var oppgaveService: OppgaveService

    @RelaxedMockK
    private lateinit var dokgenService: DokgenService

    @RelaxedMockK
    private lateinit var ferdigbehandlingKontrollFacade: FerdigbehandlingKontrollFacade

    @RelaxedMockK
    private lateinit var saksbehandlingRegler: SaksbehandlingRegler

    private val unleash = FakeUnleash()

    private lateinit var trygdeavtaleVedtakService: TrygdeavtaleVedtakService

    @BeforeEach
    fun setup() {
        trygdeavtaleVedtakService = TrygdeavtaleVedtakService(
            behandlingsresultatService,
            behandlingService,
            prosessinstansService,
            oppgaveService,
            dokgenService,
            ferdigbehandlingKontrollFacade,
            saksbehandlingRegler,
            unleash
        )

        unleash.enable(ToggleName.STANDARDVEDLEGG_EGET_VEDLEGG_AVTALELAND)
        SpringSubjectHandler.set(TestSubjectHandler())

        // Mock the validation to return empty collection to avoid validation errors
        every { 
            ferdigbehandlingKontrollFacade.kontrollerVedtakMedRegisteropplysninger(
                any(), any(), any(), any()
            )
        } returns emptyList()
    }

    @Test
    fun `fattVedtak førstegangsvedtak fatterVedtak`() {
        val behandlingsresultat = lagBehandlingsresultat()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val request = lagFattVedtakRequest(FØRSTEGANGSVEDTAK, null)
        trygdeavtaleVedtakService.fattVedtak(lagBehandling(), request)

        val behandlingsresultatSlot = slot<Behandlingsresultat>()
        val behandlingSlot = slot<Behandling>()
        val brevbestillingSlot = slot<BrevbestillingDto>()

        verify { behandlingsresultatService.lagre(capture(behandlingsresultatSlot)) }
        verify { behandlingService.endreStatus(capture(behandlingSlot), IVERKSETTER_VEDTAK) }
        verify { prosessinstansService.opprettProsessinstansIverksettVedtakTrygdeavtale(any<Behandling>()) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify { dokgenService.produserOgDistribuerBrev(any<Long>(), capture(brevbestillingSlot)) }
        verify {
            ferdigbehandlingKontrollFacade.kontrollerVedtakMedRegisteropplysninger(
                any<Behandling>(),
                Sakstyper.TRYGDEAVTALE,
                any<Behandlingsresultattyper>(),
                null
            )
        }

        val lagretBehandlingsresultat = behandlingsresultatSlot.captured
        lagretBehandlingsresultat.type shouldBe FASTSATT_LOVVALGSLAND
        lagretBehandlingsresultat.begrunnelseFritekst shouldBe "Begrunnelse"
        lagretBehandlingsresultat.fastsattAvLand shouldBe Land_iso2.NO

        val lagretBehandling = behandlingSlot.captured
        lagretBehandling.fagsak.status shouldBe MEDLEMSKAP_AVKLART

        val brevbestillingDto = brevbestillingSlot.captured
        brevbestillingDto.produserbardokument shouldBe TRYGDEAVTALE_GB
        brevbestillingDto.bestillersId shouldBe "Z990007"
        brevbestillingDto.mottaker shouldBe BRUKER
        brevbestillingDto.innledningFritekst shouldBe "Innledning"
        brevbestillingDto.begrunnelseFritekst shouldBe "Begrunnelse"
        brevbestillingDto.ektefelleFritekst shouldBe "Ektefelle omfattet"
        brevbestillingDto.barnFritekst shouldBe "Barn omfattet"
        brevbestillingDto.nyVurderingBakgrunn shouldBe null
        brevbestillingDto.standardvedleggType shouldBe StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE

        brevbestillingDto.kopiMottakere shouldHaveSize 2
        brevbestillingDto.kopiMottakere[0].rolle() shouldBe ARBEIDSGIVER
        brevbestillingDto.kopiMottakere[1].rolle() shouldBe UTENLANDSK_TRYGDEMYNDIGHET
    }

    @Test
    fun `fattVedtak førstegangsvedtak fatterVedtak UTEN TOGGLE`() {
        unleash.disable(ToggleName.STANDARDVEDLEGG_EGET_VEDLEGG_AVTALELAND)
        val behandlingsresultat = lagBehandlingsresultat()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val request = lagFattVedtakRequest(FØRSTEGANGSVEDTAK, null)
        trygdeavtaleVedtakService.fattVedtak(lagBehandling(), request)

        val behandlingsresultatSlot = slot<Behandlingsresultat>()
        val behandlingSlot = slot<Behandling>()
        val brevbestillingSlot = slot<BrevbestillingDto>()

        verify { behandlingsresultatService.lagre(capture(behandlingsresultatSlot)) }
        verify { behandlingService.endreStatus(capture(behandlingSlot), IVERKSETTER_VEDTAK) }
        verify { prosessinstansService.opprettProsessinstansIverksettVedtakTrygdeavtale(any<Behandling>()) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify { dokgenService.produserOgDistribuerBrev(any<Long>(), capture(brevbestillingSlot)) }
        verify {
            ferdigbehandlingKontrollFacade.kontrollerVedtakMedRegisteropplysninger(
                any<Behandling>(),
                Sakstyper.TRYGDEAVTALE,
                any<Behandlingsresultattyper>(),
                null
            )
        }

        val lagretBehandlingsresultat = behandlingsresultatSlot.captured
        lagretBehandlingsresultat.type shouldBe FASTSATT_LOVVALGSLAND
        lagretBehandlingsresultat.begrunnelseFritekst shouldBe "Begrunnelse"
        lagretBehandlingsresultat.fastsattAvLand shouldBe Land_iso2.NO

        val lagretBehandling = behandlingSlot.captured
        lagretBehandling.fagsak.status shouldBe MEDLEMSKAP_AVKLART

        val brevbestillingDto = brevbestillingSlot.captured
        brevbestillingDto.produserbardokument shouldBe TRYGDEAVTALE_GB
        brevbestillingDto.bestillersId shouldBe "Z990007"
        brevbestillingDto.mottaker shouldBe BRUKER
        brevbestillingDto.innledningFritekst shouldBe "Innledning"
        brevbestillingDto.begrunnelseFritekst shouldBe "Begrunnelse"
        brevbestillingDto.ektefelleFritekst shouldBe "Ektefelle omfattet"
        brevbestillingDto.barnFritekst shouldBe "Barn omfattet"
        brevbestillingDto.nyVurderingBakgrunn shouldBe null
        brevbestillingDto.standardvedleggType shouldBe null

        brevbestillingDto.kopiMottakere shouldHaveSize 2
        brevbestillingDto.kopiMottakere[0].rolle() shouldBe ARBEIDSGIVER
        brevbestillingDto.kopiMottakere[1].rolle() shouldBe UTENLANDSK_TRYGDEMYNDIGHET
    }

    @Test
    fun `fattVedtak korrigert vedtak fatterVedtak`() {
        val behandlingsresultat = lagBehandlingsresultat()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val request = lagFattVedtakRequest(KORRIGERT_VEDTAK, Nyvurderingbakgrunner.FEIL_I_BEHANDLING.kode)
        trygdeavtaleVedtakService.fattVedtak(lagBehandling(), request)

        val behandlingsresultatSlot = slot<Behandlingsresultat>()
        val behandlingSlot = slot<Behandling>()
        val brevbestillingSlot = slot<BrevbestillingDto>()

        verify { behandlingsresultatService.lagre(capture(behandlingsresultatSlot)) }
        verify { behandlingService.endreStatus(capture(behandlingSlot), IVERKSETTER_VEDTAK) }
        verify { prosessinstansService.opprettProsessinstansIverksettVedtakTrygdeavtale(any<Behandling>()) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify { dokgenService.produserOgDistribuerBrev(any<Long>(), capture(brevbestillingSlot)) }
        verify {
            ferdigbehandlingKontrollFacade.kontrollerVedtakMedRegisteropplysninger(
                any<Behandling>(),
                Sakstyper.TRYGDEAVTALE,
                any<Behandlingsresultattyper>(),
                null
            )
        }

        val lagretBehandlingsresultat = behandlingsresultatSlot.captured
        lagretBehandlingsresultat.type shouldBe FASTSATT_LOVVALGSLAND
        lagretBehandlingsresultat.begrunnelseFritekst shouldBe "Begrunnelse"
        lagretBehandlingsresultat.fastsattAvLand shouldBe Land_iso2.NO

        val lagretBehandling = behandlingSlot.captured
        lagretBehandling.fagsak.status shouldBe MEDLEMSKAP_AVKLART

        val brevbestillingDto = brevbestillingSlot.captured
        brevbestillingDto.produserbardokument shouldBe TRYGDEAVTALE_GB
        brevbestillingDto.bestillersId shouldBe "Z990007"
        brevbestillingDto.mottaker shouldBe BRUKER
        brevbestillingDto.innledningFritekst shouldBe "Innledning"
        brevbestillingDto.begrunnelseFritekst shouldBe "Begrunnelse"
        brevbestillingDto.ektefelleFritekst shouldBe "Ektefelle omfattet"
        brevbestillingDto.barnFritekst shouldBe "Barn omfattet"
        brevbestillingDto.nyVurderingBakgrunn shouldBe Nyvurderingbakgrunner.FEIL_I_BEHANDLING.kode
        brevbestillingDto.standardvedleggType shouldBe StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE

        brevbestillingDto.kopiMottakere shouldHaveSize 2
        brevbestillingDto.kopiMottakere[0].rolle() shouldBe ARBEIDSGIVER
        brevbestillingDto.kopiMottakere[1].rolle() shouldBe UTENLANDSK_TRYGDEMYNDIGHET
    }

    @Test
    fun `fattVedtak endringsvedtak fatterVedtak`() {
        val behandlingsresultat = lagBehandlingsresultat()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val request = lagFattVedtakRequest(ENDRINGSVEDTAK, Nyvurderingbakgrunner.NYE_OPPLYSNINGER.kode)
        trygdeavtaleVedtakService.fattVedtak(lagBehandling(), request)

        val behandlingsresultatSlot = slot<Behandlingsresultat>()
        val behandlingSlot = slot<Behandling>()
        val brevbestillingSlot = slot<BrevbestillingDto>()

        verify { behandlingsresultatService.lagre(capture(behandlingsresultatSlot)) }
        verify { behandlingService.endreStatus(capture(behandlingSlot), IVERKSETTER_VEDTAK) }
        verify { prosessinstansService.opprettProsessinstansIverksettVedtakTrygdeavtale(any<Behandling>()) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify { dokgenService.produserOgDistribuerBrev(any<Long>(), capture(brevbestillingSlot)) }
        verify {
            ferdigbehandlingKontrollFacade.kontrollerVedtakMedRegisteropplysninger(
                any<Behandling>(),
                Sakstyper.TRYGDEAVTALE,
                any<Behandlingsresultattyper>(),
                null
            )
        }

        val lagretBehandlingsresultat = behandlingsresultatSlot.captured
        lagretBehandlingsresultat.type shouldBe FASTSATT_LOVVALGSLAND
        lagretBehandlingsresultat.begrunnelseFritekst shouldBe "Begrunnelse"
        lagretBehandlingsresultat.fastsattAvLand shouldBe Land_iso2.NO

        val lagretBehandling = behandlingSlot.captured
        lagretBehandling.fagsak.status shouldBe MEDLEMSKAP_AVKLART

        val brevbestillingDto = brevbestillingSlot.captured
        brevbestillingDto.produserbardokument shouldBe TRYGDEAVTALE_GB
        brevbestillingDto.bestillersId shouldBe "Z990007"
        brevbestillingDto.mottaker shouldBe BRUKER
        brevbestillingDto.innledningFritekst shouldBe "Innledning"
        brevbestillingDto.begrunnelseFritekst shouldBe "Begrunnelse"
        brevbestillingDto.ektefelleFritekst shouldBe "Ektefelle omfattet"
        brevbestillingDto.barnFritekst shouldBe "Barn omfattet"
        brevbestillingDto.nyVurderingBakgrunn shouldBe Nyvurderingbakgrunner.NYE_OPPLYSNINGER.kode
        brevbestillingDto.standardvedleggType shouldBe StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE

        brevbestillingDto.kopiMottakere shouldHaveSize 2
        brevbestillingDto.kopiMottakere[0].rolle() shouldBe ARBEIDSGIVER
        brevbestillingDto.kopiMottakere[1].rolle() shouldBe UTENLANDSK_TRYGDEMYNDIGHET
    }

    @Test
    fun `fattVedtak avslag manglende opplysninger fatterVedtak`() {
        val behandlingsresultat = Behandlingsresultat()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val request = FattVedtakRequest.Builder().apply {
            medBehandlingsresultatType(AVSLAG_MANGLENDE_OPPL)
            medVedtakstype(FØRSTEGANGSVEDTAK)
            medFritekst("fritekst for beskrivelse avslag")
            medBestillersId(SubjectHandler.getInstance().userID)
        }.build()

        trygdeavtaleVedtakService.fattVedtak(lagBehandling(), request)

        val behandlingsresultatSlot = slot<Behandlingsresultat>()
        val behandlingSlot = slot<Behandling>()
        val brevbestillingSlot = slot<BrevbestillingDto>()

        verify { behandlingsresultatService.lagre(capture(behandlingsresultatSlot)) }
        verify { behandlingService.endreStatus(capture(behandlingSlot), IVERKSETTER_VEDTAK) }
        verify { prosessinstansService.opprettProsessinstansIverksettVedtakTrygdeavtale(any<Behandling>()) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify { dokgenService.produserOgDistribuerBrev(any<Long>(), capture(brevbestillingSlot)) }

        val lagretBehandlingsresultat = behandlingsresultatSlot.captured
        lagretBehandlingsresultat.type shouldBe AVSLAG_MANGLENDE_OPPL

        val lagretBehandling = behandlingSlot.captured
        lagretBehandling.fagsak.status shouldBe MEDLEMSKAP_AVKLART

        val brevbestillingDto = brevbestillingSlot.captured
        brevbestillingDto.produserbardokument shouldBe AVSLAG_MANGLENDE_OPPLYSNINGER
        brevbestillingDto.bestillersId shouldBe "Z990007"
        brevbestillingDto.mottaker shouldBe BRUKER
        brevbestillingDto.fritekst shouldBe "fritekst for beskrivelse avslag"
        brevbestillingDto.standardvedleggType shouldBe null
        brevbestillingDto.kopiMottakere shouldHaveSize 0
    }

    private fun lagFattVedtakRequest(vedtakstype: Vedtakstyper, nyVurderingBakgrunn: String?) =
        FattVedtakRequest.Builder().apply {
            medBehandlingsresultatType(FASTSATT_LOVVALGSLAND)
            medVedtakstype(vedtakstype)
            medInnledningFritekst("Innledning")
            medBegrunnelseFritekst("Begrunnelse")
            medEktefelleFritekst("Ektefelle omfattet")
            medBarnFritekst("Barn omfattet")
            medKopiMottakere(
                listOf(
                    KopiMottakerDto(ARBEIDSGIVER, "987654321", null, null),
                    KopiMottakerDto(UTENLANDSK_TRYGDEMYNDIGHET, null, null, "GB:UK010")
                )
            )
            medBestillersId(SubjectHandler.getInstance().userID)
            medNyVurderingBakgrunn(nyVurderingBakgrunn)
        }.build()

    private fun lagBehandling() = BehandlingTestFactory.builderWithDefaults()
        .medId(BEHANDLING_ID)
        .medFagsak(FagsakTestFactory.lagFagsak())
        .medMottatteOpplysninger(lagMottatteOpplysninger())
        .build()

    private fun lagMottatteOpplysninger() = MottatteOpplysninger().apply {
        mottatteOpplysningerData = MottatteOpplysningerData().apply {
            soeknadsland.landkoder = listOf(Land_iso2.GB.kode)
        }
    }

    private fun lagBehandlingsresultat(): Behandlingsresultat {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            lovvalgsland = Land_iso2.GB
        }

        return Behandlingsresultat().apply {
            lovvalgsperioder = setOf(lovvalgsperiode)
        }
    }
}