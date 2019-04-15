package no.nav.melosys.saksflyt.agent.ufm;

import java.util.Map;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("RegistreringUnntakFerdigstillJournalpost")
public class FerdigstillJournalpost extends AbstraktStegBehandler {

    private final JoarkFasade joarkFasade;

    @Autowired
    public FerdigstillJournalpost(JoarkFasade joarkFasade) {
        this.joarkFasade = joarkFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_FERDIGSTILL_JOURNALPOST;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        String journalpostId = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        joarkFasade.ferdigstillJournalføring(journalpostId);
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_HENT_PERSON);
    }
}
