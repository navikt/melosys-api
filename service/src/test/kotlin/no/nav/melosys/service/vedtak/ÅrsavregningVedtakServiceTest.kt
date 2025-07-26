package no.nav.melosys.service.vedtak

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.brev.StandardvedleggType
import no.nav.melosys.domain.buildWithDefaults
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.DokgenService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockKExtension::class)
class ÅrsavregningVedtakServiceTest {
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

    @BeforeAll
    fun beforeAll() {
        SubjectHandler.set(TestSubjectHandler())
    }


    @Test
    fun fattVedtak_medBehandlingTypeÅrsavregning_fatterVedtak() {
        val BEH_ID = 123L

        val årsavregningVedtakService = ÅrsavregningVedtakService(
            prosessinstansService, behandlingService,
            behandlingsresultatService, oppgaveService,
            dokgenService
        )

        every { behandlingsresultatService.hentBehandlingsresultat(any()) }.returns(Behandlingsresultat())
        every { behandlingsresultatService.lagre(any()) } returnsArgument 0

        val request = lagFattVedtakRequest(
            behandlingsResultattype = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN,
            nyVurderingBakgrunn = "nyVurderingBakgrunn",
            begrunnelseFritekst = "begrunnelseFritekst",
            innledningFritekst = "innledningFritekst",
            trygdeavgiftFritekst = "trygdeavgiftFritekst"
        )
        val behandling = Behandling.buildWithDefaults {
            id = BEH_ID
            fagsak = FagsakTestFactory.lagFagsak()
            tema = Behandlingstema.YRKESAKTIV
            type = Behandlingstyper.ÅRSAVREGNING
        }


        årsavregningVedtakService.fattVedtak(behandling, request)


        verify {
            behandlingsresultatService.lagre(withArg {
                it.type shouldBe request.behandlingsresultatTypeKode
                it.nyVurderingBakgrunn shouldBe request.nyVurderingBakgrunn
                it.begrunnelseFritekst shouldBe request.begrunnelseFritekst
                it.innledningFritekst shouldBe request.innledningFritekst
                it.trygdeavgiftFritekst shouldBe request.trygdeavgiftFritekst
            })
        }
        verify { behandlingService.endreStatus(behandling, Behandlingsstatus.IVERKSETTER_VEDTAK) }
        verify { prosessinstansService.opprettProsessinstansIverksettVedtakÅrsavregning(behandling) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEH_ID) }
        verify {
            dokgenService.produserOgDistribuerBrev(BEH_ID,
                withArg {
                    it.produserbardokument shouldBe Produserbaredokumenter.AARSAVREGNING_VEDTAKSBREV
                    it.standardvedleggType shouldBe StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_AVSLAG
                })
        }
    }

    private fun lagFattVedtakRequest(
        behandlingsResultattype: Behandlingsresultattyper,
        nyVurderingBakgrunn: String? = null,
        vedtakstype: Vedtakstyper = Vedtakstyper.FØRSTEGANGSVEDTAK,
        innledningFritekst: String? = null,
        begrunnelseFritekst: String? = null,
        trygdeavgiftFritekst: String? = null,
    ): FattVedtakRequest =
        FattVedtakRequest.Builder().medBestillersId(SubjectHandler.getInstance().getUserID()).medBehandlingsresultatType(behandlingsResultattype)
            .medNyVurderingBakgrunn(nyVurderingBakgrunn).medVedtakstype(vedtakstype).medBegrunnelseFritekst(begrunnelseFritekst)
            .medInnledningFritekst(innledningFritekst).medTrygdeavgiftFritekst(trygdeavgiftFritekst).build()

}
