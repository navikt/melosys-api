package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.aarsavregning.GjeldendeBehandlingsresultaterForÅrsavregning
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.DokumentServiceFasade
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SkattepliktigeInnhentingsbrevServiceTest {

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var årsavregningService: ÅrsavregningService

    @MockK
    private lateinit var trygdeavgiftMottakerService: TrygdeavgiftMottakerService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var innhentingsbrevUtsender: InnhentingsbrevUtsender

    private val service: SkattepliktigeInnhentingsbrevService by lazy {
        SkattepliktigeInnhentingsbrevService(
            fagsakService,
            årsavregningService,
            trygdeavgiftMottakerService,
            behandlingsresultatService,
            innhentingsbrevUtsender,
        )
    }

    @Test
    fun `dryrun sender ikke brev men rapporterer at brevet ville blitt sendt`() {
        stubSakMedAktivÅrsavregning(BEHANDLING_ID, AKTØR_ID)

        service.prosesserSkattehendelser(input(AKTØR_ID), skarp = false)

        verify(exactly = 0) { innhentingsbrevUtsender.sendInnhentingsbrev(any()) }
        service.status()["antallVilleSendtBrev"] shouldBe 1
        service.status()["antallBrevSendt"] shouldBe 0
        service.resultater.single().villeSendtBrev shouldBe true
        service.resultater.single().brevSendt shouldBe null
    }

    @Test
    fun `skarp sender innhentingsbrev for den eksisterende årsavregningsbehandlingen`() {
        stubSakMedAktivÅrsavregning(BEHANDLING_ID, AKTØR_ID)
        every { innhentingsbrevUtsender.sendInnhentingsbrev(BEHANDLING_ID) } just Runs

        service.prosesserSkattehendelser(input(AKTØR_ID), skarp = true)

        verify(exactly = 1) { innhentingsbrevUtsender.sendInnhentingsbrev(BEHANDLING_ID) }
        service.status()["antallBrevSendt"] shouldBe 1
        service.resultater.single().brevSendt shouldBe true
        service.resultater.single().behandlingId shouldBe BEHANDLING_ID
    }

    @Test
    fun `sak uten aktiv årsavregning får ikke brev og telles som utenAarsavregning`() {
        val fagsak = lagFagsak()
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        stubTrygdeavgiftFilter(fagsak)

        service.prosesserSkattehendelser(input(AKTØR_ID), skarp = true)

        verify(exactly = 0) { innhentingsbrevUtsender.sendInnhentingsbrev(any()) }
        service.status()["antallUtenAarsavregning"] shouldBe 1
        service.status()["antallBrevSendt"] shouldBe 0
        service.resultater.single().harAktivAarsavregning shouldBe false
    }

    @Test
    fun `maksAntall begrenser antall saker også i dryrun`() {
        stubSakMedAktivÅrsavregning(BEHANDLING_ID, AKTØR_ID)

        service.prosesserSkattehendelser(
            input(AKTØR_ID) + InnhentingsbrevItem(GJELDER_ÅR.toString(), ANNEN_AKTØR_ID),
            skarp = false,
            maksAntall = 1,
        )

        service.status()["antallVilleSendtBrev"] shouldBe 1
        // Stoppet før oppslag på neste aktør
        verify(exactly = 0) { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, ANNEN_AKTØR_ID) }
    }

    @Test
    fun `maksAntall begrenser antall sendte brev i skarp`() {
        stubSakMedAktivÅrsavregning(BEHANDLING_ID, AKTØR_ID)
        every { innhentingsbrevUtsender.sendInnhentingsbrev(BEHANDLING_ID) } just Runs

        service.prosesserSkattehendelser(
            input(AKTØR_ID) + InnhentingsbrevItem(GJELDER_ÅR.toString(), ANNEN_AKTØR_ID),
            skarp = true,
            maksAntall = 1,
        )

        verify(exactly = 1) { innhentingsbrevUtsender.sendInnhentingsbrev(any()) }
        service.status()["antallBrevSendt"] shouldBe 1
        verify(exactly = 0) { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, ANNEN_AKTØR_ID) }
    }

    @Test
    fun `samme behandling fra to input-rader gir kun ett brev og telles som duplikat`() {
        stubSakMedAktivÅrsavregning(BEHANDLING_ID, AKTØR_ID)
        every { innhentingsbrevUtsender.sendInnhentingsbrev(BEHANDLING_ID) } just Runs

        // Samme identifikator to ganger -> samme behandling slås opp to ganger
        service.prosesserSkattehendelser(input(AKTØR_ID) + input(AKTØR_ID), skarp = true)

        verify(exactly = 1) { innhentingsbrevUtsender.sendInnhentingsbrev(BEHANDLING_ID) }
        service.status()["antallBrevSendt"] shouldBe 1
        service.status()["antallDuplikatHoppetOver"] shouldBe 1
        service.resultater.count { it.brevSendt == true } shouldBe 1
        service.resultater.count { it.feilmelding?.startsWith("duplikat") == true } shouldBe 1
    }

    private fun input(aktørId: String) = listOf(InnhentingsbrevItem(GJELDER_ÅR.toString(), aktørId))

    private fun stubSakMedAktivÅrsavregning(behandlingId: Long, aktørId: String) {
        val fagsak = Fagsak.forTest {
            type = Sakstyper.EU_EOS
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            status(Saksstatuser.OPPRETTET)
            behandling {
                id = behandlingId
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.UNDER_BEHANDLING
            }
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            this.behandling { id = behandlingId }
            årsavregning { aar = GJELDER_ÅR }
        }

        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, aktørId) } returns listOf(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat
        stubTrygdeavgiftFilter(fagsak, behandlingsresultat)
        every { trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat) } returns
            Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT
    }

    private fun stubTrygdeavgiftFilter(fagsak: Fagsak, behandlingsresultat: Behandlingsresultat = mockk()) {
        every {
            årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(fagsak.saksnummer, GJELDER_ÅR)
        } returns GjeldendeBehandlingsresultaterForÅrsavregning(
            sisteBehandlingsresultatMedAvgift = behandlingsresultat,
        )
        every { trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat) } returns true
    }

    private fun lagFagsak() = Fagsak.forTest {
        type = Sakstyper.EU_EOS
        tema = Sakstemaer.MEDLEMSKAP_LOVVALG
        status(Saksstatuser.OPPRETTET)
    }

    companion object {
        const val AKTØR_ID = "456789123"
        const val ANNEN_AKTØR_ID = "999888777"
        const val GJELDER_ÅR = 2024
        const val BEHANDLING_ID = 100L
    }
}

@ExtendWith(MockKExtension::class)
class InnhentingsbrevUtsenderTest {

    @MockK
    private lateinit var dokumentServiceFasade: DokumentServiceFasade

    @Test
    fun `sender INNHENTING-brev til BRUKER for behandlingen`() {
        val utsender = InnhentingsbrevUtsender(dokumentServiceFasade)
        every { dokumentServiceFasade.produserDokument(BEHANDLING_ID, any()) } just Runs

        utsender.sendInnhentingsbrev(BEHANDLING_ID)

        verify(exactly = 1) {
            dokumentServiceFasade.produserDokument(
                BEHANDLING_ID,
                match<BrevbestillingDto> {
                    it.produserbardokument == Produserbaredokumenter.INNHENTING_AV_INNTEKTSOPPLYSNINGER &&
                        it.mottaker == Mottakerroller.BRUKER
                }
            )
        }
    }

    companion object {
        const val BEHANDLING_ID = 100L
    }
}
