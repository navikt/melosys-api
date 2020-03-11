package no.nav.melosys.saksflyt.steg.afl;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.saksflyt.steg.sob.SakOgBehandlingStegBehander;
import no.nav.melosys.service.BehandlingService;
import org.springframework.stereotype.Component;

@Component("AFLSakOgBehandlingOpprettet")
public class SakOgBehandlingOpprettet extends SakOgBehandlingStegBehander {

    public SakOgBehandlingOpprettet(SakOgBehandlingFasade sakOgBehandlingFasade, TpsService tpsService, BehandlingService behandlingService) {
        super(sakOgBehandlingFasade, tpsService, behandlingService);
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_SAK_OG_BEHANDLING_OPPRETTET;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        sakOgBehandlingOpprettet(
            prosessinstans.getBehandling().getFagsak().getSaksnummer(),
            prosessinstans.getBehandling().getId(),
            prosessinstans.getData(ProsessDataKey.AKTØR_ID)
        );

        prosessinstans.setSteg(ProsessSteg.AFL_AVSLUTT_TIDLIGERE_PERIODE);
    }
}
