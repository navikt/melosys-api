package no.nav.melosys.saksflyt.steg.sed.sak;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("SedMottakOpprettGsakSak")
public class OpprettGsakSak extends AbstraktStegBehandler {

    private final GsakFasade gsakFasade;
    private final FagsakService fagsakService;

    public OpprettGsakSak(@Qualifier("system") GsakFasade gsakFasade, FagsakService fagsakService) {
        this.gsakFasade = gsakFasade;
        this.fagsakService = fagsakService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_OPPRETT_GSAK_SAK;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        Fagsak fagsak = prosessinstans.getBehandling().getFagsak();
        Long gsakSaksnummer = gsakFasade.opprettSak(
            fagsak.getSaksnummer(),
            prosessinstans.getBehandling().getType(),
            prosessinstans.getData(ProsessDataKey.AKTØR_ID)
        );

        fagsak.setGsakSaksnummer(gsakSaksnummer);
        fagsakService.lagre(fagsak);
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, gsakSaksnummer);

        prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_OPPDATER_SAKSRELASJON);
    }
}
