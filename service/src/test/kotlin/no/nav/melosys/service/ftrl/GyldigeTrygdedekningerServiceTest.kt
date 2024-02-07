package no.nav.melosys.service.ftrl

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.behandling.BehandlingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class GyldigeTrygdedekningerServiceTest {

    @MockK
    lateinit var behandlingService: BehandlingService

    private val unleash = FakeUnleash()

    private lateinit var gyldigeTrygdedekningerService: GyldigeTrygdedekningerService

    private lateinit var behandling: Behandling
    private val BEHANDLING_ID = 1L

    @BeforeEach
    fun setUp() {
        unleash.resetAll()
        gyldigeTrygdedekningerService = GyldigeTrygdedekningerService(behandlingService, unleash)

        behandling = Behandling().apply {
            id = BEHANDLING_ID
            tema = Behandlingstema.YRKESAKTIV
            fagsak = Fagsak().apply {
                type = Sakstyper.FTRL
            }
        }
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
    }

    @Test
    fun hentTrygdedekninger_sakstypeErIkkeFTRL_kasterFeil() {
        behandling.fagsak.type = Sakstyper.EU_EOS


        shouldThrow<FunksjonellException> {
            gyldigeTrygdedekningerService.hentTrygdedekninger(BEHANDLING_ID)
        }.message.shouldContain("Behandling 1 med sakstype EU_EOS har ikke gyldige trygdedekninger")
    }

    @Test
    fun hentTrygdedekninger_temaErIkkeStøttet_kasterFeil() {
        behandling.tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL


        shouldThrow<FunksjonellException> {
            gyldigeTrygdedekningerService.hentTrygdedekninger(BEHANDLING_ID)
        }.message.shouldContain("Behandling 1 med behandlingstema ANMODNING_OM_UNNTAK_HOVEDREGEL har ikke gyldige trygdedekninger")
    }

    @Test
    fun hentTrygdedekninger_ikkeYrkesaktivMenToggleErAv_kasterFeil() {
        unleash.disableAll()
        behandling.tema = Behandlingstema.IKKE_YRKESAKTIV


        shouldThrow<FunksjonellException> {
            gyldigeTrygdedekningerService.hentTrygdedekninger(BEHANDLING_ID)
        }.message.shouldContain("Behandling 1 med behandlingstema Ikke Yrkesaktiv har ikke gyldige trygdedekninger mens toggle er slått av")
    }

    @Test
    fun hentTrygdedekninger_yrkesaktivToggleErPå_returnererKorrektListe() {
        unleash.enableAll()


        gyldigeTrygdedekningerService.hentTrygdedekninger(BEHANDLING_ID)
            .shouldNotBeNull()
            .shouldHaveSize(8)
            .shouldContainExactly(
                Trygdedekninger.FULL_DEKNING_FTRL,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
                Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER,
                Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER
            )
    }

    @Test
    fun hentTrygdedekninger_yrkesaktivToggleErAv_returnererKorrektListe() {
        unleash.disableAll()


        gyldigeTrygdedekningerService.hentTrygdedekninger(BEHANDLING_ID)
            .shouldNotBeNull()
            .shouldHaveSize(5)
            .shouldContainExactly(
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            )
    }

    @Test
    fun hentTrygdedekninger_ikkeYrkesaktiv_returnererKorrektListe() {
        unleash.enableAll()
        behandling.tema = Behandlingstema.IKKE_YRKESAKTIV

        gyldigeTrygdedekningerService.hentTrygdedekninger(BEHANDLING_ID)
            .shouldNotBeNull()
            .shouldHaveSize(7)
            .run {
                shouldContainExactly(
                    Trygdedekninger.FULL_DEKNING_FTRL,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
                    Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER
                )
                shouldNotContain(
                    Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER
                )
            }
    }
}
