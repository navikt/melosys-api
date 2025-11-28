package no.nav.melosys.saksflyt.steg.medl

import mu.KotlinLogging
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class LagreMedlemsperiodeMedl(
    private val medlemskapsperiodeService: MedlemskapsperiodeService,
    private val behandlingsresultatService: BehandlingsresultatService
) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.LAGRE_MEDLEMSKAPSPERIODE_MEDL
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.hentBehandling
        val behandlingID = prosessinstans.hentBehandling.id
        log.info { "[STEP-START] LagreMedlemsperiodeMedl behandlingId=$behandlingID" }
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        log.info { "[STEP-LOADED] LagreMedlemsperiodeMedl behandlingId=$behandlingID type=${behandlingsresultat.type}" }

        if(behandling.erEøsPensjonist()){
            return
        }

        if (behandling.erAndregangsbehandling() && behandling.opprinneligBehandling != null) {
            medlemskapsperiodeService.erstattMedlemskapsperioder(
                behandlingID,
                behandling.opprinneligBehandling!!.id,
                behandlingsresultat.medlemskapsperioder.toList()
            )
        } else {
            behandlingsresultat.medlemskapsperioder.filter(Medlemskapsperiode::erInnvilget).forEach {
                medlemskapsperiodeService.opprettEllerOppdaterMedlPeriode(behandlingID, it)
            }
        }
    }
}
