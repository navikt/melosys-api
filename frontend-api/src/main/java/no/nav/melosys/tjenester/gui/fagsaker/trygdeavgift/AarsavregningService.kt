package no.nav.melosys.tjenester.gui.fagsaker.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.MedlemskapsperiodeDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningRequest
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningResponse
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import no.nav.melosys.service.sak.FagsakService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AarsavregningService (
    private val fagsakService: FagsakService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val behandlingService: BehandlingService,
    private val persondataService: PersondataService,
    private val trygdeavgiftConsumer: TrygdeavgiftConsumer
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

    fun beregnOgLagreAarsavgift(behandlingID: Long, årsavgiftDto: ÅrsavgiftDto): List<TrygdeavgiftsberegningResponse> {
        val trygdeavgiftsberegningRequest = TrygdeavgiftsberegningRequest(
            årsavgiftDto.medlemskapsPerioder.toSet(),
            årsavgiftDto.skatteforholdsperioder.toSet(),
            årsavgiftDto.inntektskilder,
            hentFødselsdatoOmViHarTjenstligBehov(behandlingID, årsavgiftDto.medlemskapsPerioder)
        )

        val beregnetTrygdeavgift = trygdeavgiftConsumer.beregnTrygdeavgift(trygdeavgiftsberegningRequest)
        return beregnetTrygdeavgift
    }

    private fun hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultatID: Long, medlemskapsperioder: List<MedlemskapsperiodeDto>): LocalDate? {
        if (medlemskapsperioder.any { it.medlemskapstype == Medlemskapstyper.PLIKTIG }) {
            val fagsak = behandlingService.hentBehandling(behandlingsresultatID).fagsak
            return persondataService.hentPerson(fagsak.hentBruker().aktørId).fødselsdato
        }
        return null
    }
}
