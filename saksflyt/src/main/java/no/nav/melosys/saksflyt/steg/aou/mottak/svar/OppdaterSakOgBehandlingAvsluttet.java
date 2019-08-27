package no.nav.melosys.saksflyt.steg.aou.mottak.svar;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.saksflyt.steg.sob.SakOgBehandlingStegBehander;
import no.nav.melosys.service.BehandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakSvarOppdaterSakOgBehandlingAvsluttet")
public class OppdaterSakOgBehandlingAvsluttet extends SakOgBehandlingStegBehander {
    private static final Logger log = LoggerFactory.getLogger(OppdaterSakOgBehandlingAvsluttet.class);

    @Autowired
    protected OppdaterSakOgBehandlingAvsluttet(SakOgBehandlingFasade sakOgBehandlingFasade, TpsService tpsService, BehandlingService behandlingService) {
        super(sakOgBehandlingFasade, tpsService, behandlingService);
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_SVAR_SAK_OG_BEHANDLING_AVSLUTTET;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        long behandlingId = prosessinstans.getBehandling().getId();
        String saksnummer = prosessinstans.getBehandling().getFagsak().getSaksnummer();
        String aktørId = prosessinstans.getData(ProsessDataKey.AKTØR_ID);

        sakOgBehandlingAvsluttet(saksnummer, behandlingId, aktørId);
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
