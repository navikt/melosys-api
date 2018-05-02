package no.nav.melosys.service.journalforing;

import java.time.LocalDate;

import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Journalpost;
import no.nav.melosys.domain.gsak.AktorType;
import no.nav.melosys.domain.gsak.Fagomrade;
import no.nav.melosys.domain.gsak.Oppgavetype;
import no.nav.melosys.domain.gsak.PrioritetType;
import no.nav.melosys.integrasjon.felles.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.felles.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.OpprettOppgaveRequest;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.FagsakService;
import no.nav.melosys.service.journalforing.dto.JournalforingDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;

@Service
public class JournalforingService {

    GsakFasade gsakFasade;

    JoarkFasade joarkFasade;

    TpsFasade tpsFasade;

    FagsakService fagsakService;

    @Autowired
    public JournalforingService(JoarkFasade joarkFasade) {
        this.joarkFasade = joarkFasade;
    }

    public Journalpost hentJournalpost(String journalpostID) throws SikkerhetsbegrensningException {
        return joarkFasade.hentJournalpost(journalpostID);
    }

    @Transactional
    public void opprettSakOgJournalfør(JournalforingDto journalforingDto) throws SikkerhetsbegrensningException {
        try {
            BehandlingType behandlingType = BehandlingType.SØKNAD;

            // Henter aktørID
            String brukerID = journalforingDto.getBrukerID();
            String aktørID = null;
            try {
                aktørID = tpsFasade.hentAktørIdForIdent(brukerID);
            } catch (IkkeFunnetException e) {
                //FIXME
            }

            // Ny sak opprettes
            Fagsak fagsak = fagsakService.nyFagsakOgBehandling(brukerID, behandlingType, false);

            // Skyggesak opprettes i GSAK
            final String gsakSakId = gsakFasade.opprettSak(fagsak.getSaksnummer(), behandlingType, aktørID);

            // Journalposten oppdateres i Joark
            String journalpostID = journalforingDto.getJournalpostID();
            String avsenderID = journalforingDto.getAvsenderID();
            String avsenderNavn = journalforingDto.getAvsenderNavn();
            String tittel = journalforingDto.getDokumenttittel();
            joarkFasade.oppdaterJounalpost(journalpostID, gsakSakId, brukerID, avsenderID, avsenderNavn, tittel);

            // Journalposten ferdigstilles
            joarkFasade.ferdigstillJournalføring(journalpostID);

            // Sakstilknytningoppgaven ferdigstilles i GSAK
            gsakFasade.ferdigstillOppgave(journalforingDto.getOppgaveID());

            // Behandlingsoppgave opprettes.
            OpprettOppgaveRequest.Builder builder = OpprettOppgaveRequest.builder();
            builder.medSaksnummer(gsakSakId);
            builder.medAktivFra(LocalDate.now());
            builder.medAnsvarligEnhetId(String.valueOf(MELOSYS_ENHET_ID));
            builder.medOpprettetAvEnhetId(MELOSYS_ENHET_ID);

            if (BehandlingType.SØKNAD.equals(behandlingType)) {
                builder.medFagområde(Fagomrade.MED);
                builder.medOppgaveType(Oppgavetype.BEH_SAK_MED);
                builder.medPrioritetType(PrioritetType.NORM_MED);
            } else if (BehandlingType.UNNTAK_MEDL.equals(behandlingType)) {
                builder.medFagområde(Fagomrade.UFM);
                builder.medOppgaveType(Oppgavetype.BEH_SAK_MK_UFM);
                builder.medPrioritetType(PrioritetType.NORM_UFM);
            } else {
                throw new TekniskException("BehandlingType " + behandlingType.getBeskrivelse() + " støttes ikke.");
            }

            //builder.medUnderkategori() FIXME Ekisterer det i den nye GSAK tjenesten?
            builder.medAktørType(AktorType.PERSON);
            builder.medFnr(brukerID);
            //builder.medBeskrivelse(); FIXME settes til? Are T.
            //builder.medMottattDato(); FIXME settes? Are T.
            //builder.medNormertBehandlingsTidInnen(); FIXME settes?
            builder.medLest(false);

            gsakFasade.opprettOppgave(builder.build());

            //FIXME Sak og Behandling kalles
        } catch (SikkerhetsbegrensningException s) {
            throw new SikkerhetsbegrensningException(s);
        }
    }

    public void tilordneSakOgJournalfør(JournalforingDto journalforingDto) {

    }
}
