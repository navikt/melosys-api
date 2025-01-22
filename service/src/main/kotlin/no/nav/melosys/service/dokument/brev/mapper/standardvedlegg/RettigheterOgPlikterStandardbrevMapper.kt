package no.nav.melosys.service.dokument.brev.mapper.standardvedlegg

import no.nav.melosys.integrasjon.dokgen.dto.standardvedlegg.InnvilgelseRettigheterPlikterStandardvedlegg
import no.nav.melosys.integrasjon.dokgen.dto.standardvedlegg.StandardvedleggDto
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Component

@Component
class RettigheterOgPlikterStandardbrevMapper(
    private val behandlingsresultatService: BehandlingsresultatService
) {

    fun mapInnvilgelse(behandlingId: Long, skalMappeBestemmelse: Boolean): InnvilgelseRettigheterPlikterStandardvedlegg {
        if (!skalMappeBestemmelse) {
            return InnvilgelseRettigheterPlikterStandardvedlegg()
        }
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId)

        val medlemskapsperiodeBestemmelse = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }.sortedBy { it.fom }.first().bestemmelse
        val lovvalgsperiodeBestemmelse = behandlingsresultat.lovvalgsperioder.filter { it.erInnvilget() }.sortedBy { it.fom }.first().bestemmelse
        val bestemmelse = medlemskapsperiodeBestemmelse ?: lovvalgsperiodeBestemmelse

        return InnvilgelseRettigheterPlikterStandardvedlegg(bestemmelse?.kode)
    }
}
