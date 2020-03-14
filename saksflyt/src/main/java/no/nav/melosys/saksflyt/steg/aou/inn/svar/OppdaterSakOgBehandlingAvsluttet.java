package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.saksflyt.steg.sob.SakOgBehandlingStegBehander;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakSvarOppdaterSakOgBehandlingAvsluttet")
public class OppdaterSakOgBehandlingAvsluttet extends SakOgBehandlingStegBehander {
    private static final Logger log = LoggerFactory.getLogger(OppdaterSakOgBehandlingAvsluttet.class);

    private final FagsakService fagsakService;

    @Autowired
    protected OppdaterSakOgBehandlingAvsluttet(SakOgBehandlingFasade sakOgBehandlingFasade,
                                               TpsService tpsService,
                                               BehandlingService behandlingService,
                                               FagsakService fagsakService) {
        super(sakOgBehandlingFasade, tpsService, behandlingService);
        this.fagsakService = fagsakService;
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

        fagsakService.avsluttFagsakOgBehandling(
            prosessinstans.getBehandling().getFagsak(), Saksstatuser.LOVVALG_AVKLART, prosessinstans.getBehandling()
        );
        sakOgBehandlingAvsluttet(saksnummer, behandlingId, aktørId);
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
