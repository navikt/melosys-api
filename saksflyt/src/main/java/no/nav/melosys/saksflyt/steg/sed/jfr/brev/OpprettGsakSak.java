package no.nav.melosys.saksflyt.steg.sed.jfr.brev;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("JournalførAouBrevOpprettGsakSak")
public class OpprettGsakSak extends AbstraktStegBehandler {
    private final OppgaveService oppgaveService;

    public OpprettGsakSak(@Qualifier("system") OppgaveService oppgaveService) {
        this.oppgaveService = oppgaveService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_AOU_BREV_OPPRETT_GSAK_SAK;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        Long gsakSaksnummer = oppgaveService.opprettSakForFagsak(
            prosessinstans.getBehandling().getFagsak(),
            prosessinstans.getBehandling().getType(),
            prosessinstans.getData(ProsessDataKey.AKTØR_ID)
        );

        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, gsakSaksnummer);
        prosessinstans.setSteg(ProsessSteg.JFR_AOU_BREV_FERDIGSTILL_JOURNALPOST);
    }
}
