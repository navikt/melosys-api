package no.nav.melosys.itest.vedtak.satsendring

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.OpprettForslagMedlemskapsperiodeService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.sak.OpprettBehandlingForSak
import no.nav.melosys.service.sak.OpprettSakDto
import no.nav.melosys.service.vedtak.FattVedtakRequest
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import no.nav.melosys.service.vilkaar.VilkaarDto
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import no.nav.melosys.tjenester.gui.config.ApiKeyInterceptor
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import wiremock.com.google.common.net.HttpHeaders
import java.time.LocalDate

@AutoConfigureMockMvc
class SatsendringAdminControllerIT @Autowired constructor(
    private val mockMvc: MockMvc,
    private val mockOAuth2Server: MockOAuth2Server,
    private val opprettBehandlingForSak: OpprettBehandlingForSak,
    private val prosessinstansRepository: ProsessinstansRepository,
    avklartefaktaService: AvklartefaktaService,
    mottatteOpplysningerService: MottatteOpplysningerService,
    opprettForslagMedlemskapsperiodeService: OpprettForslagMedlemskapsperiodeService,
    trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    vedtaksfattingFasade: VedtaksfattingFasade,
    vilkaarsresultatService: VilkaarsresultatService
) : SatsendringTestBase(
    avklartefaktaService,
    mottatteOpplysningerService,
    opprettForslagMedlemskapsperiodeService,
    trygdeavgiftsberegningService,
    vedtaksfattingFasade,
    vilkaarsresultatService
) {

    private val testYear = 2024

    private fun hentBearerToken(): String {
        return mockOAuth2Server.issueToken(
            issuerId = "issuer1",
            subject = "testbruker",
            audience = "dumbdumb",
            claims = mapOf(
                "oid" to "test-oid",
                "azp" to "test-azp",
                "NAVident" to "test123"
            )
        ).serialize()
    }

    @Test
    fun `ProsessGenerator skal opprette prosesser for behandlinger som trenger satsendring`() {
        // Opprett behandlinger som blir påvirket av satsendringer
        val behandlingMedSatsendring = lagFørstegangsbehandlingMedSatsendring()
        val behandlingUtenSatsendring = lagFørstegangsbehandlingUtenSatsendring()
        val behandlingMedSatsendringOgNyVurdering = lagFørstegangsbehandlingMedSatsendringOgNyVurdering()

        val prosesserFørKjøring = prosessinstansRepository.findAll()


        // Trigger SatsendringProsessGenerator via admin-endepunkt
        executeAndWait(
            waitForProsesses = mapOf(
                ProsessType.SATSENDRING to 1,
                ProsessType.SATSENDRING_TILBAKESTILL_NY_VURDERING to 1
            )
        ) {
            // Lagre gjeldende prosessID fra testklasse før den slettes etter forespørselen fra controlleren.
            val processID = ThreadLocalAccessInfo.getProcessId()

            mockMvc.perform(
                MockMvcRequestBuilders.post("/admin/satsendringer/${testYear}?dryRun=false")
                    .header(ApiKeyInterceptor.Companion.API_KEY_HEADER, "dummy")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken()}")
            ).andExpect(MockMvcResultMatchers.status().isAccepted)

            // Gjenopprett ThreadLocal-konteksten med samme UUID
            ThreadLocalAccessInfo.beforeExecuteProcess(processID, "steg")
        }


        // Verifiser at riktig behandling har fått riktig prosess
        val nyeProsesser = prosessinstansRepository.findAll().filter { it !in prosesserFørKjøring }
        nyeProsesser shouldHaveSize 2

        val satsendringProsess = nyeProsesser.find { it.type == ProsessType.SATSENDRING }
        satsendringProsess.shouldNotBeNull().behandlingOrFail().opprinneligBehandling!!.id shouldBe behandlingMedSatsendring.id

        val tilbakestillProsess = nyeProsesser.find { it.type == ProsessType.SATSENDRING_TILBAKESTILL_NY_VURDERING }
        tilbakestillProsess.shouldNotBeNull().behandlingOrFail().opprinneligBehandling!!.id shouldBe behandlingMedSatsendringOgNyVurdering.id

        // Behandling uten satsendring skal ikke ha ny prosess
        nyeProsesser.filter { it.behandling?.id == behandlingUtenSatsendring.id }.shouldBeEmpty()
    }

    private fun lagFørstegangsbehandlingMedSatsendring(): Behandling {
        // Opprett en periode som vil bli påvirket av satsendring (April 2024). Perioden matcher perioden i stubbing som utløser satsendring
        val medlemskapsperiode = Periode(LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30))
        return lagFørstegangsbehandling(medlemskapsperiode)
    }

    private fun lagFørstegangsbehandlingUtenSatsendring(): Behandling {
        // Opprett en periode som IKKE vil bli påvirket av satsendring (Q1 2024)
        val medlemskapsperiode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31))
        return lagFørstegangsbehandling(medlemskapsperiode)
    }

    private fun lagFørstegangsbehandlingMedSatsendringOgNyVurdering(): Behandling {
        // Først lag en annen førstegangsbehandling som trenger satsendring
        val medlemskapsperiode = Periode(LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 31))
        val førstegangsbehandling = lagFørstegangsbehandling(medlemskapsperiode)

        // Deretter opprett ny vurdering behandling for førstegangsbehandlingen
        val nyVurderingBehandling = executeAndWait(
            mapOf(
                ProsessType.OPPRETT_REPLIKERT_BEHANDLING_FOR_SAK to 1
            )
        ) {
            opprettBehandlingForSak.opprettBehandling(
                førstegangsbehandling.fagsak.saksnummer,
                OpprettSakDto().apply {
                    behandlingstema = Behandlingstema.YRKESAKTIV
                    behandlingstype = Behandlingstyper.NY_VURDERING
                    mottaksdato = LocalDate.now()
                    behandlingsaarsakType = Behandlingsaarsaktyper.ANNET
                }
            )
        }.behandling.shouldNotBeNull()

        setupTrygdeavgift(nyVurderingBehandling.id, medlemskapsperiode)

        return førstegangsbehandling
    }

    private fun lagFørstegangsbehandling(medlemskapsperiode: Periode): Behandling {
        val behandling = journalførOgVentTilProsesserErFerdige(
            defaultJournalføringDto().apply {
                fagsak.sakstype = Sakstyper.FTRL.name
                fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.name
                behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
                behandlingstemaKode = Behandlingstema.YRKESAKTIV.name
                mottattDato = medlemskapsperiode.fom?.minusDays(7)
            },
            mapOf(
                ProsessType.JFR_NY_SAK_BRUKER to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ).behandling.shouldNotBeNull()

        val mottatteOpplysninger =
            mottatteOpplysningerService.hentEllerOpprettMottatteOpplysninger(behandling.id, true)
                .shouldNotBeNull()
                .apply {
                    mottatteOpplysningerData
                        .shouldBeInstanceOf<SøknadNorgeEllerUtenforEØS>()
                        .apply {
                            periode = medlemskapsperiode
                            soeknadsland = Soeknadsland(listOf("AF"), false)
                            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
                        }
                }
        mottatteOpplysningerService.oppdaterMottatteOpplysninger(
            behandling.id,
            mottatteOpplysninger.mottatteOpplysningerData.toJsonNode
        )

        val virksomhet = AvklartefaktaDto(
            listOf("TRUE"), "VIRKSOMHET"
        ).apply {
            avklartefaktaType = Avklartefaktatyper.VIRKSOMHET
            subjektID = "999999999"
            begrunnelseKoder = emptyList()
            begrunnelseFritekst = null
        }
        avklartefaktaService.lagreAvklarteFakta(behandling.id, setOf(virksomhet))

        val vilkår = listOf(
            VilkaarDto().apply {
                vilkaar = Vilkaar.FTRL_2_1A_TRYGDEKOORDINGERING.kode
                isOppfylt = true
            },
            VilkaarDto().apply {
                vilkaar = Vilkaar.FTRL_FORUTGÅENDE_TRYGDETID.kode
                isOppfylt = true
            },
            VilkaarDto().apply {
                vilkaar = Vilkaar.FTRL_2_8_FØRSTE_LEDD_NÆR_TILKNYTNING_NORGE.kode
                isOppfylt = true
            }
        )
        vilkaarsresultatService.registrerVilkår(behandling.id, vilkår)

        setupTrygdeavgift(behandling.id, medlemskapsperiode)

        val vedtakRequest = FattVedtakRequest.Builder()
            .medBehandlingsresultatType(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medBestillersId("component test")
            .build()

        executeAndWait(
            mapOf(
                ProsessType.IVERKSETT_VEDTAK_FTRL to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ) {
            vedtaksfattingFasade.fattVedtak(behandling.id, vedtakRequest)
        }

        return behandling
    }
}

