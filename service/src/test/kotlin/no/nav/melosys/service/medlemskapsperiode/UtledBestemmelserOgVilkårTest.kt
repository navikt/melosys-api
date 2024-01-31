package no.nav.melosys.service.medlemskapsperiode

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldHaveKeys
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.maps.shouldNotHaveKeys
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import org.junit.jupiter.api.Test

class UtledBestemmelserOgVilkårTest {

    private val utledBestemmelserOgVilkår = UtledBestemmelserOgVilkår()

    @Test
    fun hentStøttede_behandlingstemaYRKESAKTIV_returnererStøttede() {
        utledBestemmelserOgVilkår.hentStøttedeBestemmelserOgVilkår(Behandlingstema.YRKESAKTIV)
            .shouldHaveKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
    }

    @Test
    fun hentStøttede_behandlingstemaIKKE_YRKESAKTIV_returnererFærre() {
        utledBestemmelserOgVilkår.hentStøttedeBestemmelserOgVilkår(Behandlingstema.IKKE_YRKESAKTIV)
            .shouldHaveKeys(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD)
    }

    @Test
    fun hentStøttede_ikkeRelevantBehandlingstema_returnererDefault() {
        utledBestemmelserOgVilkår.hentStøttedeBestemmelserOgVilkår(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY)
            .shouldHaveKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
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
    fun hentBegrunnelserForVilkår_vilkårErFTRL_2_8_NÆR_TILKNYTNING_NORGE_returnererBegrunnelser() {
        utledBestemmelserOgVilkår.hentBegrunnelserForVilkår(Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE)
            .shouldNotBeNull()
            .shouldNotBeEmpty()
            .shouldContain(Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANSATT_I_MULTINASJONALT_SELSKAP.kode)

    }

    @Test
    fun hentBegrunnelserForVilkår_vilkårErIkkeFTRL_2_8_NÆR_TILKNYTNING_NORGE_returnererTomListe() {
        utledBestemmelserOgVilkår.hentBegrunnelserForVilkår(Vilkaar.FO_883_2004_INNGANGSVILKAAR)
            .shouldNotBeNull()
            .shouldBeEmpty()
    }
}

