package no.nav.melosys.service.lovligekombinasjoner

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.*
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.anmodningsperiode
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@ExtendWith(MockKExtension::class)
class LovligeKombinasjonerSaksbehandlingServiceTest {

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK(relaxed = true)
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    private val unleash = FakeUnleash()

    private lateinit var lovligeKombinasjonerSaksbehandlingService: LovligeKombinasjonerSaksbehandlingService

    @BeforeEach
    fun setup() {
        unleash.enable(ToggleName.BEHANDLINGSTYPE_KLAGE)
        lovligeKombinasjonerSaksbehandlingService =
            LovligeKombinasjonerSaksbehandlingService(fagsakService, behandlingService, behandlingsresultatService, unleash)
    }

    @Test
    fun hentMuligeSakstyper_saksnummerErNull_returnererAlleSakstyper() {
        val muligeSakstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstyper(null)


        muligeSakstyper shouldHaveSize 3
        muligeSakstyper shouldContainExactlyInAnyOrder listOf(Sakstyper.EU_EOS, Sakstyper.FTRL, Sakstyper.TRYGDEAVTALE)
    }

    @Test
    fun hentMuligeSakstyper_saksnummerIkkeNullSakKanEndres_returnererAlleSakstyper() {
        val fagsak = Fagsak.forTest {
            behandlinger(Behandling.forTest())
        }
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak


        val muligeSakstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstyper(FagsakTestFactory.SAKSNUMMER)


        muligeSakstyper shouldHaveSize 3
        muligeSakstyper shouldContainExactlyInAnyOrder listOf(Sakstyper.EU_EOS, Sakstyper.FTRL, Sakstyper.TRYGDEAVTALE)
    }

    @Test
    fun hentMuligeSakstyper_saksnummerIkkeNullSakKanIkkeEndres_returnererTomListe() {
        val behandling1 = Behandling.forTest { status = Behandlingsstatus.AVSLUTTET }
        val behandling2 = Behandling.forTest()
        val fagsak = Fagsak.forTest {
            behandlinger(listOf(behandling1, behandling2))
        }
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak


        val muligeSakstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstyper(FagsakTestFactory.SAKSNUMMER)


        muligeSakstyper.shouldBeEmpty()
    }

    @Test
    fun hentMuligeSakstemaer_saksnummerErNull_returnererLovligeSakstemaer() {
        val muligeSakstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstemaer(Aktoersroller.VIRKSOMHET, Sakstyper.TRYGDEAVTALE, null)


        muligeSakstemaer.shouldNotBeEmpty()
    }

    @Test
    fun hentMuligeSakstemaer_saksnummerErIkkeNullSakKanEndres_returnererLovligeSakstemaer() {
        val fagsak = Fagsak.forTest {
            behandlinger(Behandling.forTest())
        }
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak


        val muligeSakstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstemaer(
            Aktoersroller.VIRKSOMHET,
            Sakstyper.TRYGDEAVTALE,
            FagsakTestFactory.SAKSNUMMER
        )


        muligeSakstemaer.shouldNotBeEmpty()
    }

    @Test
    fun hentMuligeSakstemaer_saksnummerErIkkeNullSakKanIkkeEndres_returnererTomListe() {
        val behandling1 = Behandling.forTest { status = Behandlingsstatus.AVSLUTTET }
        val behandling2 = Behandling.forTest()
        val fagsak = Fagsak.forTest {
            behandlinger(listOf(behandling1, behandling2))
        }
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak


        val muligeSakstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstemaer(
            Aktoersroller.VIRKSOMHET,
            Sakstyper.TRYGDEAVTALE,
            FagsakTestFactory.SAKSNUMMER
        )


        muligeSakstemaer.shouldBeEmpty()
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak_FTRLYrkesaktivlovvalgMedlemsskap_returnererLovligKombinasjon TOGGLE ÅRSAVREGNING`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING)

        val behandling = sisteBehandling(Behandlingstyper.FØRSTEGANG) {
            fagsak {
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                type = Sakstyper.FTRL
            }
        }
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.YRKESAKTIV
        )

        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.KLAGE,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.ÅRSAVREGNING,
        )
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak_FTRLYrkesaktivTrygdeavgift_returnererLovligKombinasjon TOGGLE ÅRSAVREGNING`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING)

