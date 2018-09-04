package no.nav.melosys.service.journalforing;

import java.time.LocalDateTime;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.service.journalforing.dto.JournalforingDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JournalforingService {

    private Binge binge;

    private JoarkFasade joarkFasade;

    private ProsessinstansRepository prosessinstansRepo;

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
    public void opprettSakOgJournalfør(JournalforingDto journalforingDto) throws FunksjonellException {
        valider(journalforingDto);
        if (journalforingDto.getFagsak() == null) {
            throw new FunksjonellException("Opplysninger for å opprette en søknad mangler");
        }
        if (journalforingDto.getFagsak().getSoknadsperiode() == null) {
            throw new FunksjonellException("Søknadsperiode mangler");
        }
        if (journalforingDto.getFagsak().getSoknadsperiode().getFom() == null) {
            throw new FunksjonellException("Søknadsperiodes fra og med dato mangler");
        }
        if (journalforingDto.getFagsak().getSoknadsperiode().getTom() == null) {
            throw new FunksjonellException("Søknadsperiodes til og med dato mangler");
        }
        if (journalforingDto.getFagsak().getLand() == null || journalforingDto.getFagsak().getLand().isEmpty()) {
            throw new FunksjonellException("Land mangler");
        }
        
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_NY_SAK);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalforingDto.getJournalpostID());
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, journalforingDto.getDokumentID());
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, journalforingDto.getOppgaveID());
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, journalforingDto.getBrukerID());
        prosessinstans.setData(ProsessDataKey.AVSENDER_ID, journalforingDto.getAvsenderID());
        prosessinstans.setData(ProsessDataKey.AVSENDER_NAVN, journalforingDto.getAvsenderNavn());
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, journalforingDto.getDokumenttittel());
        //FIXME MELOSYS-1283 vedlegg
        // Land trenges av regelmodulen får å vurdere inngangsvilkår
        prosessinstans.setData(ProsessDataKey.LAND, journalforingDto.getFagsak().getLand());
        // Perioden trenges for å hente saksopplysninger
        prosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, journalforingDto.getFagsak().getSoknadsperiode());

        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);
        prosessinstansRepo.save(prosessinstans);
        binge.leggTil(prosessinstans);
    }

    @Transactional
    public void tilordneSakOgJournalfør(JournalforingDto journalforingDto) throws FunksjonellException {
        valider(journalforingDto);
        if (StringUtils.isEmpty(journalforingDto.getSaksnummer())) {
            throw new FunksjonellException("Saksnummer mangler");
        }

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalforingDto.getJournalpostID());
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, journalforingDto.getDokumentID());
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, journalforingDto.getOppgaveID());
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, journalforingDto.getBrukerID());
        prosessinstans.setData(ProsessDataKey.AVSENDER_ID, journalforingDto.getAvsenderID());
        prosessinstans.setData(ProsessDataKey.AVSENDER_NAVN, journalforingDto.getAvsenderNavn());
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, journalforingDto.getDokumenttittel());
        //FIXME MELOSYS-1283 vedlegg
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, journalforingDto.getSaksnummer());

        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);
        prosessinstansRepo.save(prosessinstans);
        binge.leggTil(prosessinstans);
    }

    // Denne er package-visible kun for at det skal være lettere å teste den isolert
    void valider(JournalforingDto journalforingDto) throws FunksjonellException {
        if (StringUtils.isEmpty(journalforingDto.getJournalpostID())) {
            throw new FunksjonellException("JournalpostID mangler");
        }
        if (StringUtils.isEmpty(journalforingDto.getOppgaveID())) {
            throw new FunksjonellException("OppgaveID mangler");
        }
        if (StringUtils.isEmpty(journalforingDto.getAvsenderID())) {
            throw new FunksjonellException("AvsenderID mangler");
        }
        if (StringUtils.isEmpty(journalforingDto.getAvsenderNavn())) {
            throw new FunksjonellException("AvsenderNavn mangler");
        }
        if (StringUtils.isEmpty(journalforingDto.getBrukerID())) {
            throw new FunksjonellException("BrukerID mangler");
        }
        if (StringUtils.isEmpty(journalforingDto.getDokumenttittel())) {
            throw new FunksjonellException("Dokumenttittel mangler");
        }
    }
}
