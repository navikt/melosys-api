package no.nav.melosys.service.medlemskapsperiode

import io.getunleash.FakeUnleash
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_7_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.featuretoggle.ToggleName
import org.assertj.core.api.Assertions.assertThat
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
        assertThat(utledBestemmelserOgVilkår.hentStøttedeBestemmelserOgVilkår(Behandlingstema.YRKESAKTIV))
            .containsOnlyKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD,
            )
    }

    @Test
    fun hentStøttede_behandlingstemaYRKESAKTIV_returnererStøttedeMedToggle() {
        fakeUnleash.enable(ToggleName.MELOSYS_FOLKETRYGDEN_2_7)

        assertThat(utledBestemmelserOgVilkår.hentStøttedeBestemmelserOgVilkår(Behandlingstema.YRKESAKTIV))
            .containsOnlyKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7A
            )
    }


    @Test
    fun hentStøttede_behandlingstemaIKKE_YRKESAKTIV_returnererFærre() {
        assertThat(utledBestemmelserOgVilkår.hentStøttedeBestemmelserOgVilkår(Behandlingstema.IKKE_YRKESAKTIV))
            .containsOnlyKeys(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD)
    }

    @Test
    fun hentStøttede_behandlingstemaIKKE_YRKESAKTIV_returnererFærreMedToggle() {
        fakeUnleash.enable(ToggleName.MELOSYS_FOLKETRYGDEN_2_7)

        assertThat(utledBestemmelserOgVilkår.hentStøttedeBestemmelserOgVilkår(Behandlingstema.IKKE_YRKESAKTIV))
            .containsOnlyKeys(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD)
    }

    @Test
    fun hentStøttede_ikkeRelevantBehandlingstema_returnererDefault() {
        assertThat(utledBestemmelserOgVilkår.hentStøttedeBestemmelserOgVilkår(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY))
            .containsOnlyKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
    }

    @Test
    fun hentIkkeStøttede_ikkeRelevantBehandlingstema_returnererIngen() {
        assertThat(utledBestemmelserOgVilkår.hentIkkeStøttedeBestemmelserOgVilkår(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY))
            .isEmpty()
    }

    @Test
    fun hentIkkeStøttede_behandlingstemaYRKESAKTIV_returnererIkkeStøttede() {
        assertThat(utledBestemmelserOgVilkår.hentIkkeStøttedeBestemmelserOgVilkår(Behandlingstema.YRKESAKTIV))
            .isNotEmpty()
            .containsKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7A,
            )
            .doesNotContainKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
    }

    @Test
    fun hentIkkeStøttede_behandlingstemaYRKESAKTIV_returnererIkkeStøttedeMedToggle() {
        fakeUnleash.enableAll()
        assertThat(utledBestemmelserOgVilkår.hentIkkeStøttedeBestemmelserOgVilkår(Behandlingstema.YRKESAKTIV))
            .isNotEmpty()
            .doesNotContainKeys(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7A,
            )
    }

    @Test
    fun hentBegrunnelserForVilkår_vilkårErFTRL_2_8_NÆR_TILKNYTNING_NORGE_returnererBegrunnelser() {
        assertThat(utledBestemmelserOgVilkår.hentBegrunnelserForVilkår(Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE))
            .isNotNull.isNotEmpty
            .containsAll(Ftrl_2_8_naer_tilknytning_norge_begrunnelser.values().map { it.kode })
    }

    @Test
    fun hentBegrunnelserForVilkår_vilkårErFTRL_2_7_RIMELIGHETSVURDERING_returnererBegrunnelser() {
        assertThat(utledBestemmelserOgVilkår.hentBegrunnelserForVilkår(Vilkaar.FTRL_2_7_RIMELIGHETSVURDERING))
            .isNotNull.isNotEmpty
            .containsAll(Ftrl_2_7_begrunnelser.values().map { it.kode })
    }

    @Test
    fun hentBegrunnelserForVilkår_vilkårErIkkeFTRL_2_8_NÆR_TILKNYTNING_NORGE_returnererTomListe() {
        assertThat(utledBestemmelserOgVilkår.hentBegrunnelserForVilkår(Vilkaar.FO_883_2004_INNGANGSVILKAAR))
            .isNotNull.isEmpty()
    }

}

