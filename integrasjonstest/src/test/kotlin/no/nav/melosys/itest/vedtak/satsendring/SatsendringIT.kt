package no.nav.melosys.itest.vedtak.satsendring

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.itest.MelosysHendelseKafkaConsumer
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avgift.satsendring.SatsendringFinner
import no.nav.melosys.service.avgift.satsendring.SatsendringFinner.BehandlingInfo
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.OpprettForslagMedlemskapsperiodeService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.sak.OpprettBehandlingForSak
import no.nav.melosys.service.sak.OpprettSakDto
import no.nav.melosys.service.vedtak.FattVedtakRequest
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import no.nav.melosys.service.vilkaar.VilkaarDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class SatsendringIT @Autowired constructor(
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val melosysHendelseKafkaConsumer: MelosysHendelseKafkaConsumer,
    private val opprettBehandlingForSak: OpprettBehandlingForSak,
    private val prosessinstansService: ProsessinstansService,
    private val satsendringFinner: SatsendringFinner,
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

    @AfterEach
    fun afterEach() {
        melosysHendelseKafkaConsumer.clear()
    }

    @Test
    fun `Finn satsendring etter yrkesaktiv FTRL vedtak`() {
        // Lag 1 behandling utenfor SATSENDRING_ÅR
        lagFørstegangsbehandling(år = SATSENDRING_ÅR - 1)
        // Lag 2 behandlinger for SATSENDRING_ÅR, en med satsendring og en uten
        val behandlingMedSatsendring = lagFørstegangsbehandling(harSatsendringEtterÅrsskiftet = true)
        val behandlingUtenSatsendring = lagFørstegangsbehandling(harSatsendringEtterÅrsskiftet = false)


        val avgiftSatsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(SATSENDRING_ÅR)


        avgiftSatsendringInfo.run {
            år shouldBe SATSENDRING_ÅR
            behandlingerMedSatsendring.shouldContainOnly(
                BehandlingInfo(
                    behandlingMedSatsendring.id,
                    behandlingMedSatsendring.fagsak.saksnummer,
                    Behandlingstyper.FØRSTEGANG,
                    påvirketAvSatsendring = true,
                    harAnnenAktivBehandling = false,
                    feilÅrsak = null
                )
            )
            behandlingerUtenSatsendring.shouldContainOnly(
                BehandlingInfo(
                    behandlingUtenSatsendring.id,
                    behandlingUtenSatsendring.fagsak.saksnummer,
                    Behandlingstyper.FØRSTEGANG,
                    påvirketAvSatsendring = false,
                    harAnnenAktivBehandling = false,
                    feilÅrsak = null
                )
            )
        }
    }

    @Test
    fun `Finn satsendring etter yrkesaktiv FTRL vedtak med ny vurdering`() {
        // Lag 1 behandling utenfor SATSENDRING_ÅR
        lagFørstegangsbehandling(år = SATSENDRING_ÅR - 1)
        // Lag 3 behandlinger for SATSENDRING_ÅR, en med satsendring og en uten og en ny vurdering
        val behandlingMedSatsendring = lagFørstegangsbehandling(harSatsendringEtterÅrsskiftet = true)
        lagNyVurderingBehandling(behandlingMedSatsendring)
        val behandlingUtenSatsendring = lagFørstegangsbehandling(harSatsendringEtterÅrsskiftet = false)


        val avgiftSatsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(SATSENDRING_ÅR)


        avgiftSatsendringInfo.run {
            år shouldBe SATSENDRING_ÅR
            behandlingerMedSatsendringOgBerørtAktivBehandling.shouldContainOnly(
                BehandlingInfo(
                    behandlingMedSatsendring.id,
                    behandlingMedSatsendring.fagsak.saksnummer,
                    Behandlingstyper.FØRSTEGANG,
                    påvirketAvSatsendring = true,
                    harAnnenAktivBehandling = true,
                    feilÅrsak = null
                )
            )
            behandlingerUtenSatsendring.shouldContainOnly(
                BehandlingInfo(
                    behandlingUtenSatsendring.id,
                    behandlingUtenSatsendring.fagsak.saksnummer,
                    Behandlingstyper.FØRSTEGANG,
                    påvirketAvSatsendring = false,
                    harAnnenAktivBehandling = false,
                    feilÅrsak = null
                )
            )
        }
    }

    @ParameterizedTest
    @EnumSource(value = ProsessType::class, names = ["SATSENDRING", "SATSENDRING_TILBAKESTILL_NY_VURDERING"])
    fun `oppretter prosess og påfølgende satsendringbehandling som iverksettes og sender faktura`(prosessType: ProsessType) {
        val førstegangsbehandling = lagFørstegangsbehandling(harSatsendringEtterÅrsskiftet = true)

        val nyVurderingBehandling = if (prosessType == ProsessType.SATSENDRING_TILBAKESTILL_NY_VURDERING) {
            // Opprett ny vurdering behandling med trygdeavgift for å teste at trygdeavgiftsperioder nullstilles
            lagNyVurderingBehandling(førstegangsbehandling).also {
                setupTrygdeavgift(it.id, lagPeriode(harSatsendringEtterÅrsskiftet = true))
            }
        } else null

        val satsendringID = executeAndWait(
            mapOf(
                prosessType to 1
            )
        ) {
            if (prosessType == ProsessType.SATSENDRING_TILBAKESTILL_NY_VURDERING) {
                prosessinstansService.opprettSatsendringBehandlingMedTilbakestillingAvAvgift(førstegangsbehandling, SATSENDRING_ÅR)
            } else {
                prosessinstansService.opprettSatsendringBehandlingFor(førstegangsbehandling, SATSENDRING_ÅR)
            }

        }.hentBehandling.id


        val satsendringBehandling = behandlingService.hentBehandling(satsendringID)
        // Henter behandling på nytt siden førstegangsbehandling returneres før ApplicationEvent i Avsluttfagsak (kjører async)
        val førstegangsbehandlingRefresh = behandlingService.hentBehandling(førstegangsbehandling.id)
        val satsendringBehandlingresultat = behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(satsendringID)
        val førstegangBehandlingsresultat = behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(førstegangsbehandling.id)

        satsendringBehandling.run {
            status shouldBe Behandlingsstatus.AVSLUTTET
            type shouldBe Behandlingstyper.SATSENDRING
            tema shouldBe Behandlingstema.YRKESAKTIV
            behandlingsårsak!!.type shouldBe Behandlingsaarsaktyper.ÅRLIG_SATSOPPDATERING
            oppgaveId shouldBe null
            fagsak.saksnummer shouldBe førstegangsbehandling.fagsak.saksnummer
            fagsak.status shouldBe førstegangsbehandlingRefresh.fagsak.status shouldBe Saksstatuser.LOVVALG_AVKLART
        }

        satsendringBehandlingresultat.run {
            type shouldBe Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            vedtakMetadata.vedtakstype shouldBe Vedtakstyper.ENDRINGSVEDTAK

            fakturaserieReferanse shouldBe "fakturaserieReferanse-2" // fakturaserie erstattes pga. endring
            trygdeavgiftsperioder.run {
                shouldHaveSize(1)
                first().run {
                    periodeFra.shouldNotBeNull() shouldBe førstegangBehandlingsresultat.trygdeavgiftsperioder.first().periodeFra
                    periodeTil.shouldNotBeNull() shouldBe førstegangBehandlingsresultat.trygdeavgiftsperioder.first().periodeTil
                    trygdesats shouldBe NY_SATS.toBigDecimal()
                    trygdeavgiftsbeløpMd shouldBe Penger((NY_SATS * 10000).toBigDecimal())
                }
            }

            medlemskapsperioder.run {
                shouldHaveSize(1)
                first().run {
                    fom.shouldNotBeNull() shouldBe førstegangBehandlingsresultat.medlemskapsperioder.first().fom
                    tom.shouldNotBeNull() shouldBe førstegangBehandlingsresultat.medlemskapsperioder.first().tom
                    innvilgelsesresultat shouldBe førstegangBehandlingsresultat.medlemskapsperioder.first().innvilgelsesresultat
                    medlemskapstype shouldBe førstegangBehandlingsresultat.medlemskapsperioder.first().medlemskapstype
                    trygdedekning shouldBe førstegangBehandlingsresultat.medlemskapsperioder.first().trygdedekning
                    medlPeriodeID shouldBe førstegangBehandlingsresultat.medlemskapsperioder.first().medlPeriodeID
                    bestemmelse shouldBe førstegangBehandlingsresultat.medlemskapsperioder.first().bestemmelse
                }
            }

            hentInntektsperioder().run {
                shouldHaveSize(1)
                first().run {
                    fom.shouldNotBeNull() shouldBe førstegangBehandlingsresultat.hentInntektsperioder().first().fom
                    tom.shouldNotBeNull() shouldBe førstegangBehandlingsresultat.hentInntektsperioder().first().tom
                    type shouldBe førstegangBehandlingsresultat.hentInntektsperioder().first().type
                    avgiftspliktigMndInntekt shouldBe førstegangBehandlingsresultat.hentInntektsperioder().first().avgiftspliktigMndInntekt
                    avgiftspliktigTotalinntekt shouldBe førstegangBehandlingsresultat.hentInntektsperioder().first().avgiftspliktigTotalinntekt
                }
            }

            hentSkatteforholdTilNorge().run {
                shouldHaveSize(1)
                first().run {
                    fomDato.shouldNotBeNull() shouldBe førstegangBehandlingsresultat.hentSkatteforholdTilNorge().first().fomDato
                    tomDato.shouldNotBeNull() shouldBe førstegangBehandlingsresultat.hentSkatteforholdTilNorge().first().tomDato
                    skatteplikttype shouldBe førstegangBehandlingsresultat.hentSkatteforholdTilNorge().first().skatteplikttype
                }
            }
        }

        val fakturaserieRequestJson = """
            {
                     "fodselsnummer" : "30056928150",
                     "fakturaserieReferanse" : "fakturaserieReferanse-1",
                     "fullmektig" : {
                       "fodselsnummer" : null,
                       "organisasjonsnummer" : null
                     },
                     "referanseBruker" : "Faktura for årlig satsoppdatering av trygdeavgift",
                     "referanseNAV" : "Medlemskap og avgift",
                     "fakturaGjelderInnbetalingstype" : "TRYGDEAVGIFT",
                     "intervall" : "KVARTAL",
                     "perioder" : [ {
                       "enhetsprisPerManed" : 69000.0,
                       "startDato" : "2024-04-01",
                       "sluttDato" : "2024-04-30",
                       "beskrivelse" : "Faktura for årlig satsoppdatering av trygdeavgift, Inntekt: 10000, Dekning: Pensjonsdel (§ 2-9), Sats: 6.9 %"
                     } ]
                    }
                    """.trimIndent()

        mockServer.verify(
            postRequestedFor(urlEqualTo("/fakturaserier"))
                .withRequestBody(equalToJson(fakturaserieRequestJson, true, true))
        )

        if (prosessType == ProsessType.SATSENDRING_TILBAKESTILL_NY_VURDERING) {
            nyVurderingBehandling.shouldNotBeNull()
            val nyVurderingBehandlingsresultat = behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(nyVurderingBehandling.id)
            nyVurderingBehandlingsresultat.trygdeavgiftsperioder.shouldBeEmpty()
        }
    }

    @Test
    fun `Prosess kan ikke opprette prosess på nytt for samme behandling`() {
        val behandling = Behandling.forTest { id = 3647 }
        prosessinstansService.opprettSatsendringBehandlingFor(behandling, SATSENDRING_ÅR).also {
            addCleanUpAction {
                slettProsessinstans(it)
            }
        }

        shouldThrow<FunksjonellException> { prosessinstansService.opprettSatsendringBehandlingFor(behandling, SATSENDRING_ÅR) }
            .message shouldBe "Det finnes allerede en aktiv prosess for satsendring av behandling ${behandling.id}"
    }

    private fun lagFørstegangsbehandling(år: Int = SATSENDRING_ÅR, harSatsendringEtterÅrsskiftet: Boolean = false): Behandling {
        // Perioden brukes for å avgjøre om det blir satsendring
        val medlemskapsperiode = lagPeriode(år, harSatsendringEtterÅrsskiftet)

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
        mottatteOpplysningerService.oppdaterMottatteOpplysninger(behandling.id, mottatteOpplysninger.mottatteOpplysningerData.toJsonNode)

        val virksomhet = AvklartefaktaDto(
            listOf("TRUE"), "VIRKSOMHET"
        ).apply {
            avklartefaktaType = Avklartefaktatyper.VIRKSOMHET
            subjektID = "999999999"
            begrunnelseKoder = emptyList()
            begrunnelseFritekst = null
        }
        avklartefaktaService.lagreAvklarteFakta(behandling.id, setOf(virksomhet))

        val vilkår = listOf(VilkaarDto().apply {
            vilkaar = Vilkaar.FTRL_2_1A_TRYGDEKOORDINGERING.kode
            isOppfylt = true
        }, VilkaarDto().apply {
            vilkaar = Vilkaar.FTRL_FORUTGÅENDE_TRYGDETID.kode
            isOppfylt = true
        }, VilkaarDto().apply {
            vilkaar = Vilkaar.FTRL_2_8_FØRSTE_LEDD_NÆR_TILKNYTNING_NORGE.kode
            isOppfylt = true
        })
        vilkaarsresultatService.registrerVilkår(behandling.id, vilkår)

        setupTrygdeavgift(behandling.id, medlemskapsperiode)

        val vedtakRequest = FattVedtakRequest.Builder()
            .medBehandlingsresultatType(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medBestillersId("komponent test")
            .build()

        executeAndWait(
            mapOf(
                ProsessType.IVERKSETT_VEDTAK_FTRL to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ) {
            vedtaksfattingFasade.fattVedtak(behandling.id, vedtakRequest)
        }

        return behandling.also {
            addCleanUpAction {
                slettSakMedAvhengigheter(it.fagsak.saksnummer)
            }
        }
    }

    private fun lagPeriode(
        år: Int = SATSENDRING_ÅR,
        harSatsendringEtterÅrsskiftet: Boolean = false
    ): Periode {
        if (harSatsendringEtterÅrsskiftet) {
            return Periode(LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30))
        }
        val startDato = LocalDate.of(år, 1, 1)
        val sluttDato = LocalDate.of(år, 3, 31)
        return Periode(startDato, sluttDato)
    }

    private fun lagNyVurderingBehandling(førstegangsbehandling: Behandling) =
        executeAndWait(
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

    companion object {
        private const val SATSENDRING_ÅR = 2024
        const val GAMMEL_SATS = 6.7
        const val NY_SATS = 6.9
    }
}

