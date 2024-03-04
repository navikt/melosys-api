package no.nav.melosys.service.ftrl.bestemmelse.vilkaar

import io.getunleash.Unleash
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.featuretoggle.ToggleName
import org.springframework.stereotype.Component

@Component
class VilkårForBestemmelse(
    val vilkårForBestemmelseYrkesaktiv: VilkårForBestemmelseYrkesaktiv,
    val vilkårForBestemmelseIkkeYrkesaktiv: VilkårForBestemmelseIkkeYrkesaktiv,
    val vilkårForBestemmelseYrkesaktivNy: VilkårForBestemmelseYrkesaktivNy,
    val vilkårForBestemmelseIkkeYrkesaktivNy: VilkårForBestemmelseIkkeYrkesaktivNy,
    val unleash: Unleash
) {
    fun hentVilkår(
        bestemmelse: Folketrygdloven_kap2_bestemmelser,
        behandlingstema: Behandlingstema,
        avklarteFakta: Map<Avklartefaktatyper, String>,
        behandlingID: Long?
    ): List<Vilkår> {

        val pliktigeBestemmelserToggleAktiv = unleash.isEnabled(ToggleName.MELOSYS_FTRL_YRKESAKTIV_PLIKTIGE_BESTEMMELSER)
        if(!pliktigeBestemmelserToggleAktiv) {
            return when (behandlingstema) {
                Behandlingstema.IKKE_YRKESAKTIV -> vilkårForBestemmelseIkkeYrkesaktiv.hentVilkår(bestemmelse, avklarteFakta, behandlingID)
                Behandlingstema.YRKESAKTIV -> vilkårForBestemmelseYrkesaktiv.hentVilkår(bestemmelse, avklarteFakta, behandlingID)
                else -> emptyList()
            }
        }
        return when (behandlingstema) {
            Behandlingstema.IKKE_YRKESAKTIV -> vilkårForBestemmelseIkkeYrkesaktivNy.hentVilkår(bestemmelse, avklarteFakta, behandlingID)
            Behandlingstema.YRKESAKTIV -> vilkårForBestemmelseYrkesaktivNy.hentVilkår(bestemmelse, avklarteFakta, behandlingID)
            else -> emptyList()
        }
    }
}
