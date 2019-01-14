package no.nav.melosys.service.journalforing;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.ProsessinstansService;
import no.nav.melosys.service.journalforing.dto.JournalfoeringDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JournalfoeringService {

    private final JoarkFasade joarkFasade;

    private final OppgaveService oppgaveService;

    private final ProsessinstansService prosessinstansService;

    @Autowired
    public JournalfoeringService(
                                 JoarkFasade joarkFasade,
                                 OppgaveService oppgaveService,
                                 ProsessinstansService prosessinstansService) {
        this.joarkFasade = joarkFasade;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
    }

    public Journalpost hentJournalpost(String journalpostID) throws FunksjonellException, IntegrasjonException {
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
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, journalfoeringDto.getHoveddokumentTittel());
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

        prosessinstansService.lagreProsessinstans(prosessinstans);
        oppgaveService.ferdigstillOppgave(journalfoeringDto.getOppgaveID());
    }

    @Transactional
    public void tilordneSakOgJournalfør(JournalfoeringTilordneDto journalfoeringDto) throws FunksjonellException, TekniskException {
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
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, journalfoeringDto.getHoveddokumentTittel());
        //FIXME MELOSYS-1283 vedlegg
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, journalfoeringDto.getSaksnummer());
        if (journalfoeringDto.getBehandlingstype() != null) {
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, journalfoeringDto.getBehandlingstype());
        }

        prosessinstansService.lagreProsessinstans(prosessinstans);
        oppgaveService.ferdigstillOppgave(journalfoeringDto.getOppgaveID());
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
        if (StringUtils.isEmpty(journalfoeringDto.getHoveddokumentTittel())) {
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
