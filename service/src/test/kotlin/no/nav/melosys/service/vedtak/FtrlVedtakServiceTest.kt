package no.nav.melosys.service.vedtak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.DokgenService
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.KopiMottakerDto
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.vilkaar.VilkaarsresultatService
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class FtrlVedtakServiceTest {
    val SAKSNUMMER = "MEL-123"
    val BEH_ID = 123L

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
    private lateinit var vilkaarsresultatService: VilkaarsresultatService

    private var behandlingsresultatSlot = slot<Behandlingsresultat>()
    private var behandlingSlot = slot<Behandling>()
    private var brevbestillingRequestSlot = slot<BrevbestillingDto>()

    private lateinit var ftrlVedtakService: FtrlVedtakService

    @BeforeEach
    fun setup() {
        ftrlVedtakService = FtrlVedtakService(
            behandlingsresultatService,
            behandlingService,
            prosessinstansService,
            oppgaveService,
            dokgenService,
            vilkaarsresultatService
        )
        behandlingsresultatSlot.clear()
        behandlingSlot.clear()
        brevbestillingRequestSlot.clear()
        SpringSubjectHandler.set(TestSubjectHandler())
    }

    @Test
    fun fattVedtak_Førstegangsvedtak_fatterVedtak() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_ID) } returns Behandlingsresultat()
        val request = lagFattVedtakRequest(
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN,
            innledningFritekst = "Innledning",
            begrunnelseFritekst = "Begrunnelse",
            ekteFelleFritekst = "Ektefelle omfattet",
            barnFritekst = "Barn omfattet",
            kopiMottakere = listOf(
                KopiMottakerDto(Mottakerroller.ARBEIDSGIVER, "987654321", null, null),
                KopiMottakerDto(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET, null, null, "GB:UK010")
            )
        )


        ftrlVedtakService.fattVedtak(lagBehandling(), request)


        verify { behandlingsresultatService.lagre(capture(behandlingsresultatSlot)) }
        verify { behandlingService.endreStatus(capture(behandlingSlot), Behandlingsstatus.IVERKSETTER_VEDTAK) }
        verify { prosessinstansService.opprettProsessinstansIverksettVedtakFTRL(any(), eq(request.tilVedtakRequest())) }
        verify { oppgaveService.ferdigstillOppgaveMedSaksnummer(SAKSNUMMER) }
        verify { dokgenService.produserOgDistribuerBrev(BEH_ID, capture(brevbestillingRequestSlot)) }

        behandlingsresultatSlot.captured.shouldNotBeNull().run {
            type.shouldBe(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)
            begrunnelseFritekst.shouldBe(request.begrunnelseFritekst)
            fastsattAvLand.shouldBe(Land_iso2.NO)
        }
        behandlingSlot.captured.shouldNotBeNull().fagsak.status.shouldBe(Saksstatuser.MEDLEMSKAP_AVKLART)
        brevbestillingRequestSlot.captured.shouldNotBeNull().run {
            produserbardokument.shouldBe(Produserbaredokumenter.INNVILGELSE_FOLKETRYGDLOVEN)
            bestillersId.shouldBe("Z990007")
            mottaker.shouldBe(Mottakerroller.BRUKER)
            innledningFritekst.shouldBe(request.innledningFritekst)
            begrunnelseFritekst.shouldBe(request.begrunnelseFritekst)
            ektefelleFritekst.shouldBe(request.ektefelleFritekst)
            barnFritekst.shouldBe(request.barnFritekst)
            kopiMottakere.shouldHaveSize(2).toList().run {
                get(0).rolle.shouldBe(Mottakerroller.ARBEIDSGIVER)
                get(1).rolle.shouldBe(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET)
            }
        }
    }

    @Test
    fun fattVedtak_avslag_manglende_opplysninger_fatterVedtak() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_ID) } returns Behandlingsresultat()
        val request = lagFattVedtakRequest(type = Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, fritekst = "fritekst for beskrivelse avslag")


        ftrlVedtakService.fattVedtak(lagBehandling(), request)


        verify { behandlingsresultatService.lagre(capture(behandlingsresultatSlot)) }
        verify { behandlingService.endreStatus(capture(behandlingSlot), Behandlingsstatus.IVERKSETTER_VEDTAK) }
        verify { prosessinstansService.opprettProsessinstansIverksettVedtakFTRL(any(), eq(request.tilVedtakRequest())) }
        verify { oppgaveService.ferdigstillOppgaveMedSaksnummer(SAKSNUMMER) }
        verify { dokgenService.produserOgDistribuerBrev(BEH_ID, capture(brevbestillingRequestSlot)) }

        behandlingsresultatSlot.captured.shouldNotBeNull().run {
            type.shouldBe(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL)
            begrunnelseFritekst.shouldBe(request.begrunnelseFritekst)
            fastsattAvLand.shouldBe(Land_iso2.NO)
        }
        behandlingSlot.captured.shouldNotBeNull().fagsak.status.shouldBe(Saksstatuser.MEDLEMSKAP_AVKLART)
        brevbestillingRequestSlot.captured.shouldNotBeNull().run {
            produserbardokument.shouldBe(Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER)
            bestillersId.shouldBe("Z990007")
            mottaker.shouldBe(Mottakerroller.BRUKER)
            fritekst.shouldBe(request.fritekst)
            kopiMottakere.shouldBeEmpty()
        }
    }

    @Test
    fun fattVedtak_delvis_opphørt_fatterVedtak() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_ID) } returns Behandlingsresultat().apply {
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.OPPHØRT
                    fom = LocalDate.now()
                })
            }
        }
        val request = lagFattVedtakRequest(
            type = Behandlingsresultattyper.DELVIS_OPPHØRT,
            begrunnelseFritekst = "fritekst for begrunnelse",
            opphørtDato = LocalDate.now()
        )


        ftrlVedtakService.fattVedtak(lagBehandling(), request)


        verify { behandlingsresultatService.lagre(capture(behandlingsresultatSlot)) }
        verify { behandlingService.endreStatus(capture(behandlingSlot), Behandlingsstatus.IVERKSETTER_VEDTAK) }
        verify { prosessinstansService.opprettProsessinstansIverksettVedtakFTRL(any(), eq(request.tilVedtakRequest())) }
        verify { oppgaveService.ferdigstillOppgaveMedSaksnummer(SAKSNUMMER) }
        verify { dokgenService.produserOgDistribuerBrev(BEH_ID, capture(brevbestillingRequestSlot)) }
        verify(exactly = 0) { vilkaarsresultatService.tømVilkårForBehandlingsresultat(any()) }

        behandlingsresultatSlot.captured.shouldNotBeNull().run {
            type.shouldBe(Behandlingsresultattyper.DELVIS_OPPHØRT)
            begrunnelseFritekst.shouldBe(request.begrunnelseFritekst)
            fastsattAvLand.shouldBe(Land_iso2.NO)
        }
        behandlingSlot.captured.shouldNotBeNull().fagsak.status.shouldBe(Saksstatuser.MEDLEMSKAP_AVKLART)
        brevbestillingRequestSlot.captured.shouldNotBeNull().run {
            produserbardokument.shouldBe(Produserbaredokumenter.VEDTAK_OPPHOERT_MEDLEMSKAP)
            bestillersId.shouldBe("Z990007")
            mottaker.shouldBe(Mottakerroller.BRUKER)
            begrunnelseFritekst.shouldBe(request.begrunnelseFritekst)
            opphørsdato.shouldBe(request.opphørsdato)
            kopiMottakere.shouldBeEmpty()
        }
    }

    @Test
    fun fattVedtak_opphørt_fatterVedtak() {
        val behandlingsresultat = Behandlingsresultat().apply {
            avklartefakta = mutableSetOf(Avklartefakta(), Avklartefakta().apply {
                type = Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING
                referanse = Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING.kode
            })
            medlemAvFolketrygden = MedlemAvFolketrygden()
            medlemAvFolketrygden.medlemskapsperioder =
                mutableSetOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                })
            utfallRegistreringUnntak = Utfallregistreringunntak.GODKJENT
            nyVurderingBakgrunn = "blah"
            innledningFritekst = "blah"
            begrunnelseFritekst = "blah"
            trygdeavgiftFritekst = "blah"
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_ID) } returns behandlingsresultat
        val request = lagFattVedtakRequest(
            type = Behandlingsresultattyper.OPPHØRT,
            begrunnelseFritekst = "fritekst for begrunnelse",
            opphørtDato = LocalDate.now()
        )


        ftrlVedtakService.fattVedtak(lagBehandling(), request)


        verify { behandlingsresultatService.lagre(capture(behandlingsresultatSlot)) }
        verify { behandlingService.endreStatus(capture(behandlingSlot), Behandlingsstatus.IVERKSETTER_VEDTAK) }
        verify { prosessinstansService.opprettProsessinstansIverksettVedtakFTRL(any(), eq(request.tilVedtakRequest())) }
        verify { oppgaveService.ferdigstillOppgaveMedSaksnummer(SAKSNUMMER) }
        verify { dokgenService.produserOgDistribuerBrev(BEH_ID, capture(brevbestillingRequestSlot)) }
        verify { vilkaarsresultatService.tømVilkårForBehandlingsresultat(any()) }

        behandlingsresultatSlot.captured.shouldNotBeNull().run {
            type.shouldBe(Behandlingsresultattyper.OPPHØRT)
            begrunnelseFritekst.shouldBe(request.begrunnelseFritekst)
            fastsattAvLand.shouldBe(Land_iso2.NO)
            medlemAvFolketrygden.bestemmelse.shouldBe(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD)
            medlemAvFolketrygden.medlemskapsperioder.shouldHaveSize(1).first()!!.innvilgelsesresultat.shouldBe(InnvilgelsesResultat.OPPHØRT)
            avklartefakta.shouldHaveSize(1)
            utfallRegistreringUnntak.shouldBeNull()
            nyVurderingBakgrunn.shouldBeNull()
            innledningFritekst.shouldBeNull()
            trygdeavgiftFritekst.shouldBeNull()
            begrunnelseFritekst.shouldBe(request.begrunnelseFritekst)
            vedtakMetadata.vedtakstype.shouldBe(request.vedtakstype)
        }
        behandlingSlot.captured.shouldNotBeNull().fagsak.status.shouldBe(Saksstatuser.OPPHØRT)
        brevbestillingRequestSlot.captured.shouldNotBeNull().run {
            produserbardokument.shouldBe(Produserbaredokumenter.VEDTAK_OPPHOERT_MEDLEMSKAP)
            bestillersId.shouldBe("Z990007")
            mottaker.shouldBe(Mottakerroller.BRUKER)
            begrunnelseFritekst.shouldBe(request.begrunnelseFritekst)
            opphørsdato.shouldBe(request.opphørsdato)
            kopiMottakere.shouldBeEmpty()
        }
    }

    @Test
    fun fattVedtak_opphørt_manglerAvklartFakta_kasterFeil() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_ID) } returns Behandlingsresultat()
        val request = lagFattVedtakRequest(
            type = Behandlingsresultattyper.OPPHØRT,
            begrunnelseFritekst = "fritekst for begrunnelse",
            opphørtDato = LocalDate.now()
        )
        val behandling = lagBehandling()


        shouldThrow<FunksjonellException> {
            ftrlVedtakService.fattVedtak(behandling, request)
        }.shouldHaveMessage("Forventer at fullstendigManglendeInnbetaling er satt ved fatting av vedtak for behandlingstype OPPHØRT")
    }

    @Test
    fun fattVedtak_opphørt_feilOpphørtDato_kasterFeil() {
        val behandlingsresultat = Behandlingsresultat().apply {
            avklartefakta = mutableSetOf(Avklartefakta(), Avklartefakta().apply {
                type = Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING
                referanse = Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING.kode
            })
            medlemAvFolketrygden = MedlemAvFolketrygden()
            medlemAvFolketrygden.medlemskapsperioder =
                mutableSetOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                })
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_ID) } returns behandlingsresultat
        val request = lagFattVedtakRequest(
            type = Behandlingsresultattyper.OPPHØRT,
            begrunnelseFritekst = "fritekst for begrunnelse",
            opphørtDato = LocalDate.now().plusDays(1)
        )
        val behandling = lagBehandling()


        shouldThrow<FunksjonellException> {
            ftrlVedtakService.fattVedtak(behandling, request)
        }.shouldHaveMessage("Medsendt opphørsdato: ${request.opphørsdato} er ikke lik forventet opphørsdato: ${behandlingsresultat.medlemAvFolketrygden.utledOpphørtDato()}")
    }


    private fun lagFattVedtakRequest(
        type: Behandlingsresultattyper,
        vedtakstype: Vedtakstyper = Vedtakstyper.FØRSTEGANGSVEDTAK,
        fritekst: String? = null,
        innledningFritekst: String? = null,
        begrunnelseFritekst: String? = null,
        ekteFelleFritekst: String? = null,
        barnFritekst: String? = null,
        opphørtDato: LocalDate? = null,
        kopiMottakere: List<KopiMottakerDto> = emptyList()
    ): FattVedtakRequest =
        FattVedtakRequest.Builder()
            .medBehandlingsresultatType(type)
            .medVedtakstype(vedtakstype)
            .medFritekst(fritekst)
            .medInnledningFritekst(innledningFritekst)
            .medBegrunnelseFritekst(begrunnelseFritekst)
            .medEktefelleFritekst(ekteFelleFritekst)
            .medBarnFritekst(barnFritekst)
            .medOpphørsdato(opphørtDato)
            .medKopiMottakere(kopiMottakere)
            .medBestillersId(SubjectHandler.getInstance().getUserID())
            .build()

    private fun lagBehandling(): Behandling =
        Behandling().apply {
            id = BEH_ID
            fagsak = Fagsak().apply { saksnummer = SAKSNUMMER }
        }
}
