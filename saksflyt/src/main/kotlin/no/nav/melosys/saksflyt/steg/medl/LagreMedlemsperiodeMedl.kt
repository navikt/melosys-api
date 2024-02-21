package no.nav.melosys.saksflyt.steg.medl

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import org.springframework.stereotype.Component

@Component
class LagreMedlemsperiodeMedl(
    private val medlemskapsperiodeService: MedlemskapsperiodeService,
    private val behandlingsresultatService: BehandlingsresultatService
) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.LAGRE_MEDLEMSKAPSPERIODE_MEDL
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.behandling
        val behandlingID = prosessinstans.behandling.id
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        if (behandling.erAndregangsbehandling() && behandling.opprinneligBehandling != null) {
            medlemskapsperiodeService.erstattMedlemskapsperioder(
                behandlingID,
                behandling.opprinneligBehandling.id,
                behandlingsresultat.finnMedlemskapsperioder().toList()
            )
        } else {
            behandlingsresultat.finnMedlemskapsperioder().filter(Medlemskapsperiode::erInnvilget).forEach {
                medlemskapsperiodeService.opprettEllerOppdaterMedlPeriode(behandlingID, it)
            }
        }
    }
}
