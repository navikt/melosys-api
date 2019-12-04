package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.saksflyt.steg.sob.SakOgBehandlingStegBehander;
import no.nav.melosys.service.BehandlingService;
import org.springframework.stereotype.Component;

@Component
public class OppdaterSakOgBehandlingAvsluttet extends SakOgBehandlingStegBehander {

    protected OppdaterSakOgBehandlingAvsluttet(SakOgBehandlingFasade sakOgBehandlingFasade, TpsService tpsService, BehandlingService behandlingService) {
        super(sakOgBehandlingFasade, tpsService, behandlingService);
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

        long behandlingId = prosessinstans.getBehandling().getId();
        String saksnummer = prosessinstans.getBehandling().getFagsak().getSaksnummer();
        String aktørId = prosessinstans.getData(ProsessDataKey.AKTØR_ID);

        sakOgBehandlingAvsluttet(saksnummer, behandlingId, aktørId);
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
