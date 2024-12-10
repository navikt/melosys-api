package no.nav.melosys.service.ftrl.bestemmelse.vilkaar

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import org.springframework.stereotype.Component

@Component
class VilkårForBestemmelse(
    val vilkårForBestemmelseYrkesaktiv: VilkårForBestemmelseYrkesaktiv,
    val vilkårForBestemmelseIkkeYrkesaktiv: VilkårForBestemmelseIkkeYrkesaktiv
) {
    fun hentVilkår(
        bestemmelse: Bestemmelse,
        behandlingstema: Behandlingstema,
        avklarteFakta: Map<Avklartefaktatyper, String>,
        behandlingID: Long?
    ): List<Vilkår> = when (behandlingstema) {
        Behandlingstema.IKKE_YRKESAKTIV -> vilkårForBestemmelseIkkeYrkesaktiv.hentVilkår(bestemmelse, avklarteFakta, behandlingID)
        Behandlingstema.YRKESAKTIV -> vilkårForBestemmelseYrkesaktiv.hentVilkår(bestemmelse, avklarteFakta, behandlingID)
        else -> emptyList()
    }
}