        val behandling = sisteBehandling(Behandlingstyper.FØRSTEGANG) {
            fagsak {
                tema = Sakstemaer.TRYGDEAVGIFT
                type = Sakstyper.FTRL
            }
        }
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.YRKESAKTIV
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.KLAGE,
            Behandlingstyper.HENVENDELSE,
        )
    }

    @Test
    fun `hentMuligeBehandlingstyper_temaArbeidThenestepersonEllerFly_returnererLovligKombinasjon TOGGLE ÅRSAVREGNING`() {

        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE,
        )
    }

    @Test
    fun `hentMuligeBehandlingstyper_temaArbeidThenestepersonEllerFly_returnererLovligKombinasjon TOGGLE ÅRSAVREGNING_UTEN_FLYT`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING_UTEN_FLYT)

        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE,
        )
    }

    @Test
    fun hentMuligeBehandlingstyper_EU_EOS_MEDLEMSKAP_LOVVALG_temaIkkeYrkesAktiv_returnererLovligKombinasjon() {
        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.IKKE_YRKESAKTIV
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE
        )
    }

    @Test
    fun hentMuligeBehandlingstyper_EU_EOS_UNNTAK_temaForesporselTrygdemyndighet_returnererLovligKombinasjon() {
        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.EU_EOS,
            Sakstemaer.UNNTAK,
            Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(Behandlingstyper.HENVENDELSE)
    }

    @Test
    fun hentMuligeBehandlingstyper_EU_EOS_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.EU_EOS,
            Sakstemaer.TRYGDEAVGIFT,
            Behandlingstema.YRKESAKTIV
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE
        )
    }

    @Test
    fun hentMuligeBehandlingstyper_EU_EOS_TRYGDEAVGIFT_PENSJONIST_returnererLovligKombinasjon() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING)
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING_EØS_PENSJONIST)

        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.EU_EOS,
            Sakstemaer.TRYGDEAVGIFT,
            Behandlingstema.PENSJONIST
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE,
            Behandlingstyper.ÅRSAVREGNING
        )
    }

    @Test
    fun `hentMuligeBehandlingstyper_FTRL_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_ikkeIKnyttTilSakKontekst_returnererLovligKombinasjon TOGGLE ÅRSAVREGNING`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING)

        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE,
            Behandlingstyper.ÅRSAVREGNING
        )
    }

    @Test
    fun `hentMuligeBehandlingstyper_FTRL_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_ikkeIKnyttTilSakKontekst_returnererLovligKombinasjon TOGGLE ÅRSAVREGNING_UTEN_FLYT`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING_UTEN_FLYT)

        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE,
            Behandlingstyper.ÅRSAVREGNING
        )
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak_FTRL_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_iKnyttTilSakKontekstOgSakHarBehandlingMedManglendeInnbetaling_returnererLovligKombinasjon TOGGLE ÅRSAVREGNING`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING)

        val behandling = sisteBehandling(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT) {
            fagsak {
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                type = Sakstyper.FTRL
            }
        }
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.YRKESAKTIV
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE,
            Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT,
            Behandlingstyper.ÅRSAVREGNING
        )
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak_FTRL_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_iKnyttTilSakKontekstOgSakHarBehandlingMedManglendeInnbetaling_returnererLovligKombinasjon TOGGLE ÅRSAVREGNING_UTEN_FLYT`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING_UTEN_FLYT)

        val behandling = sisteBehandling(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT) {
            fagsak {
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                type = Sakstyper.FTRL
            }
        }
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.YRKESAKTIV
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE,
            Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT,
            Behandlingstyper.ÅRSAVREGNING
        )
    }


    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak_FTRL_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_iKnyttTilSakKontekstMenSakHarIkkeBehandlingMedManglendeInnbetaling_returnererLovligKombinasjon TOGGLE ÅRSAVREGNING`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING)

        val behandling = sisteBehandling(Behandlingstyper.FØRSTEGANG) {
            fagsak {
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                type = Sakstyper.FTRL
            }
        }
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.YRKESAKTIV
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE,
            Behandlingstyper.ÅRSAVREGNING
        )
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak_FTRL_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_iKnyttTilSakKontekstMenSakHarIkkeBehandlingMedManglendeInnbetaling_returnererLovligKombinasjon ÅRSAVREGNING_UTEN_FLYT`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING_UTEN_FLYT)

        val behandling = sisteBehandling(Behandlingstyper.FØRSTEGANG) {
            fagsak {
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                type = Sakstyper.FTRL
            }
        }
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.YRKESAKTIV
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE,
            Behandlingstyper.ÅRSAVREGNING
        )
    }


    @Test
    fun `hentMuligeBehandlingstyper_FTRL_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_returnererLovligKombinasjon TOGGLE ÅRSAVREGNING`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING)

        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE,
            Behandlingstyper.ÅRSAVREGNING
        )
    }


    @Test
    fun `hentMuligeBehandlingstyper_FTRL_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_returnererLovligKombinasjon TOGGLE ÅRSAVREGNING_UTEN_FLYT`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING_UTEN_FLYT)

        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE,
            Behandlingstyper.ÅRSAVREGNING
        )
    }

    @Test
    fun hentMuligeBehandlingstyper_FTRL_LOVVALG_MEDLEMSKAP_temaPensjonist_returnererLovligKombinasjon() {
        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.PENSJONIST
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE
        )
    }

    @Test
    fun hentMuligeBehandlingstyper_FTRL_TRYGDEAVGIFT_temaPensjonist_returnererLovligKombinasjon() {
        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.FTRL,
            Sakstemaer.TRYGDEAVGIFT,
            Behandlingstema.PENSJONIST
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE
        )
    }

    @Test
    fun hentMuligeBehandlingstyper_FTRL_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.FTRL,
            Sakstemaer.TRYGDEAVGIFT,
            Behandlingstema.YRKESAKTIV
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE
        )
    }

    @Test
    fun `hentMuligeBehandlingstyper_TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_returnererLovligKombinasjon TOGGLE ÅRSAVREGNING`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING)

        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE,
        )
    }

    @Test
    fun `hentMuligeBehandlingstyper_TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_returnererLovligKombinasjon TOGGLE ÅRSAVREGNING UTEN FLYT`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING_UTEN_FLYT)

        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE
        )
    }

    @Test
    fun hentMuligeBehandlingstyper_TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_temaPensjonist_returnererLovligKombinasjon() {
        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.PENSJONIST
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE
        )
    }

    @Test
    fun hentMuligeBehandlingstyper_TRYGDEAVTALE_UNNTAK_temaForesporselTrygdemyndighet_returnererLovligKombinasjon() {
        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.UNNTAK,
            Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(Behandlingstyper.HENVENDELSE)
    }

    @Test
    fun hentMuligeBehandlingstyper_TRYGDEAVTALE_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.TRYGDEAVGIFT,
            Behandlingstema.YRKESAKTIV
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE
        )
    }

    @Test
    fun hentMuligeBehandlingstyper_TRYGDEAVTALE_TRYGDEAVGIFT_temaPensjonist_returnererLovligKombinasjon() {
        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.TRYGDEAVGIFT,
            Behandlingstema.PENSJONIST
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE
        )
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak_avsluttet_returnererIkkeFørstegang TOGGLE ÅRSAVREGNING`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING)

        val behandling = behandlingMedTemaOgType(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstyper.FØRSTEGANG) {
            status = Behandlingsstatus.AVSLUTTET
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            }
        }
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.UTSENDT_ARBEIDSTAKER
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE,
        )
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak_avsluttet_returnererIkkeFørstegang TOGGLE ÅRSAVREGNING_UTEN_FLYT`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING_UTEN_FLYT)

        val behandling = behandlingMedTemaOgType(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstyper.FØRSTEGANG) {
            status = Behandlingsstatus.AVSLUTTET
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            }
        }
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.UTSENDT_ARBEIDSTAKER
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE,
        )
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak_midlertidigLovvalgsbesluttet_returnererIkkeFørstegang TOGGLE ÅRSAVREGNING`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING)

        val behandling = behandlingMedTemaOgType(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstyper.FØRSTEGANG) {
            status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
        }
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.UTSENDT_ARBEIDSTAKER
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE,
        )
        muligeTyper shouldNotContain Behandlingstyper.FØRSTEGANG
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak_midlertidigLovvalgsbesluttet_returnererIkkeFørstegang TOGGLE ÅRSAVREGNING_UTEN_FLYT`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING_UTEN_FLYT)

        val behandling = behandlingMedTemaOgType(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstyper.FØRSTEGANG) {
            status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
        }
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.UTSENDT_ARBEIDSTAKER
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.KLAGE
        )
        muligeTyper shouldNotContain Behandlingstyper.FØRSTEGANG
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak EU_EØS TRYGDEAVGIFT tema Pensjonist returnerer Årsavregning TOGGLE ÅRSAVREGNING_EØS_PENSJONIST`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING_EØS_PENSJONIST)

        val behandling = behandlingMedTemaOgType(Behandlingstema.PENSJONIST, Behandlingstyper.FØRSTEGANG) {
            status = Behandlingsstatus.UNDER_BEHANDLING
            fagsak {
                tema = Sakstemaer.TRYGDEAVGIFT
            }
        }
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.PENSJONIST
        )


        muligeTyper shouldContainExactlyInAnyOrder listOf(
            Behandlingstyper.ÅRSAVREGNING,
        )
    }

    @Test
    fun hentMuligeBehandlingstemaer_hovedpartVIRKSOMHETIkkeTrygdeavgift_skalReturnereBehandlingsTemaVIRKSOMHET() {
        val behandlingstemas = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(
            Aktoersroller.VIRKSOMHET,
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            null,
            null
        )
        behandlingstemas shouldContainExactlyInAnyOrder listOf(Behandlingstema.VIRKSOMHET)
    }

    @Test
    fun hentMuligeBehandlingstemaer_hovedpartVIRKSOMHETTrygdeavgift_skalReturnereBehandlingsTemaYRKESAKTIV() {
        val behandlingstemas = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(
            Aktoersroller.VIRKSOMHET,
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.TRYGDEAVGIFT,
            null,
            null
        )
        behandlingstemas shouldContainExactlyInAnyOrder listOf(Behandlingstema.YRKESAKTIV)
    }

    @Test
    fun hentMuligeBehandlingstemaer_ingenHovedpart_skalReturnereSammeSomHovedpartVIRKSOMHETogBRUKER() {
        val behandlingstemaer =
            lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(null, Sakstyper.TRYGDEAVTALE, Sakstemaer.TRYGDEAVGIFT, null, null)

        val behandlingstemaerVirksomhet = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(
            Aktoersroller.VIRKSOMHET,
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.TRYGDEAVGIFT,
            null,
            null
        )
        val behandlingstemaerBruker = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(
            Aktoersroller.BRUKER,
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.TRYGDEAVGIFT,
            null,
            null
        )

        behandlingstemaer shouldContainAll behandlingstemaerVirksomhet
        behandlingstemaer shouldContainAll behandlingstemaerBruker
    }

    @Test
    fun hentMuligeBehandlingstemaer_ingenHovedpartMedlemskapLovvalg_skalReturnereSedBehandlingstema() {
        val behandlingstemaer =
            lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(null, Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, null, null)
        behandlingstemaer shouldContain Behandlingstema.BESLUTNING_LOVVALG_NORGE
    }

    @Test
    fun hentMuligeBehandlingstemaer_ingenHovedpartUnntak_skalReturnereSedBehandlingstema() {
        val behandlingstemaer =
            lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(null, Sakstyper.EU_EOS, Sakstemaer.UNNTAK, null, null)
        behandlingstemaer shouldContainAll listOf(
            Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
            Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
            Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        )
    }

    @Test
    fun hentMuligeBehandlingstemaer_ingenHovedpartTrygdeavgift_skalReturnereIngenSedBehandlingstema() {
        val behandlingstemaer =
            lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(null, Sakstyper.EU_EOS, Sakstemaer.TRYGDEAVGIFT, null, null)
        behandlingstemaer shouldNotContainAll listOf(
            Behandlingstema.BESLUTNING_LOVVALG_NORGE,
            Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
            Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
            Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        )
    }

    @Test
    fun hentMuligeBehandlingstemaer_EuEosUnntak_skalReturnereA1AnmodningOmUnntakPapir() {
        val behandlingstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(
            Aktoersroller.BRUKER,
            Sakstyper.EU_EOS,
            Sakstemaer.UNNTAK,
            null,
            null
        )
        behandlingstemaer shouldContainExactlyInAnyOrder listOf(
            Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR,
            Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET
        )
    }

    @Test
    fun hentMuligeBehandlingstemaer_manglendeInnbetalingTrygdeavgift_skalKunReturnereEksisterendeBehTema() {
        val aktivBehandling = Behandling.forTest {
            tema = Behandlingstema.YRKESAKTIV
            type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        }
        every { behandlingService.hentBehandling(1L) } returns aktivBehandling

        val behandlingstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(
            Aktoersroller.BRUKER,
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            1L,
            null
        )
        behandlingstemaer shouldContainExactlyInAnyOrder listOf(Behandlingstema.YRKESAKTIV)
    }

    @Test
    fun hentMuligeBehandlingstemaer_sistBehandlingstemaErSedTema_skalKunReturnereSistBehandlingstema() {
        val behandlingstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(
            Aktoersroller.BRUKER,
            Sakstyper.EU_EOS,
            Sakstemaer.UNNTAK,
            null,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        )
        behandlingstemaer shouldContainExactlyInAnyOrder listOf(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak_sisteBehandlingFinnes_skalIkkeReturnereFørstegangsbehandling TOGGLE ÅRSAVREGNING`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING)

        val behandling = behandlingMedTemaOgType(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstyper.FØRSTEGANG) {
            status = Behandlingsstatus.AVSLUTTET
            fagsak { type = Sakstyper.EU_EOS }
        }
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.UTSENDT_ARBEIDSTAKER
        )


        muligeBehandlingstyper shouldContainExactly listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.KLAGE,
            Behandlingstyper.HENVENDELSE,
        )
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak_sisteBehandlingFinnes_skalIkkeReturnereFørstegangsbehandling TOGGLE ÅRSAVREGNING_UTEN_FLYT`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING_UTEN_FLYT)

        val behandling = behandlingMedTemaOgType(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstyper.FØRSTEGANG) {
            status = Behandlingsstatus.AVSLUTTET
        }
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.UTSENDT_ARBEIDSTAKER
        )


        muligeBehandlingstyper shouldContainExactly listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.KLAGE,
            Behandlingstyper.HENVENDELSE
        )
    }

    @Test
    fun hentMuligeBehandlingstyper_unntakA1AnmodningOmUnntakPåPapir_skalReturnereBehTypeFØRSTEGANG_NY_VURDERING_KLAGE() {
        val muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(
            Aktoersroller.BRUKER,
            Sakstyper.EU_EOS,
            Sakstemaer.UNNTAK,
            Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR
        )


        muligeBehandlingstyper shouldHaveSize 4
        muligeBehandlingstyper shouldContainExactly listOf(
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.KLAGE,
            Behandlingstyper.HENVENDELSE
        )
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak returnerer ÅRSAVREGNING dersom behandlingstema er tillatt TOGGLE ÅRSAVREGNING`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING)

        val behandling = Behandling.forTest {
            id = 1L
            tema = Behandlingstema.YRKESAKTIV
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.UNDER_BEHANDLING
            fagsak { type = Sakstyper.FTRL }
        }
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
        )


        muligeBehandlingstyper shouldHaveSize 1
        muligeBehandlingstyper shouldContainExactly listOf(Behandlingstyper.ÅRSAVREGNING)
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak returnerer ÅRSAVREGNING dersom behandlingstema er tillatt TOGGLE ÅRSAVREGNING_UTEN_FLYT`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING_UTEN_FLYT)

        val behandling = Behandling.forTest {
            id = 1L
            tema = Behandlingstema.YRKESAKTIV
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.UNDER_BEHANDLING
            fagsak { type = Sakstyper.FTRL }
        }
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
        )


        muligeBehandlingstyper shouldHaveSize 1
        muligeBehandlingstyper shouldContainExactly listOf(Behandlingstyper.ÅRSAVREGNING)
    }


    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak returnerer ÅRSAVREGNING dersom behandlingstema er tillatt EU_EOS TOGGLE ÅRSAVREGNING`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING)

        val behandling = Behandling.forTest {
            id = 1L
            tema = Behandlingstema.ARBEID_FLERE_LAND
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.UNDER_BEHANDLING
            fagsak { type = Sakstyper.EU_EOS }
        }
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
        )


        muligeBehandlingstyper shouldHaveSize 0
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak returnerer IKKE ÅRSAVREGNING dersom sakstype er EU_EOS TOGGLE ÅRSAVREGNING_UTEN_FLYT`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING_UTEN_FLYT)

        val behandling = Behandling.forTest {
            id = 1L
            tema = Behandlingstema.ARBEID_FLERE_LAND
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.UNDER_BEHANDLING
            fagsak { type = Sakstyper.EU_EOS }
        }
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
        )


        muligeBehandlingstyper shouldHaveSize 0
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak med avsluttet FØRSTEGANG og åpen ÅRSAVREGNING skal ikke ta hensyn til ÅRSAVREGNING TOGGLE ÅRSAVREGNING`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING)

        val fagsak = Fagsak.forTest {
            type = Sakstyper.FTRL
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            behandling {
                id = 1L
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
            }
            behandling {
                id = 2L
                tema = Behandlingstema.ARBEID_FLERE_LAND
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.UNDER_BEHANDLING
            }
        }
        every { fagsakService.hentFagsak(fagsak.saksnummer) } returns fagsak


        val muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            fagsak.saksnummer,
            Behandlingstema.YRKESAKTIV,
        )


        muligeBehandlingstyper shouldHaveSize 4
        muligeBehandlingstyper shouldContainExactly listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.KLAGE,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.ÅRSAVREGNING
        )
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak med avsluttet FØRSTEGANG og åpen ÅRSAVREGNING skal ikke ta hensyn til ÅRSAVREGNING TOGGLE ÅRSAVREGNING_UTEN_FLYT`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING_UTEN_FLYT)

        val fagsak = Fagsak.forTest {
            type = Sakstyper.FTRL
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            behandling {
                id = 1L
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
            }
            behandling {
                id = 2L
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.UNDER_BEHANDLING
            }
        }
        every { fagsakService.hentFagsak(fagsak.saksnummer) } returns fagsak


        val muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            fagsak.saksnummer,
            Behandlingstema.YRKESAKTIV,
        )


        muligeBehandlingstyper shouldHaveSize 4
        muligeBehandlingstyper shouldContainExactly listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.KLAGE,
            Behandlingstyper.HENVENDELSE,
            Behandlingstyper.ÅRSAVREGNING
        )
    }

    @ParameterizedTest
    @EnumSource(value = Sakstyper::class, names = ["FTRL", "EU_EOS"])
    fun `hentMuligeBehandlingstyperForKnyttTilSak med avsluttet ÅRSAVREGNING skal kun returnere ny årsavregning`(sakstype: Sakstyper) {
        val fagsak = Fagsak.forTest {
            type = sakstype
            behandling {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
            }
        }
        every { fagsakService.hentFagsak(fagsak.saksnummer) } returns fagsak


        val muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            fagsak.saksnummer,
            null
        )


        muligeBehandlingstyper shouldHaveSize 1
        muligeBehandlingstyper shouldContainExactly listOf(
            Behandlingstyper.ÅRSAVREGNING
        )
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak med avsluttet FØRSTEGANG og åpen NY_VURDERING og lukket ÅRSAVREGNING skal kun returnere ÅRSAVREGNING TOGGLE ÅRSAVREGNING`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING)

        val fagsak = Fagsak.forTest {
            type = Sakstyper.FTRL
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            behandling {
                id = 1L
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
            }
            behandling {
                id = 2L
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.UNDER_BEHANDLING
            }
            behandling {
                id = 3L
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
            }
        }
        every { fagsakService.hentFagsak(fagsak.saksnummer) } returns fagsak


        val muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            fagsak.saksnummer,
            Behandlingstema.YRKESAKTIV,
        )


        muligeBehandlingstyper shouldHaveSize 1
        muligeBehandlingstyper shouldContainExactly listOf(Behandlingstyper.ÅRSAVREGNING)
    }

    @Test
    fun `hentMuligeBehandlingstyperForKnyttTilSak med avsluttet FØRSTEGANG og åpen NY_VURDERING og lukket ÅRSAVREGNING skal kun returnere ÅRSAVREGNING TOGGLE ÅRSAVREGNING_UTEN_FLYT`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING_UTEN_FLYT)

        val fagsak = Fagsak.forTest {
            type = Sakstyper.FTRL
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            behandling {
                id = 1L
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
            }
            behandling {
                id = 2L
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.UNDER_BEHANDLING
            }
            behandling {
                id = 3L
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
            }
        }
        every { fagsakService.hentFagsak(fagsak.saksnummer) } returns fagsak


        val muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            fagsak.saksnummer,
            Behandlingstema.YRKESAKTIV,
        )


        muligeBehandlingstyper shouldHaveSize 1
        muligeBehandlingstyper shouldContainExactly listOf(Behandlingstyper.ÅRSAVREGNING)
    }

    @Test
    fun hentMuligeBehandlingstyperForKnyttTilSak_sisteBehandlingAktiv_skalReturnereTomListe() {
        val behandling = Behandling.forTest {
            id = 1L
            tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.UNDER_BEHANDLING
        }
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
        )


        muligeBehandlingstyper.shouldBeEmpty()
    }

    @Test
    fun hentMuligeBehandlingstyperForKnyttTilSak_sisteBehandlingAktivOgAnmodningsperiodeSendt_skalReturnereKunNyVurdering() {
        val behandling = Behandling.forTest {
            id = 1L
            tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.UNDER_BEHANDLING
        }
        val sisteBehandlingsresultat = Behandlingsresultat.forTest {
            anmodningsperiode {
                sendtUtland = true
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultatMedAnmodningsperioder(behandling.id) } returns sisteBehandlingsresultat
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            behandling.fagsak.saksnummer,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
        )


        muligeBehandlingstyper shouldHaveSize 1
        muligeBehandlingstyper shouldContainExactly listOf(Behandlingstyper.NY_VURDERING)
    }

    @Test
    fun `hentMuligeBehandlingstyperForEndring_aktivBehandlingSomErFørst_skalReturnereAlleBehandlingstyper TOGGLE ÅRSAVREGNING`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING)

        val behandling = Behandling.forTest {
            id = 1L
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.UNDER_BEHANDLING
        }
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForEndring(
            Aktoersroller.BRUKER,
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
            behandling.fagsak.saksnummer
        )


        muligeBehandlingstyper shouldContainExactly listOf(
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.KLAGE,
            Behandlingstyper.HENVENDELSE,
        )
    }

    @Test
    fun `hentMuligeBehandlingstyperForEndring_aktivBehandlingSomErFørst_skalReturnereAlleBehandlingstyper TOGGLE ÅRSAVREGNING_UTEN_FLYT`() {
        unleash.enable(ToggleName.MELOSYS_ÅRSAVREGNING_UTEN_FLYT)

        val behandling = Behandling.forTest {
            id = 1L
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.UNDER_BEHANDLING
            fagsak { type = Sakstyper.FTRL }
        }
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsak(behandling.fagsak.saksnummer) } returns behandling.fagsak


        val muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForEndring(
            Aktoersroller.BRUKER,
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
            behandling.fagsak.saksnummer
        )


        muligeBehandlingstyper shouldContainExactly listOf(
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.KLAGE,
            Behandlingstyper.HENVENDELSE,
        )
    }

    @Test
    fun hentMuligeBehandlingstyperForKnyttTilSak_aktivBehandlingSomIkkeErFørst_skalIkkeHaFørstegang() {
        val fagsak = Fagsak.forTest {
            behandling {
                id = 1L
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
            }
            behandling {
                id = 2L
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.UNDER_BEHANDLING
            }
        }
        val aktivBehandling = fagsak.behandlinger.find { it.id == 2L }!!
        every { behandlingService.hentBehandling(aktivBehandling.id) } returns aktivBehandling
        every { fagsakService.hentFagsak(fagsak.saksnummer) } returns fagsak


        val muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
            Aktoersroller.BRUKER,
            fagsak.saksnummer,
            Behandlingstema.UTSENDT_ARBEIDSTAKER
        )


        muligeBehandlingstyper shouldNotContain Behandlingstyper.FØRSTEGANG
    }

    @Test
    fun hentMuligeBehandlingsårsaktyper_behandlingstypeErManglendeInnbetalingTrygdeavgift() {
        val typer = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingsårsaktyper(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT)
        typer shouldContainExactly listOf(Behandlingsaarsaktyper.MELDING_OM_MANGLENDE_INNBETALING)
    }

    @Test
    fun hentMuligeBehandlingsårsaktyper_behandlingstypeErAnnetEnnManglendeInnbetalingTrygdeavgift() {
        val typer = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingsårsaktyper(Behandlingstyper.FØRSTEGANG)
        typer shouldContainExactly listOf(
            Behandlingsaarsaktyper.SØKNAD,
            Behandlingsaarsaktyper.SED,
            Behandlingsaarsaktyper.HENVENDELSE,
            Behandlingsaarsaktyper.FRITEKST
        )
    }

    @Test
    fun hentMuligeBehandlingStatuser_harNoyaktigRekkefolgePaMuligeStatus() {
        val muligeStatuser = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingStatuser()
        muligeStatuser shouldContainExactly listOf(
            Behandlingsstatus.UNDER_BEHANDLING,
            Behandlingsstatus.AVVENT_DOK_PART,
            Behandlingsstatus.AVVENT_DOK_UTL,
            Behandlingsstatus.AVVENT_FAGLIG_AVKLARING
        )
    }

    @Test
    fun hentMuligeBehandlingStatuser_avsluttetErIkkeMulig() {
        val muligeStatuser = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingStatuser()
        muligeStatuser shouldContainExactlyInAnyOrder listOf(
            Behandlingsstatus.UNDER_BEHANDLING,
            Behandlingsstatus.AVVENT_DOK_PART,
            Behandlingsstatus.AVVENT_DOK_UTL,
            Behandlingsstatus.AVVENT_FAGLIG_AVKLARING
        )
        muligeStatuser shouldNotContain Behandlingsstatus.AVSLUTTET
    }

    /**
     * Creates a behandling with YRKESAKTIV tema, AVSLUTTET status, and the specified behandlingstype.
     * The behandling is properly linked to its fagsak with bidirectional relationship.
     */
    private fun sisteBehandling(
        behandlingstype: Behandlingstyper,
        init: BehandlingTestFactory.BehandlingTestBuilder.() -> Unit = {}
    ): Behandling {
        return Behandling.forTest {
            id = 1L
            tema = Behandlingstema.YRKESAKTIV
            type = behandlingstype
            status = Behandlingsstatus.AVSLUTTET
            init()
        }
    }

    /**
     * Creates a behandling with specified tema and type.
     */
    private fun behandlingMedTemaOgType(
        tema: Behandlingstema,
        type: Behandlingstyper,
        init: BehandlingTestFactory.BehandlingTestBuilder.() -> Unit = {}
    ): Behandling {
        return Behandling.forTest {
            this.tema = tema
            this.type = type
            init()
        }
    }
}
