package no.nav.melosys.tjenester.gui.fagsaker.trygdeavgift

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftsberegningsRequestMapper
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningRequest
import no.nav.melosys.service.MedlemAvFolketrygdenService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.BeregnetTrygdeavgiftDto
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.TrygdeavgiftsgrunnlagDto
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AarsavregningService (
    private val fagsakService: FagsakService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val medlemAvFolketrygdenService: MedlemAvFolketrygdenService
) {
    fun hentEksisterendeTrygdeavgiftsperioderForFagsak(saksnummer: String, år: Int): Set<Trygdeavgiftsperiode> {
        return fagsakService.hentFagsak(saksnummer)
            .behandlinger
            .flatMap { behandling ->
                behandlingsresultatService.hentBehandlingsresultat(behandling.id)
                    .medlemAvFolketrygden
                    .fastsattTrygdeavgift
                    .trygdeavgiftsperioder
                    .filter { periode -> periode.periodeFra.year == år }
            }
            .toSet()
    }

    fun beregnOgLagreAarsavgift(årsavgiftDto: ÅrsavgiftDto): Double {
        val trygdeavgiftsberegningRequest = TrygdeavgiftsberegningRequest(
            årsavgiftDto.medlemskapsPerioder.toSet(),
            årsavgiftDto.skatteforholdsperioder.toSet(),
            årsavgiftDto.inntektskilder,
            hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultatID, innvilgedeMedlemskapsperioder)
        )

        val beregnetTrygdeavgift = trygdeavgiftConsumer.beregnTrygdeavgift(trygdeavgiftsberegningRequest)
    }

    private fun hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultatID: Long, medlemskapsperioder: List<Medlemskapsperiode>): LocalDate? {
        if (medlemskapsperioder.any { it.erPliktig() }) {
            val fagsak = behandlingService.hentBehandling(behandlingsresultatID).fagsak
            return persondataService.hentPerson(fagsak.hentBruker().aktørId).fødselsdato
        }
        return null
    }
}
