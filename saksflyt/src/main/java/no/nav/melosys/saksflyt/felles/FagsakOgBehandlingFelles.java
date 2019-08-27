package no.nav.melosys.saksflyt.felles;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FagsakOgBehandlingFelles {
    private static final Logger log = LoggerFactory.getLogger(FagsakOgBehandlingFelles.class);

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatRepository behandlingsresultatRepository;

    @Autowired
    public FagsakOgBehandlingFelles(FagsakService fagsakService,
                                    BehandlingService behandlingService,
                                    BehandlingsresultatRepository behandlingsresultatRepository) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    public void avsluttFagsakOgBehandling(Behandling behandling, Behandlingsresultattyper behandlingsresultattype) throws TekniskException, FunksjonellException {
        oppdaterFagsakOgBehandlingStatuser(behandling, Saksstatuser.LOVVALG_AVKLART, Behandlingsstatus.AVSLUTTET);

        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandling.getId())
            .orElseThrow(() -> new TekniskException("Finner ikke behandlingsresultat for behandling " + behandling.getId()));

        behandlingsresultat.setType(behandlingsresultattype);
        behandlingsresultat.setUtfallRegistreringUnntak(UtfallRegistreringUnntak.GODKJENT);
        behandlingsresultatRepository.save(behandlingsresultat);

        log.info("Periode regisrert og behandling avsluttet for fagsak {}, behandling {}", behandling.getFagsak().getSaksnummer(), behandling.getId());
    }

    public Fagsak opprettFagsakOgBehandling(String aktørId, Behandlingstyper behandlingstype, String journalpostId,
                                            String dokumentId, long gsakSaksnummer, Sakstyper sakstype)
        throws FunksjonellException {

        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder()
            .medAktørID(aktørId)
            .medBehandlingstype(behandlingstype)
            .medInitierendeJournalpostId(journalpostId)
            .medInitierendeDokumentId(dokumentId)
            .medGsakSaksnummer(gsakSaksnummer)
            .medSakstype(sakstype)
            .build();

        Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
        log.info("Fagsak og behandling opprettet for EESSI-sak med gsakSaksnummer {}", gsakSaksnummer);

        return fagsak;
    }

    @Transactional
    public Behandling opprettBehandlingPåEksisterendeFagsak(Fagsak fagsak, Behandlingsstatus behandlingsstatus, Behandlingstyper behandlingstype,
                                                            String journalpostId, String dokumentId, long gsakSaksnummer)
        throws TekniskException, IkkeFunnetException {

        if (Saksstatuser.OPPRETTET != fagsak.getStatus()) {
            fagsak.setStatus(Saksstatuser.OPPRETTET);
        }

        avsluttTidligereBehandling(fagsak);
        Behandling behandling = behandlingService.nyBehandling(fagsak, behandlingsstatus, behandlingstype, journalpostId, dokumentId);
        log.info("Opprettet ny behandling for fagsak {} med gsakSaksnummer {}", fagsak.getSaksnummer(), gsakSaksnummer);

        return behandling;
    }

    private void avsluttTidligereBehandling(Fagsak fagsak) throws TekniskException, IkkeFunnetException {
        Behandling aktivBehandling = fagsak.getAktivBehandling();

        if (aktivBehandling != null) {
            behandlingService.avsluttBehandling(aktivBehandling.getId());
        }
    }

    private void oppdaterFagsakOgBehandlingStatuser(Behandling behandling, Saksstatuser saksstatus, Behandlingsstatus behandlingsstatus) throws FunksjonellException {
        Fagsak fagsak = behandling.getFagsak();
        fagsak.setStatus(saksstatus);
        fagsakService.lagre(fagsak);
        behandling.setStatus(behandlingsstatus);
        behandlingService.oppdaterStatus(behandling.getId(), behandlingsstatus);
    }
}
