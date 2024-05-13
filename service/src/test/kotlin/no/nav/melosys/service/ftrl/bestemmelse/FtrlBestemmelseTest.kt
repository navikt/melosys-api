package no.nav.melosys.service.ftrl.bestemmelse

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FtrlBestemmelseTest {
    private lateinit var ftrlBestemmelser: FtrlBestemmelser

    @BeforeEach
    fun setUp() {
        ftrlBestemmelser = FtrlBestemmelser()
    }

    @Test
    fun hentBestemmelser_yrkesaktiv_returnererYrkesaktivListe() {
        ftrlBestemmelser.hentBestemmelser(Behandlingstema.YRKESAKTIV, null)
            .shouldNotBeNull()
            .shouldBeEqual(YrkesaktivBestemmelser.bestemmelser)
    }

    @Test
    fun hentBestemmelser_yrkesaktivFullDekning_returnererFiltrertListe() {
        ftrlBestemmelser.hentBestemmelser(Behandlingstema.YRKESAKTIV, Trygdedekninger.FULL_DEKNING_FTRL)
            .shouldNotBeNull()
            .shouldHaveSize(12)
    }

    @Test
    fun hentBestemmelser_ikkeYrkesaktivFullDekning_returnererFiltrertListe() {
        ftrlBestemmelser.hentBestemmelser(Behandlingstema.IKKE_YRKESAKTIV, Trygdedekninger.FULL_DEKNING_FTRL)
            .shouldNotBeNull()
            .shouldHaveSize(5)
    }

    @Test
    fun hentBestemmelser_yrkesaktiv2_7_ADekning_returnererFiltrertListe() {
        ftrlBestemmelser.hentBestemmelser(Behandlingstema.YRKESAKTIV, Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER)
            .shouldNotBeNull()
            .shouldHaveSize(1)
            .shouldContainExactly(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7A)
    }

    @Test
    fun hentBestemmelser_ikkeYrkesaktiv2_7_ADekning_returnererTomListe() {
        ftrlBestemmelser.hentBestemmelser(Behandlingstema.IKKE_YRKESAKTIV, Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER)
            .shouldNotBeNull()
            .shouldBeEmpty()
    }

    @Test
    fun hentBestemmelser_yrkesaktiv2_7Dekning_returnererFiltrertListe() {
        ftrlBestemmelser.hentBestemmelser(Behandlingstema.YRKESAKTIV, Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER)
            .shouldNotBeNull()
            .shouldHaveSize(1)
            .shouldContainExactly(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD)
    }

    @Test
    fun hentBestemmelser_ikkeYrkesaktiv2_7Dekning_returnererFiltrertListe() {
        ftrlBestemmelser.hentBestemmelser(Behandlingstema.IKKE_YRKESAKTIV, Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER)
            .shouldNotBeNull()
            .shouldHaveSize(2)
            .shouldContainExactly(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FJERDE_LEDD
            )
    }

    @Test
    fun hentBestemmelser_yrkesaktiv2_8Dekning_returnererFiltrertListe() {
        ftrlBestemmelser.hentBestemmelser(Behandlingstema.YRKESAKTIV, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
            .shouldNotBeNull()
            .shouldHaveSize(2)
            .shouldContainExactly(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
    }

    @Test
    fun hentBestemmelser_ikkeYrkesaktiv2_8Dekning_returnererFiltrertListe() {
        ftrlBestemmelser.hentBestemmelser(Behandlingstema.IKKE_YRKESAKTIV, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
            .shouldNotBeNull()
            .shouldHaveSize(4)
            .shouldContainExactly(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_B,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_C,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FJERDE_LEDD
            )
    }

    @Test
    fun hentBestemmelser_ustøttetBehandlingstema_returnererAlleBestemmelsene() {
        ftrlBestemmelser.hentBestemmelser(Behandlingstema.YRKESAKTIV, null)
            .shouldNotBeNull()
            .shouldNotBeEmpty()
    }
}
