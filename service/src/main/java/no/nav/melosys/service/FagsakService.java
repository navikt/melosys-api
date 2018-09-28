package no.nav.melosys.service;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.service.datavarehus.BehandlingOpprettetEvent;
import no.nav.melosys.service.datavarehus.FagsakOpprettetEvent;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FagsakService {

    private static final String FAGSAKID_PREFIX = "MEL-";

    private final FagsakRepository fagsakRepository;

    private final BehandlingRepository behandlingRepository;

    private final SaksopplysningerService saksopplysningerService;

    private final TpsFasade tpsFasade;

    // FIXME: Fjernes når testmetode for oppretting av fagsak fjernes
    @Deprecated
    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public FagsakService(FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository,
                         SaksopplysningerService saksopplysningerService, TpsFasade tpsFasade,
                         ApplicationEventPublisher applicationEventPublisher) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.saksopplysningerService = saksopplysningerService;
        this.tpsFasade = tpsFasade;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public Fagsak hentFagsak(String saksnummer) {
        return fagsakRepository.findBySaksnummer(saksnummer);
    }

    public List<Fagsak> hentFagsakerMedAktør(RolleType rolleType, String ident) throws IkkeFunnetException {
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

    /**
     * Oppretter en fagsak og en behandling ut fra et fødselsnummer.
     */
    @Transactional
    public Fagsak nyFagsakOgBehandling(String aktørID, String arbeidsgiver, String representant, Behandlingstype behandlingstype) {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(hentNesteSaksnummer());

        HashSet<Aktoer> aktører = new HashSet<>();

        Aktoer aktør = new Aktoer();
        aktør.setAktørId(aktørID);
        aktør.setFagsak(fagsak);
        aktør.setRolle(RolleType.BRUKER);
        aktører.add(aktør);

        Aktoer aktørArbeidsgiver = new Aktoer();
        aktørArbeidsgiver.setOrgnr(arbeidsgiver);
        aktørArbeidsgiver.setFagsak(fagsak);
        aktørArbeidsgiver.setRolle(RolleType.ARBEIDSGIVER);
        aktører.add(aktørArbeidsgiver);

        if (representant != null) {
            Aktoer aktørRepresentant = new Aktoer();
            aktørRepresentant.setOrgnr(representant);
            aktørRepresentant.setFagsak(fagsak);
            aktørRepresentant.setRolle(RolleType.REPRESENTANT);
            aktører.add(aktørRepresentant);
        }

        Instant nå = Instant.now();

        fagsak.setAktører(aktører);
        fagsak.setRegistrertDato(nå);
        fagsak.setEndretDato(nå);
        fagsak.setStatus(Fagsaksstatus.OPPRETTET);

        lagre(fagsak);

        Behandling behandling = new Behandling();
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        behandling.setFagsak(fagsak);
        behandling.setRegistrertDato(nå);
        behandling.setEndretDato(nå);

        behandling.setStatus(Behandlingsstatus.OPPRETTET);
        behandling.setType(behandlingstype);
        behandlingRepository.save(behandling);
        return fagsak;
    }

    // FIXME Trenger test en metode for å opprette fagsaker utenom saksflyt?
    @Deprecated
    @Transactional
    public Fagsak testFagsakOgBehandling(String ident, Behandlingstype behandlingstype) throws SikkerhetsbegrensningException, IkkeFunnetException, TekniskException {
        String aktørID = tpsFasade.hentAktørIdForIdent(ident);

        Fagsak fagsak = nyFagsakOgBehandling(aktørID, null, null, behandlingstype);
        fagsak.setType(Fagsakstype.EU_EØS);
        Set<Saksopplysning> saksopplysninger = saksopplysningerService.hentSaksopplysninger(aktørID);
        Behandling behandling = fagsak.getAktivBehandling();
        saksopplysninger.forEach(x -> x.setBehandling(behandling));
        behandling.setSaksopplysninger(saksopplysninger);

        applicationEventPublisher.publishEvent(new FagsakOpprettetEvent(fagsak, SubjectHandler.getInstance().getUserID()));
        applicationEventPublisher.publishEvent(new BehandlingOpprettetEvent(behandling, SubjectHandler.getInstance().getUserID()));

        return fagsakRepository.save(fagsak);
    }

    private String hentNesteSaksnummer() {
        Long sekvensVerdi = fagsakRepository.hentNesteSekvensVerdi();
        if (sekvensVerdi == null) {
            throw new RuntimeException("Henting av neste SekvensVerdi fra sekvensen feilet.");
        } else {
            return FAGSAKID_PREFIX + Long.toString(sekvensVerdi);
        }
    }
}
