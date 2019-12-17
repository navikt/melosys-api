package no.nav.melosys.saksflyt.steg.gsak;

import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.GJENBRUK_OPPGAVE;

@Component
public class GjenbrukOppgave extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(GjenbrukOppgave.class);

    private final GsakFasade gsakFasade;

    @Autowired
    public GjenbrukOppgave(@Qualifier("system") GsakFasade gsakFasade) {
        this.gsakFasade = gsakFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return GJENBRUK_OPPGAVE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        final String oppgaveID = prosessinstans.getData(ProsessDataKey.OPPGAVE_ID);
        final String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);

        Oppgave oppgave = gsakFasade.hentOppgave(oppgaveID);
        Oppgave.Builder builder = new Oppgave.Builder(oppgave);
        builder.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        builder.setSaksnummer(saksnummer);
        builder.setBehandlesAvApplikasjon(Fagsystem.MELOSYS);
        gsakFasade.oppdaterOppgave(builder.build());

        prosessinstans.setSteg(ProsessSteg.FERDIG);
        log.info("PID {} har knyttet oppgave {} til sak {}", prosessinstans.getId(), oppgaveID, saksnummer);
    }
}
