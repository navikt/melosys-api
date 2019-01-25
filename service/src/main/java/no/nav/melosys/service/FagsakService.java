package no.nav.melosys.service;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.FagsakRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FagsakService {

    private static final String FAGSAKID_PREFIX = "MEL-";

    private final FagsakRepository fagsakRepository;

    private final BehandlingService behandlingService;

    private final TpsFasade tpsFasade;

    private final ProsessinstansService prosessinstansService;

    @Autowired
    public FagsakService(FagsakRepository fagsakRepository,
                         BehandlingService behandlingService,
                         TpsFasade tpsFasade,
                         ProsessinstansService prosessinstansService) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingService = behandlingService;
        this.tpsFasade = tpsFasade;
        this.prosessinstansService = prosessinstansService;
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
     * - Oppretter en ny fagsak med en ny behandling.
     * - Oppretter bruker, arbeidsgiver og representanter.
     * - Oppretter tom behandlingsresultat.
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

        if (arbeidsgiver != null) {
            Aktoer aktørArbeidsgiver = new Aktoer();
            aktørArbeidsgiver.setOrgnr(arbeidsgiver);
            aktørArbeidsgiver.setFagsak(fagsak);
            aktørArbeidsgiver.setRolle(RolleType.ARBEIDSGIVER);
            aktører.add(aktørArbeidsgiver);
        }

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

        Behandling behandling = behandlingService.nyBehandling(fagsak, Behandlingsstatus.OPPRETTET, behandlingstype);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        return fagsak;
    }

    private String hentNesteSaksnummer() {
        return FAGSAKID_PREFIX + fagsakRepository.hentNesteSekvensVerdi();
    }

    @Transactional
    public void henleggFagsak(String saksnummer, String begrunnelseKodeString, String fritekst) throws TekniskException {
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

        //hent siste behandling
        Behandling sisteIkkeAvsluttedeBehandling = fagsak.getBehandlinger()
            .stream()
            .filter(behandling -> behandling.getStatus() != Behandlingsstatus.AVSLUTTET)
            .max(Comparator.comparing(RegistreringsInfo::getRegistrertDato))
            .get();

        prosessinstansService.opprettProsessinstansHenleggSak(sisteIkkeAvsluttedeBehandling, begrunnelseKode, fritekst);
    }
}
