package no.nav.melosys.service.medlemskapsperiode

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtledBestemmelserOgVilkårTest {

    @Test
    fun hentStøttede_behandlingstemaYRKESAKTIV_returnererStøttede() {
        assertThat(UtledBestemmelserOgVilkår().hentStøttede(Behandlingstema.YRKESAKTIV))
            .containsOnlyKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
    }

    @Test
    fun hentStøttede_behandlingstemaIKKE_YRKESAKTIV_returnererFærre() {
        assertThat(UtledBestemmelserOgVilkår().hentStøttede(Behandlingstema.IKKE_YRKESAKTIV))
            .containsOnlyKeys(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD)
    }

    @Test
    fun hentStøttede_ikkeRelevantBehandlingstema_returnererDefault() {
        assertThat(UtledBestemmelserOgVilkår().hentStøttede(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY))
            .containsOnlyKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
    }

    @Test
    fun hentIkkeStøttede_ikkeRelevantBehandlingstema_returnererIngen() {
        assertThat(UtledBestemmelserOgVilkår().hentIkkeStøttede(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY))
            .isEmpty()
    }

    @Test
    fun hentIkkeStøttede_behandlingstemaYRKESAKTIV_returnererIkkeStøttede() {
        assertThat(UtledBestemmelserOgVilkår().hentIkkeStøttede(Behandlingstema.YRKESAKTIV))
            .isNotEmpty()
            .doesNotContainKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
    }

}

