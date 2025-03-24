package no.nav.melosys.saksflyt.steg.arsavregning

import mu.KotlinLogging
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.sak.FagsakService
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Component
class OpprettÅrsavregningBehandling(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val årsavregningService: ÅrsavregningService,
    private val mottatteOpplysningerService: MottatteOpplysningerService
) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.OPPRETT_AARSAVREGNING_BEHANDLING
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val gjelderÅr = prosessinstans.getData(ProsessDataKey.GJELDER_ÅR).toInt()
        val sakMedTrygdeavgift = fagsakService.hentFagsak(prosessinstans.getData(ProsessDataKey.SAKSNUMMER))

        val trygdeavgiftsBehandlingtMedRelevantPeriode =
            årsavregningService.hentSisteBehandlingsresultatMedInnvilgetMedlemskapsperiodeOgAvgiftsgrunnlag(
                sakMedTrygdeavgift.saksnummer,
                gjelderÅr
            )?.behandling
                ?: throw TekniskException("Fant ingen behandling med innvilget medlemskapsperiode og avgiftsgrunnlag for sak: ${sakMedTrygdeavgift.saksnummer} og år: $gjelderÅr")



        val behandling = behandlingService.nyBehandling(
            sakMedTrygdeavgift,
            Behandlingsstatus.VURDER_DOKUMENT,
            Behandlingstyper.ÅRSAVREGNING,
            trygdeavgiftsBehandlingtMedRelevantPeriode.tema,
            null,
            null,
            LocalDate.now(),
            Behandlingsaarsaktyper.MELDING_FRA_SKATT,
            null
        ).also { nyBehandling ->
            log.info { "Oppretter årsavregning for sak: ${sakMedTrygdeavgift.saksnummer} og år: $gjelderÅr" }
            årsavregningService.opprettÅrsavregning(nyBehandling.id, gjelderÅr)
            prosessinstans.behandling = nyBehandling
        }

        mottatteOpplysningerService.opprettMottatteopplysningerForAarsavregninger(behandling.id)
    }
}
