package no.nav.melosys.service.journalforing;

import java.time.LocalDateTime;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.service.journalforing.dto.JournalfoeringDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JournalfoeringService {

    private final Binge binge;

    private final JoarkFasade joarkFasade;

    private ProsessinstansRepository prosessinstansRepo;

    @Autowired
    public JournalfoeringService(Binge binge, JoarkFasade joarkFasade, ProsessinstansRepository prosessinstansRepo) {
        this.binge = binge;
        this.joarkFasade = joarkFasade;
        this.prosessinstansRepo = prosessinstansRepo;
    }

    public Journalpost hentJournalpost(String journalpostID) throws SikkerhetsbegrensningException, IkkeFunnetException, FunksjonellException, IntegrasjonException {
        return joarkFasade.hentJournalpost(journalpostID);
    }

    @Transactional
    public void opprettSakOgJournalfør(JournalfoeringOpprettDto journalfoeringDto) throws FunksjonellException, TekniskException {
        valider(journalfoeringDto);
        validerOpprettSakFelter(journalfoeringDto);
        
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_NY_SAK);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalfoeringDto.getJournalpostID());
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, journalfoeringDto.getDokumentID());
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, journalfoeringDto.getOppgaveID());
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, journalfoeringDto.getBrukerID());
        prosessinstans.setData(ProsessDataKey.AVSENDER_ID, journalfoeringDto.getAvsenderID());
        prosessinstans.setData(ProsessDataKey.AVSENDER_NAVN, journalfoeringDto.getAvsenderNavn());
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, journalfoeringDto.getDokumenttittel());
        //FIXME MELOSYS-1283 vedlegg
        // Land trenges av regelmodulen får å vurdere inngangsvilkår
        prosessinstans.setData(ProsessDataKey.OPPHOLDSLAND, journalfoeringDto.getFagsak().getLand());
        // Perioden trenges for å hente saksopplysninger
        prosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, journalfoeringDto.getFagsak().getSoknadsperiode());
        if (!StringUtils.isEmpty(journalfoeringDto.getArbeidsgiverID())) {
            prosessinstans.setData(ProsessDataKey.ARBEIDSGIVER, journalfoeringDto.getArbeidsgiverID());
        }

        if (!StringUtils.isEmpty(journalfoeringDto.getRepresentantID())) {
            prosessinstans.setData(ProsessDataKey.REPRESENTANT, journalfoeringDto.getRepresentantID());
        }

        if (SubjectHandler.getInstance().getUserID() != null) {
            prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, SubjectHandler.getInstance().getUserID());
        }

        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);
        prosessinstansRepo.save(prosessinstans);
        binge.leggTil(prosessinstans);
    }

    @Transactional
    public void tilordneSakOgJournalfør(JournalfoeringTilordneDto journalfoeringDto) throws FunksjonellException {
        valider(journalfoeringDto);
        if (StringUtils.isEmpty(journalfoeringDto.getSaksnummer())) {
            throw new FunksjonellException("Saksnummer mangler");
        }

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalfoeringDto.getJournalpostID());
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, journalfoeringDto.getDokumentID());
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, journalfoeringDto.getOppgaveID());
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, journalfoeringDto.getBrukerID());
        prosessinstans.setData(ProsessDataKey.AVSENDER_ID, journalfoeringDto.getAvsenderID());
        prosessinstans.setData(ProsessDataKey.AVSENDER_NAVN, journalfoeringDto.getAvsenderNavn());
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, journalfoeringDto.getDokumenttittel());
        //FIXME MELOSYS-1283 vedlegg
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, journalfoeringDto.getSaksnummer());

        if (SubjectHandler.getInstance().getUserID() != null) {
            prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, SubjectHandler.getInstance().getUserID());
        }

        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);
        prosessinstansRepo.save(prosessinstans);
        binge.leggTil(prosessinstans);
    }

    // Denne er package-visible kun for at det skal være lettere å teste den isolert
    void valider(JournalfoeringDto journalfoeringDto) throws FunksjonellException {
        if (StringUtils.isEmpty(journalfoeringDto.getJournalpostID())) {
            throw new FunksjonellException("JournalpostID mangler");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getDokumentID())) {
            throw new FunksjonellException("DokumentID mangler");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getOppgaveID())) {
            throw new FunksjonellException("OppgaveID mangler");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getAvsenderID())) {
            throw new FunksjonellException("AvsenderID mangler");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getAvsenderNavn())) {
            throw new FunksjonellException("AvsenderNavn mangler");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getBrukerID())) {
            throw new FunksjonellException("BrukerID mangler");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getDokumenttittel())) {
            throw new FunksjonellException("Dokumenttittel mangler");
        }
    }

    private void validerOpprettSakFelter(JournalfoeringOpprettDto journalfoeringDto) throws FunksjonellException {
        if (journalfoeringDto.getFagsak() == null) {
            throw new FunksjonellException("Opplysninger for å opprette en søknad mangler");
        }
        if (journalfoeringDto.getFagsak().getSoknadsperiode() == null) {
            throw new FunksjonellException("Søknadsperiode mangler");
        }
        if (journalfoeringDto.getFagsak().getSoknadsperiode().getFom() == null) {
            throw new FunksjonellException("Søknadsperiodes fra og med dato mangler");
        }
        if (journalfoeringDto.getFagsak().getSoknadsperiode().getTom() == null) {
            throw new FunksjonellException("Søknadsperiodes til og med dato mangler");
        }
        if (journalfoeringDto.getFagsak().getLand() == null || journalfoeringDto.getFagsak().getLand().isEmpty()) {
            throw new FunksjonellException("Land mangler");
        }
    }

}
