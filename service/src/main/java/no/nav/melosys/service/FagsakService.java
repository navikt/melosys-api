package no.nav.melosys.service;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FagsakService {

    private static final Logger log = LoggerFactory.getLogger(FagsakService.class);

    private static final String FAGSAKID_PREFIX = "MEL-";

    private final FagsakRepository fagsakRepository;

    private final BehandlingRepository behandlingRepository;

    private final SaksopplysningerService saksopplysningerService;

    private final TpsFasade tpsFasade;

    @Autowired
    public FagsakService(FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository, SaksopplysningerService saksopplysningerService, TpsFasade tpsFasade) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.saksopplysningerService = saksopplysningerService;
        this.tpsFasade = tpsFasade;
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

        // Leveranse 1 : Representant kun Orgno
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
        fagsak.setStatus(FagsakStatus.OPPRETTET);

        lagre(fagsak);

        Behandling behandling = new Behandling();
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        behandling.setFagsak(fagsak);
        behandling.setRegistrertDato(nå);
        behandling.setEndretDato(nå);

        behandling.setStatus(BehandlingStatus.OPPRETTET);
        behandling.setType(behandlingstype);
        behandlingRepository.save(behandling);
        return fagsak;
    }

    // FIXME Trenger test en metode for å opprette fagsaker utenom saksflyt?
    @Deprecated
    @Transactional
    public Fagsak testFagsakOgBehandling(String ident, String arbeidsgiver, String representant, Behandlingstype behandlingstype) throws SikkerhetsbegrensningException, IkkeFunnetException, TekniskException {
        String aktørID = tpsFasade.hentAktørIdForIdent(ident);

        Fagsak fagsak = nyFagsakOgBehandling(aktørID,arbeidsgiver, representant, behandlingstype);
        fagsak.setType(FagsakType.EU_EØS);
        Set<Saksopplysning> saksopplysninger = saksopplysningerService.hentSaksopplysninger(aktørID);
        Behandling behandling = fagsak.getAktivBehandling();
        saksopplysninger.forEach(x -> x.setBehandling(behandling));
        behandling.setSaksopplysninger(saksopplysninger);

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
