package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.TemaFactory.fraBehandlingstema;

@Component
public class FerdigstillJournalpostSed implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(FerdigstillJournalpostSed.class);

    private final JoarkFasade joarkFasade;
    private final PersondataFasade persondataFasade;

    public FerdigstillJournalpostSed(JoarkFasade joarkFasade,
                                     PersondataFasade persondataFasade) {
        this.joarkFasade = joarkFasade;
        this.persondataFasade = persondataFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {

        final var behandling = prosessinstans.getBehandling();

        final String saksnummer = behandling.getFagsak().getSaksnummer();
        final String brukerID = hentBrukerID(prosessinstans);
        final MelosysEessiMelding eessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        final String tittel = prosessinstans.getData(ProsessDataKey.HOVEDDOKUMENT_TITTEL);
        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder()
            .medBrukerID(brukerID)
            .medSaksnummer(saksnummer)
            .medTittel(tittel)
            .medTema(fraBehandlingstema(behandling.getTema()).getKode())
            .build();

        joarkFasade.oppdaterJournalpost(eessiMelding.getJournalpostId(), journalpostOppdatering, true);
        log.info("Journalpost {} ferdigstilt for behandling {}", eessiMelding.getJournalpostId(), behandling.getId());
    }

    private String hentBrukerID(Prosessinstans prosessinstans) {
        String aktørID = prosessinstans.getBehandling().getFagsak().hentBrukersAktørID();
        return persondataFasade.hentFolkeregisterident(aktørID);
    }
}
