package no.nav.melosys.service.medlemskapsperiode

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtledBestemmelserOgVilkaarTest {

    @Test
    fun hentStøttedeBestemmelserOgVilkår_behandlingstemaYRKESAKTIV_returnererStøttede() {
        assertThat(UtledBestemmelserOgVilkaar().hentStøttedeBestemmelserOgVilkår(Behandlingstema.YRKESAKTIV))
            .containsOnlyKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
    }

    @Test
    fun hentStøttedeBestemmelserOgVilkår_behandlingstemaIKKE_YRKESAKTIV_returnererFærre() {
        assertThat(UtledBestemmelserOgVilkaar().hentStøttedeBestemmelserOgVilkår(Behandlingstema.IKKE_YRKESAKTIV))
            .containsOnlyKeys(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD)
    }

    @Test
    fun hentStøttedeBestemmelserOgVilkår_ikkeRelevantBehandlingstema_returnererDefault() {
        assertThat(UtledBestemmelserOgVilkaar().hentStøttedeBestemmelserOgVilkår(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY))
            .containsOnlyKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
    }

    @Test
    fun hentIkkeStøttedeBestemmelserOgVilkår_ikkeRelevantBehandlingstema_returnererIngen() {
        assertThat(UtledBestemmelserOgVilkaar().hentIkkeStøttedeBestemmelserOgVilkår(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY))
            .isEmpty()
    }

    @Test
    fun hentIkkeStøttedeBestemmelserOgVilkår_behandlingstemaYRKESAKTIV_returnererIkkeStøttede() {
        assertThat(UtledBestemmelserOgVilkaar().hentIkkeStøttedeBestemmelserOgVilkår(Behandlingstema.YRKESAKTIV))
            .isNotEmpty()
            .doesNotContainKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
    }

}

