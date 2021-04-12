package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FerdigstillJournalpostSed implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(FerdigstillJournalpostSed.class);

    private final JoarkFasade joarkFasade;
    private final PersondataFasade persondataFasade;

    @Autowired
    public FerdigstillJournalpostSed(JoarkFasade joarkFasade, PersondataFasade persondataFasade) {
        this.joarkFasade = joarkFasade;
        this.persondataFasade = persondataFasade;
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
        MelosysEessiMelding eessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        String tittel = prosessinstans.getData(ProsessDataKey.HOVEDDOKUMENT_TITTEL);
        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder()
            .medBrukerID(brukerID)
            .medArkivSakID(arkivSakID)
            .medTittel(tittel)
            .build();

        joarkFasade.oppdaterJournalpost(eessiMelding.getJournalpostId(), journalpostOppdatering, true);
        log.info("Journalpost {} ferdigstilt for behandling {}", eessiMelding.getJournalpostId(), behandling.getId());
    }

    private String hentBrukerID(Prosessinstans prosessinstans) throws IkkeFunnetException, TekniskException {
        String aktørID = prosessinstans.getBehandling().getFagsak().hentBruker().getAktørId();
        return persondataFasade.hentFolkeregisterIdent(aktørID);
    }
}
