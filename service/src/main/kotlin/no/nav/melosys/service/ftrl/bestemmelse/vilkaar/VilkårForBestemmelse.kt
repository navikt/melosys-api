package no.nav.melosys.service.ftrl.bestemmelse.vilkaar

import io.getunleash.Unleash
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import org.springframework.stereotype.Component

@Component
class VilkårForBestemmelse(
    val vilkårForBestemmelseYrkesaktiv: VilkårForBestemmelseYrkesaktiv,
    val vilkårForBestemmelseIkkeYrkesaktiv: VilkårForBestemmelseIkkeYrkesaktiv,
    val unleash: Unleash
) {
    fun hentVilkår(
        bestemmelse: Folketrygdloven_kap2_bestemmelser,
        behandlingstema: Behandlingstema,
        avklarteFakta: Map<Avklartefaktatyper, String>,
        behandlingID: Long?
    ): List<Vilkår> {

        return when (behandlingstema) {
            Behandlingstema.IKKE_YRKESAKTIV -> vilkårForBestemmelseIkkeYrkesaktiv.hentVilkår(bestemmelse, avklarteFakta, behandlingID)
            Behandlingstema.YRKESAKTIV -> vilkårForBestemmelseYrkesaktiv.hentVilkår(bestemmelse, avklarteFakta, behandlingID)
            else -> emptyList()
        }
    }
}
