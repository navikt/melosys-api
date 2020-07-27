package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.sob.SobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OppdaterSakOgBehandlingAvsluttet extends AbstraktStegBehandler {

    private final SobService sobService;

    @Autowired
    public OppdaterSakOgBehandlingAvsluttet(SobService sobService) {
        this.sobService = sobService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

        long behandlingId = prosessinstans.getBehandling().getId();
        String saksnummer = prosessinstans.getBehandling().getFagsak().getSaksnummer();
        String aktørId = prosessinstans.getData(ProsessDataKey.AKTØR_ID);

        sobService.sakOgBehandlingAvsluttet(saksnummer, behandlingId, aktørId);
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
