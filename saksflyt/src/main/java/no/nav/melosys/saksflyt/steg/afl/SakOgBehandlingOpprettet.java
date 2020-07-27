package no.nav.melosys.saksflyt.steg.afl;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.sob.SobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AFLSakOgBehandlingOpprettet")
public class SakOgBehandlingOpprettet extends AbstraktStegBehandler {

    private final SobService sobService;

    @Autowired
    public SakOgBehandlingOpprettet(SobService sobService) {
        this.sobService = sobService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_SAK_OG_BEHANDLING_OPPRETTET;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        sobService.sakOgBehandlingOpprettet(
            prosessinstans.getBehandling().getFagsak().getSaksnummer(),
            prosessinstans.getBehandling().getId(),
            prosessinstans.getData(ProsessDataKey.AKTØR_ID)
        );

        prosessinstans.setSteg(ProsessSteg.AFL_AVSLUTT_TIDLIGERE_PERIODE);
    }
}
