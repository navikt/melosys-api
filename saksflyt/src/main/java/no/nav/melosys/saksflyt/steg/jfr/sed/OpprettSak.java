package no.nav.melosys.saksflyt.steg.jfr.sed;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.SakService;
import org.springframework.stereotype.Component;

@Component("SedMottakOpprettSak")
public class OpprettSak extends AbstraktStegBehandler {

    private final FagsakService fagsakService;
    private final SakService sakService;

    public OpprettSak(FagsakService fagsakService, SakService sakService) {
        this.sakService = sakService;
        this.fagsakService = fagsakService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_OPPRETT_SAK;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        Fagsak fagsak = fagsakService.hentFagsak(prosessinstans.getBehandling().getFagsak().getSaksnummer());

        Long gsakSaksnummer = sakService.opprettSak(
            fagsak.getSaksnummer(),
            prosessinstans.getBehandling().getTema(),
            prosessinstans.getData(ProsessDataKey.AKTØR_ID)
        );

        fagsak.setGsakSaksnummer(gsakSaksnummer);
        fagsakService.lagre(fagsak);
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, gsakSaksnummer);

        prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_OPPDATER_SAKSRELASJON);
    }
}
