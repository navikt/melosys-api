package no.nav.melosys.saksflyt.steg.afl;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("AFLOpprettOppgave")
public class OpprettOppgave extends AbstraktStegBehandler {

    private final GsakFasade gsakFasade;

    public OpprettOppgave(@Qualifier("system") GsakFasade gsakFasade) {
        this.gsakFasade = gsakFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_OPPRETT_OPPGAVE;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        Behandling behandling = prosessinstans.getBehandling();
        String aktørID = prosessinstans.getData(ProsessDataKey.AKTØR_ID);
        boolean skalTilordnes = Optional.ofNullable(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).orElse(false);

        Oppgave oppgave = OppgaveFactory.lagBehandlingsOppgaveForType(behandling.getType())
            .setTilordnetRessurs(skalTilordnes ? prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER) : null)
            .setSaksnummer(behandling.getFagsak().getSaksnummer())
            .setAktørId(aktørID)
            .build();

        gsakFasade.opprettOppgave(oppgave);

        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
