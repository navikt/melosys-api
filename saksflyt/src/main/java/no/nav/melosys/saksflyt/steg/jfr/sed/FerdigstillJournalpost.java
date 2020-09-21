package no.nav.melosys.saksflyt.steg.jfr.sed;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("SedMottakFerdigstillJournalpost")
public class FerdigstillJournalpost implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(FerdigstillJournalpost.class);

    private final JoarkFasade joarkFasade;
    private final TpsFasade tpsFasade;

    @Autowired
    public FerdigstillJournalpost(JoarkFasade joarkFasade, TpsFasade tpsFasade) {
        this.joarkFasade = joarkFasade;
        this.tpsFasade = tpsFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

        final Behandling behandling = prosessinstans.getBehandling();

        Long arkivSakID = behandling.getFagsak().getGsakSaksnummer();
        String brukerID = hentBrukerID(prosessinstans);
        String journalpostId = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        String dokumentID = prosessinstans.getData(ProsessDataKey.DOKUMENT_ID);
        String tittel = prosessinstans.getData(ProsessDataKey.HOVEDDOKUMENT_TITTEL);
        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder()
            .medBrukerID(brukerID).medArkivSakID(arkivSakID).medHovedDokumentID(dokumentID).medTittel(tittel).build();
        joarkFasade.oppdaterJournalpost(journalpostId, journalpostOppdatering, true);
        log.info("Journalpost {} ferdigstilt for behandling {}", journalpostId, behandling.getId());
    }

    private String hentBrukerID(Prosessinstans prosessinstans) throws IkkeFunnetException, TekniskException {
        String aktørID = prosessinstans.getBehandling().getFagsak().hentBruker().getAktørId();
        return tpsFasade.hentIdentForAktørId(aktørID);
    }
}
