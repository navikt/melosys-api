package no.nav.melosys.service.ftrl.medlemskapsperiode

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.*
import io.kotest.matchers.maps.*
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_7_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.featuretoggle.ToggleName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UtledBestemmelserOgVilkårTest {

    val fakeUnleash = FakeUnleash()

    lateinit var utledBestemmelserOgVilkår: UtledBestemmelserOgVilkår

    @BeforeEach
    fun setUp() {
        utledBestemmelserOgVilkår = UtledBestemmelserOgVilkår(fakeUnleash)
    }

    @Test
    fun hentStøttede_behandlingstemaYRKESAKTIV_returnererStøttede() {
        utledBestemmelserOgVilkår.hentStøttedeBestemmelserOgVilkår(Behandlingstema.YRKESAKTIV).run {
            shouldHaveKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD,
            )
            shouldHaveSize(2)
        }

    }

    @Test
    fun hentStøttede_behandlingstemaYRKESAKTIV_returnererStøttedeMedToggle() {
        fakeUnleash.enable(ToggleName.MELOSYS_FOLKETRYGDEN_2_7)

        utledBestemmelserOgVilkår.hentStøttedeBestemmelserOgVilkår(Behandlingstema.YRKESAKTIV).run {
            shouldHaveKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7A
            )
            shouldHaveSize(4)
        }

    }


    @Test
    fun hentStøttede_behandlingstemaIKKE_YRKESAKTIV_returnererFærre() {
        utledBestemmelserOgVilkår.hentStøttedeBestemmelserOgVilkår(Behandlingstema.IKKE_YRKESAKTIV)
            .shouldHaveKeys(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD)
    }

    @Test
    fun hentStøttede_behandlingstemaIKKE_YRKESAKTIV_returnererFærreMedToggle() {
        fakeUnleash.enable(ToggleName.MELOSYS_FOLKETRYGDEN_2_7)

        utledBestemmelserOgVilkår.hentStøttedeBestemmelserOgVilkår(Behandlingstema.IKKE_YRKESAKTIV).run {
            shouldHaveKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD
            )
            shouldHaveSize(2)
        }
    }

    @Test
    fun hentStøttede_ikkeRelevantBehandlingstema_returnererDefault() {
        utledBestemmelserOgVilkår.hentStøttedeBestemmelserOgVilkår(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY).run {
            shouldHaveKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
            shouldHaveSize(2)
        }
    }

    @Test
    fun hentIkkeStøttede_ikkeRelevantBehandlingstema_returnererIngen() {
        utledBestemmelserOgVilkår.hentIkkeStøttedeBestemmelserOgVilkår(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY)
            .shouldBeEmpty()
    }

    @Test
    fun hentIkkeStøttede_behandlingstemaYRKESAKTIV_returnererIkkeStøttede() {
        utledBestemmelserOgVilkår.hentIkkeStøttedeBestemmelserOgVilkår(Behandlingstema.YRKESAKTIV).run {
            shouldNotBeEmpty()
            shouldNotHaveKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
        }
    }

    @Test
    fun hentIkkeStøttede_behandlingstemaYRKESAKTIV_returnererIkkeStøttedeMedToggle() {
        fakeUnleash.enableAll()
        utledBestemmelserOgVilkår.hentIkkeStøttedeBestemmelserOgVilkår(Behandlingstema.YRKESAKTIV).run {
            shouldNotBeEmpty()
            shouldNotHaveKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7A
            )
        }

    }

    @Test
    fun hentBegrunnelserForVilkår_vilkårErFTRL_2_8_NÆR_TILKNYTNING_NORGE_returnererBegrunnelser() {
        utledBestemmelserOgVilkår.hentBegrunnelserForVilkår(Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE)
            .shouldNotBeNull()
            .shouldNotBeEmpty()
            .shouldContain(Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANSATT_I_MULTINASJONALT_SELSKAP.kode)
    }


    @Test
    fun hentBegrunnelserForVilkår_vilkårErFTRL_2_7_RIMELIGHETSVURDERING_returnererBegrunnelser() {
        utledBestemmelserOgVilkår.hentBegrunnelserForVilkår(Vilkaar.FTRL_2_7_RIMELIGHETSVURDERING)
            .shouldNotBeNull()
            .shouldNotBeEmpty()
            .shouldContainAll(Ftrl_2_7_begrunnelser.values().map { it.kode })
    }

    @Test
    fun hentBegrunnelserForVilkår_vilkårErIkkeFTRL_2_8_NÆR_TILKNYTNING_NORGE_returnererTomListe() {
        utledBestemmelserOgVilkår.hentBegrunnelserForVilkår(Vilkaar.FO_883_2004_INNGANGSVILKAAR)
            .shouldNotBeNull()
            .shouldBeEmpty()
    }
}

