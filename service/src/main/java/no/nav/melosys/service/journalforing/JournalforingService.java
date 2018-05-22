package no.nav.melosys.service.journalforing;

import java.time.LocalDateTime;

import no.nav.melosys.domain.Journalpost;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.service.journalforing.dto.JournalforingDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JournalforingService {

    Binge binge;

    JoarkFasade joarkFasade;

    ProsessinstansRepository prosessinstansRepo;

    @Autowired
    public JournalforingService(Binge binge, JoarkFasade joarkFasade, ProsessinstansRepository prosessinstansRepo) {
        this.binge = binge;
        this.joarkFasade = joarkFasade;
        this.prosessinstansRepo = prosessinstansRepo;
    }

    public Journalpost hentJournalpost(String journalpostID) throws SikkerhetsbegrensningException {
        return joarkFasade.hentJournalpost(journalpostID);
    }

    @Transactional
    public void opprettSakOgJournalfør(JournalforingDto journalforingDto) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_NY_SAK);
        prosessinstans.setSteg(ProsessSteg.JFR_AKTOER_ID);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, journalforingDto.getBrukerID());
        prosessinstans.setData(ProsessDataKey.AVSENDER_ID, journalforingDto.getAvsenderID());
        prosessinstans.setData(ProsessDataKey.AVSENDER_NAVN, journalforingDto.getAvsenderNavn());
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, journalforingDto.getDokumenttittel());
        //FIXME vedlegg
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalforingDto.getJournalpostID());
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, journalforingDto.getOppgaveID());
        //TODO journalforingDto.getFagsak().getLand(); til inngangsvilkår
        //TODO journalforingDto.getFagsak().getSoknadsperiode(): for å hente saksopplysninger
        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setSistEndret(nå);
        prosessinstans.setRegistrertDato(nå);
        prosessinstansRepo.save(prosessinstans);
        binge.leggTil(prosessinstans);
    }

    @Transactional
    public void tilordneSakOgJournalfør(JournalforingDto journalforingDto) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        prosessinstans.setSteg(ProsessSteg.JFR_OPPDATER_JOURNALPOST);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, journalforingDto.getBrukerID());
        prosessinstans.setData(ProsessDataKey.AVSENDER_ID, journalforingDto.getAvsenderID());
        prosessinstans.setData(ProsessDataKey.AVSENDER_NAVN, journalforingDto.getAvsenderNavn());
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, journalforingDto.getDokumenttittel());
        //FIXME vedlegg
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalforingDto.getJournalpostID());
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, journalforingDto.getOppgaveID());
        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setSistEndret(nå);
        prosessinstans.setRegistrertDato(nå);
        prosessinstansRepo.save(prosessinstans);
        binge.leggTil(prosessinstans);
    }
}
