package no.nav.melosys.tjenester.gui

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.tjenester.gui.dto.ÅrsavregningDto
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Service

@Service
class AarsavregningService (
    private val behandlingsresultatService: BehandlingsresultatService
) {
    fun hentEksisterendeTrygdeavgiftsperioderForBehandling(behandlingsId: Long, år: Int): List<Trygdeavgiftsperiode> =
        behandlingsresultatService.hentBehandlingsresultat(behandlingsId)
            .medlemAvFolketrygden
            .fastsattTrygdeavgift
            .trygdeavgiftsperioder
            .filter { periode -> periode.periodeFra.year == år }

    fun hentEksisterendeSkatteforholdsperioderForBehandling(behandlingsId: Long, år: Int): List<SkatteforholdTilNorge> =
        behandlingsresultatService.hentBehandlingsresultat(behandlingsId)
            .medlemAvFolketrygden
            .fastsattTrygdeavgift
            .trygdeavgiftsgrunnlag
            .skatteforholdTilNorge
            .filter { periode -> periode.fomDato.year == år }

    fun hentEksisterendeInntektsperioderForBehandling(behandlingsId: Long, år: Int): List<Inntektsperiode> =
        behandlingsresultatService.hentBehandlingsresultat(behandlingsId)
            .medlemAvFolketrygden
            .fastsattTrygdeavgift
            .trygdeavgiftsgrunnlag
            .inntektsperioder
            .filter { periode -> periode.fomDato.year == år }

    fun hentEksisterendeMedlemskapsPerioderForBehandling(behandlingsId: Long, år: Int): List<Medlemskapsperiode> =
        behandlingsresultatService.hentBehandlingsresultat(behandlingsId)
            .medlemAvFolketrygden
            .medlemskapsperioder
            .filter { periode -> periode.fom.year == år }

    fun hentAllePerioderForBehandling(behandlingsId: Long, år: Int): ÅrsavregningDto {
        val resultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsId)
        val medlem = resultat.medlemAvFolketrygden
        val fastsattTrygdeavgift = medlem.fastsattTrygdeavgift

        return ÅrsavregningDto(
            trygdeavgiftsPerioder = fastsattTrygdeavgift.trygdeavgiftsperioder.filter { it.periodeFra.year == år },
            skatteforholdsperioder = fastsattTrygdeavgift.trygdeavgiftsgrunnlag.skatteforholdTilNorge.filter { it.fomDato.year == år },
            inntektskilder = fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder.filter { it.fomDato.year == år },
            medlemskapsperioder = medlem.medlemskapsperioder.filter { it.fom.year == år }
        )
    }
}
