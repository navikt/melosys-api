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
    private val unleash = FakeUnleash()

    private lateinit var gyldigeTrygdedekningerService: GyldigeTrygdedekningerService

    @BeforeEach
    fun setUp() {
        unleash.resetAll()
        gyldigeTrygdedekningerService = GyldigeTrygdedekningerService(unleash)
    }

    @Test
    fun hentTrygdedekninger_temaErIkkeStøttet_kasterFeil() {
        shouldThrow<FunksjonellException> {
            gyldigeTrygdedekningerService.hentTrygdedekninger(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)
        }.message.shouldContain("Behandling med behandlingstema ANMODNING_OM_UNNTAK_HOVEDREGEL har ikke gyldige trygdedekninger")
    }

    @Test
    fun hentTrygdedekninger_ikkeYrkesaktivMenToggleErAv_kasterFeil() {
        unleash.disableAll()


        shouldThrow<FunksjonellException> {
            gyldigeTrygdedekningerService.hentTrygdedekninger(Behandlingstema.IKKE_YRKESAKTIV)
        }.message.shouldContain("Behandling med behandlingstema Ikke Yrkesaktiv har ikke gyldige trygdedekninger mens toggle er slått av")
    }

    @Test
    fun hentTrygdedekninger_yrkesaktivToggleErPå_returnererKorrektListe() {
        unleash.enableAll()


        gyldigeTrygdedekningerService.hentTrygdedekninger(Behandlingstema.YRKESAKTIV)
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


        gyldigeTrygdedekningerService.hentTrygdedekninger(Behandlingstema.YRKESAKTIV)
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


        gyldigeTrygdedekningerService.hentTrygdedekninger(Behandlingstema.IKKE_YRKESAKTIV)
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
