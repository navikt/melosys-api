package no.nav.melosys.service.medlemskapsperiode

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtledBestemmelserOgVilkårTest {

    @Test
    fun hentStøttede_behandlingstemaYRKESAKTIV_returnererStøttede() {
        assertThat(UtledBestemmelserOgVilkår().hentStøttedeBestemmelserOgVilkår(Behandlingstema.YRKESAKTIV))
            .containsOnlyKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
    }

    @Test
    fun hentStøttede_behandlingstemaIKKE_YRKESAKTIV_returnererFærre() {
        assertThat(UtledBestemmelserOgVilkår().hentStøttedeBestemmelserOgVilkår(Behandlingstema.IKKE_YRKESAKTIV))
            .containsOnlyKeys(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD)
    }

    @Test
    fun hentStøttede_ikkeRelevantBehandlingstema_returnererDefault() {
        assertThat(UtledBestemmelserOgVilkår().hentStøttedeBestemmelserOgVilkår(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY))
            .containsOnlyKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
    }

    @Test
    fun hentIkkeStøttede_ikkeRelevantBehandlingstema_returnererIngen() {
        assertThat(UtledBestemmelserOgVilkår().hentIkkeStøttedeBestemmelserOgVilkår(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY))
            .isEmpty()
    }

    @Test
    fun hentIkkeStøttede_behandlingstemaYRKESAKTIV_returnererIkkeStøttede() {
        assertThat(UtledBestemmelserOgVilkår().hentIkkeStøttedeBestemmelserOgVilkår(Behandlingstema.YRKESAKTIV))
            .isNotEmpty()
            .doesNotContainKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
    }

    @Test
    fun hentBegrunnelserForVilkår_vilkårErFTRL_2_8_NÆR_TILKNYTNING_NORGE_returnererBegrunnelser() {
        assertThat(UtledBestemmelserOgVilkår().hentBegrunnelserForVilkår(Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE))
            .isNotNull.isNotEmpty
            .contains(Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANSATT_I_MULTINASJONALT_SELSKAP.kode)
    }

    @Test
    fun hentBegrunnelserForVilkår_vilkårErIkkeFTRL_2_8_NÆR_TILKNYTNING_NORGE_returnererTomListe() {
        assertThat(UtledBestemmelserOgVilkår().hentBegrunnelserForVilkår(Vilkaar.FO_883_2004_INNGANGSVILKAAR))
            .isNotNull.isEmpty()
    }

}

