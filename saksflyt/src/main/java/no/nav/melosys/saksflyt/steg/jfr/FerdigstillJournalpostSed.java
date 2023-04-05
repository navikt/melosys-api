package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.Journalposttype;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FerdigstillJournalpostSed implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(FerdigstillJournalpostSed.class);

    private final JoarkFasade joarkFasade;
    private final PersondataFasade persondataFasade;
    private final OppgaveFactory oppgaveFactory;

    public FerdigstillJournalpostSed(JoarkFasade joarkFasade,
                                     PersondataFasade persondataFasade, OppgaveFactory oppgaveFactory) {
        this.joarkFasade = joarkFasade;
        this.persondataFasade = persondataFasade;
        this.oppgaveFactory = oppgaveFactory;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final MelosysEessiMelding eessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

        Journalpost journalpost = joarkFasade.hentJournalpost(eessiMelding.getJournalpostId());
        if (erJournalpostFerdigstilt(journalpost)) {
            log.warn("Journalpost {} for sed {} i RINA-sak {} er allerede ferdigstilt. Behandler ikke videre",
                eessiMelding.getJournalpostId(), eessiMelding.getSedId(), eessiMelding.getRinaSaksnummer());
        } else if (erJournalpostUtgått(journalpost)) {
            log.warn("Journalpost {} for sed {} i RINA-sak {} er utgått. Behandler ikke videre",
                eessiMelding.getJournalpostId(), eessiMelding.getSedId(), eessiMelding.getRinaSaksnummer());
        } else {
            final var behandling = prosessinstans.getBehandling();

            Fagsak fagsak = behandling.getFagsak();
            final String saksnummer = fagsak.getSaksnummer();
            final String brukerID = hentBrukerID(prosessinstans);
            final String tittel = prosessinstans.getData(ProsessDataKey.HOVEDDOKUMENT_TITTEL);

            JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder()
                .medBrukerID(brukerID)
                .medSaksnummer(saksnummer)
                .medTittel(tittel)
                .medTema(oppgaveFactory.utledTema(fagsak.getType(), fagsak.getTema(), behandling.getTema()).getKode())
                .build();

            joarkFasade.oppdaterOgFerdigstillJournalpost(eessiMelding.getJournalpostId(), journalpostOppdatering);

            log.info("Journalpost {} ferdigstilt for behandling {}", eessiMelding.getJournalpostId(), behandling.getId());
        }
    }

    private String hentBrukerID(Prosessinstans prosessinstans) {
        String aktørID = prosessinstans.getBehandling().getFagsak().hentBrukersAktørID();
        return persondataFasade.hentFolkeregisterident(aktørID);
    }

    private boolean erJournalpostFerdigstilt(Journalpost journalpost) {
        return (journalpost.isErFerdigstilt() && journalpost.getJournalposttype() == Journalposttype.INN);
    }

    private boolean erJournalpostUtgått(Journalpost journalpost) {
        return journalpost.isErUtgått();
    }
}
