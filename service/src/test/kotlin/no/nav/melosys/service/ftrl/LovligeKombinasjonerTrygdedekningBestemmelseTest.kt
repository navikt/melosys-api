package no.nav.melosys.service.ftrl

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.service.ftrl.bestemmelse.LovligeKombinasjonerTrygdedekningBestemmelse
import no.nav.melosys.service.ftrl.medlemskapsperiode.PliktigeMedlemskapsbestemmelser
import org.junit.jupiter.api.Test

class LovligeKombinasjonerTrygdedekningBestemmelseTest {

    @Test
    fun hentLovligeBestemmelser_fullTrygdedekning_returnererAllePliktigeBestemmelseneSamtFlere() {
        LovligeKombinasjonerTrygdedekningBestemmelse.hentLovligeBestemmelser(Trygdedekninger.FULL_DEKNING_FTRL)
            .shouldNotBeNull()
            .shouldHaveSize(16)
            .shouldContainAll(PliktigeMedlemskapsbestemmelser.bestemmelser)
    }

    // Støtter toggle ToggleName.MELOSYS_FTRL_YRKESAKTIV_PLIKTIGE_BESTEMMELSER, https://jira.adeo.no/browse/MELOSYS-6430
    @Test
    fun hentLovligeBestemmelser_fullTrygdedekning_returnererAllePliktigeBestemmelseneSamtFlere_TOGGLE() {
        LovligeKombinasjonerTrygdedekningBestemmelse.hentLovligeBestemmelserToggle(Trygdedekninger.FULL_DEKNING_FTRL)
            .shouldNotBeNull()
            .shouldHaveSize(13)
            .shouldContainAll(PliktigeMedlemskapsbestemmelser.bestemmelserNy)
    }

    @Test
    fun hentLovligeTrygdedekninger_2_7ABestemmelse_returnerer2_7ATrygdedekningOgFullDekning() {
        LovligeKombinasjonerTrygdedekningBestemmelse.hentLovligeTrygdedekninger(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7A)
            .shouldNotBeNull()
            .shouldHaveSize(2)
            .shouldContainExactly(
                Trygdedekninger.FULL_DEKNING_FTRL,
                Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER
            )
    }

    @Test
    fun erGyldigKombinasjon_gyldigKombinasjon_returnererTrue() {
        LovligeKombinasjonerTrygdedekningBestemmelse.erGyldigKombinasjon(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
        ).shouldBeTrue()
    }

    @Test
    fun erGyldigKombinasjon_uGyldigKombinasjon_returnererFalse() {
        LovligeKombinasjonerTrygdedekningBestemmelse.erGyldigKombinasjon(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
            Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER
        ).shouldBeFalse()
    }

    @Test
    fun erGyldigKombinasjon_uGyldigKombinasjonPliktigBestemmelse_returnererFalse() {
        LovligeKombinasjonerTrygdedekningBestemmelse.erGyldigKombinasjon(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1_FØRSTE_LEDD,
            Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER
        ).shouldBeFalse()
    }

    @Test
    fun erBestemmelseGyldigForTrygdedekning_gyldigKombinasjon_returnererTrue() {
        LovligeKombinasjonerTrygdedekningBestemmelse.erBestemmelseGyldigForTrygdedekning(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
        ).shouldBeTrue()
    }

    @Test
    fun erBestemmelseGyldigForTrygdedekning_uGyldigKombinasjon_returnererFalse() {
        LovligeKombinasjonerTrygdedekningBestemmelse.erBestemmelseGyldigForTrygdedekning(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
            Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER
        ).shouldBeFalse()
    }

    @Test
    fun erBestemmelseGyldigForTrygdedekning_uGyldigKombinasjonMenBestemmelseErPliktig_returnererTrue() {
        LovligeKombinasjonerTrygdedekningBestemmelse.erBestemmelseGyldigForTrygdedekning(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1_FØRSTE_LEDD,
            Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER
        ).shouldBeTrue()
    }
}
