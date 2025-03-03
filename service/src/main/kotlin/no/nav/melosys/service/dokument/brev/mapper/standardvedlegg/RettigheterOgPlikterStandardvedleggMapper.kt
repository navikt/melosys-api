package no.nav.melosys.service.dokument.brev.mapper.standardvedlegg

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.integrasjon.dokgen.dto.standardvedlegg.InnvilgelseRettigheterPlikterStandardvedlegg
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RettigheterOgPlikterStandardvedleggMapper(
    private val behandlingsresultatService: BehandlingsresultatService
) {

    @Transactional(readOnly = true)
    fun mapInnvilgelse(behandlingId: Long, skalMappeBestemmelse: Boolean): InnvilgelseRettigheterPlikterStandardvedlegg {
        if (!skalMappeBestemmelse) {
            return InnvilgelseRettigheterPlikterStandardvedlegg()
        }
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId)
        val bestemmelse = mapBestemmelse(behandlingsresultat)
        return InnvilgelseRettigheterPlikterStandardvedlegg(bestemmelse?.kode)
    }

    fun mapBestemmelse(behandlingsresultat: Behandlingsresultat): Bestemmelse {
        val medlemskapsperiodeBestemmelse = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }.minByOrNull { it.fom }?.bestemmelse
        val lovvalgsperiodeBestemmelse = behandlingsresultat.lovvalgsperioder.filter { it.erInnvilget() }.minByOrNull { it.fom }?.bestemmelse
        val bestemmelse = medlemskapsperiodeBestemmelse ?: lovvalgsperiodeBestemmelse

        return bestemmelse ?: throw IllegalArgumentException("Behandlingsresultat har ingen bestemmelser")

    }
}
