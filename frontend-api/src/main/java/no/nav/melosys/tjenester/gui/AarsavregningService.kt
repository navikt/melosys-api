package no.nav.melosys.tjenester.gui

import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.tjenester.gui.dto.ÅrsavregningDto
import org.springframework.stereotype.Service

@Service
class AarsavregningService (
    private val behandlingsresultatService: BehandlingsresultatService
) {
    fun hentPerioderForAarsavregningForBehandling(behandlingsId: Long, år: Int): ÅrsavregningDto {
        //TODO vi må filtrere også på sluttdato for alle andre perioder enn trygdeavgiftsPerioder
        val medlemAvFolketrygden = behandlingsresultatService.hentBehandlingsresultat(behandlingsId).medlemAvFolketrygden
        val fastsattTrygdeavgift = medlemAvFolketrygden.fastsattTrygdeavgift
        return ÅrsavregningDto.av(trygdeavgiftsPerioder = fastsattTrygdeavgift.trygdeavgiftsperioder.filter { it.periodeFra.year == år },
            skatteforholdsperioder = fastsattTrygdeavgift.trygdeavgiftsgrunnlag.skatteforholdTilNorge.filter { it.fomDato.year == år },
            inntektskilder = fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder.filter { it.fomDato.year == år },
            medlemskapsperioder = medlemAvFolketrygden.medlemskapsperioder.filter { it.fom.year == år })
    }
}
