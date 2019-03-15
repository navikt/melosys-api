package no.nav.melosys.service.sak;

import java.time.Instant;
import java.util.*;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RegistreringsInfo;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FagsakService {

    private static final String FAGSAKID_PREFIX = "MEL-";

    private final FagsakRepository fagsakRepository;

    private final BehandlingService behandlingService;

    private final OppgaveService oppgaveService;

    private final TpsFasade tpsFasade;

    private final ProsessinstansService prosessinstansService;

    @Autowired
    public FagsakService(FagsakRepository fagsakRepository,
                         BehandlingService behandlingService,
                         OppgaveService oppgaveService,
                         TpsFasade tpsFasade,
                         ProsessinstansService prosessinstansService) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
        this.tpsFasade = tpsFasade;
        this.prosessinstansService = prosessinstansService;
    }

    public Optional<Behandling> finnRedigerbarBehandling(String ident, Fagsak fagsak) throws FunksjonellException, TekniskException {
        Behandling behandling = fagsak.getAktivBehandling();
        if (behandling == null) {
            return Optional.empty();
        }

        Optional<Oppgave> oppgave = oppgaveService.hentOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer());

        if (oppgave.isPresent()
            && oppgave.filter(oppgave1 -> ident.equalsIgnoreCase(oppgave1.getTilordnetRessurs())).isPresent()
            && behandling.erRedigerbar()) {
            return Optional.of(behandling);
        } else {
            return Optional.empty();
        }
    }

    @Transactional
    public void henleggFagsak(String saksnummer, String begrunnelseKodeString, String fritekst) throws TekniskException, FunksjonellException {
        Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);

        if (fagsak.getBehandlinger().isEmpty()) {
            throw new TekniskException("Fagsak med saksnummer " + saksnummer + " har ingen tilknyttede behandlinger.");
        }

        Henleggelsesgrunner begrunnelseKode;
        try {
            begrunnelseKode = Henleggelsesgrunner.valueOf(begrunnelseKodeString.toUpperCase());
        }
        catch (java.lang.IllegalArgumentException iae) {
            throw new TekniskException(begrunnelseKodeString.toUpperCase() + " er ingen gyldig henleggelsesgrunn");
        }

        Behandling sisteIkkeAvsluttedeBehandling = getSisteIkkeAvsluttedeBehandling(fagsak);

        prosessinstansService.opprettProsessinstansHenleggSak(sisteIkkeAvsluttedeBehandling, begrunnelseKode, fritekst);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(sisteIkkeAvsluttedeBehandling.getFagsak().getSaksnummer());
    }

    public Fagsak hentFagsak(String saksnummer) throws IkkeFunnetException {
        Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);
        if (fagsak == null) {
            throw new IkkeFunnetException("Det finnes ingen fagsak med saksnummer: " + saksnummer);
        }
        return fagsak;
    }

    public List<Fagsak> hentFagsakerMedAktør(Aktoersroller rolleType, String ident) throws IkkeFunnetException {
        String aktørID = tpsFasade.hentAktørIdForIdent(ident);
        return fagsakRepository.findByRolleAndAktør(rolleType, aktørID);
    }

    @Transactional
    public void lagre(Fagsak sak) {
        if (sak.getSaksnummer() == null) {
            sak.setSaksnummer(hentNesteSaksnummer());
        }
        fagsakRepository.save(sak);
    }

    @Transactional
    public void leggTilAktør(String saksnummer, Aktoersroller aktørsrolle, String ID) {
        Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);

        Aktoer aktør = new Aktoer();
        aktør.setRolle(aktørsrolle);
        switch (aktørsrolle) {
            case BRUKER:
                aktør.setAktørId(ID);
                break;
            case ARBEIDSGIVER:
            case REPRESENTANT:
                aktør.setOrgnr(ID);
                break;
            case MYNDIGHET:
                aktør.setInstitusjonId(ID);
                break;
            default:
                throw new IllegalStateException(aktørsrolle + " støttes ikke.");
        }

        fagsak.getAktører().add(aktør);
        fagsakRepository.save(fagsak);
    }

    /**
     * - Oppretter en ny fagsak med en ny behandling.
     * - Oppretter bruker, arbeidsgiver og representanter.
     * - Oppretter tom behandlingsresultat.
     */
    @Transactional
    public Fagsak nyFagsakOgBehandling(OpprettSakRequest opprettSakRequest) {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(hentNesteSaksnummer());

        HashSet<Aktoer> aktører = new HashSet<>();

        Aktoer aktør = new Aktoer();
        aktør.setAktørId(opprettSakRequest.getAktørID());
        aktør.setFagsak(fagsak);
        aktør.setRolle(Aktoersroller.BRUKER);
        aktører.add(aktør);

        String arbeidsgiver = opprettSakRequest.getArbeidsgiver();
        if (arbeidsgiver != null) {
            Aktoer aktørArbeidsgiver = new Aktoer();
            aktørArbeidsgiver.setOrgnr(arbeidsgiver);
            aktørArbeidsgiver.setFagsak(fagsak);
            aktørArbeidsgiver.setRolle(Aktoersroller.ARBEIDSGIVER);
            aktører.add(aktørArbeidsgiver);
        }

        String representant = opprettSakRequest.getRepresentant();
        if (representant != null) {
            Aktoer aktørRepresentant = new Aktoer();
            aktørRepresentant.setOrgnr(representant);
            aktørRepresentant.setFagsak(fagsak);
            aktørRepresentant.setRolle(Aktoersroller.REPRESENTANT);
            aktører.add(aktørRepresentant);
        }

        Instant nå = Instant.now();

        fagsak.setAktører(aktører);
        fagsak.setRegistrertDato(nå);
        fagsak.setEndretDato(nå);
        fagsak.setStatus(Saksstatuser.OPPRETTET);

        lagre(fagsak);

        Behandlingstyper behandlingstype = opprettSakRequest.getBehandlingstype();
        String initierendeJournalpostId = opprettSakRequest.getInitierendeJournalpostId();
        String initierendeDokumentId = opprettSakRequest.getInitierendeDokumentId();
        Behandling behandling = behandlingService.nyBehandling(fagsak, Behandlingsstatus.OPPRETTET, behandlingstype, initierendeJournalpostId, initierendeDokumentId);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        return fagsak;
    }

    private String hentNesteSaksnummer() {
        return FAGSAKID_PREFIX + fagsakRepository.hentNesteSekvensVerdi();
    }

    private Behandling getSisteIkkeAvsluttedeBehandling(Fagsak fagsak) {
        return fagsak.getBehandlinger()
            .stream()
            .filter(behandling -> behandling.getStatus() != Behandlingsstatus.AVSLUTTET)
            .max(Comparator.comparing(RegistreringsInfo::getRegistrertDato))
            .orElseThrow(() -> new IllegalStateException("Sak " + fagsak.getSaksnummer() + " har ingen behandlinger eller bare avsluttede behandlinger."));
    }
}
