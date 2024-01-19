package no.nav.melosys.saksflyt.steg.medl

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService
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
        val behandlingId = prosessinstans.behandling.id
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId)

        val innvilgedeMedlemskapsperioder = behandlingsresultat.finnMedlemskapsperioder()
            .stream().filter { obj: Medlemskapsperiode -> obj.erInnvilget() }.toList()
        if ((behandling.erNyVurdering() || behandling.erManglendeInnbetalingTrygdeavgift()) && behandling.opprinneligBehandling != null) {
            medlemskapsperiodeService.erstattMedlemskapsperioder(
                innvilgedeMedlemskapsperioder,
                behandling.opprinneligBehandling.id,
                behandlingId
            )
        } else {
            for (medlemskapsperiode in innvilgedeMedlemskapsperioder) {
                medlemskapsperiodeService.opprettEllerOppdaterMedlPeriode(behandlingId, medlemskapsperiode)
            }
        }
    }
}
