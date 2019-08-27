package no.nav.melosys.saksflyt.felles;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FagsakOgBehandlingFelles {
    private static final Logger log = LoggerFactory.getLogger(FagsakOgBehandlingFelles.class);

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;

    @Autowired
    public FagsakOgBehandlingFelles(FagsakService fagsakService,
                                    BehandlingService behandlingService) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
    }

    public void avsluttFagsakOgBehandling(Behandling behandling, Saksstatuser saksstatuser) throws FunksjonellException {
        oppdaterFagsakOgBehandlingStatuser(behandling, saksstatuser, Behandlingsstatus.AVSLUTTET);
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
