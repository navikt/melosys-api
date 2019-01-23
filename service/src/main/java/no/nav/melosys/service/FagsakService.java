package no.nav.melosys.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessSteg.HENLEGG_SAK;

@Service
public class FagsakService {

    private static final String FAGSAKID_PREFIX = "MEL-";

    private final FagsakRepository fagsakRepository;

    private final BehandlingService behandlingService;

    private final TpsFasade tpsFasade;

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    private final ProsessinstansRepository prosessinstansRepo;

    private final Binge binge;

    @Autowired
    public FagsakService(FagsakRepository fagsakRepository,
                         BehandlingService behandlingService,
                         BehandlingsresultatRepository behandlingsresultatRepository,
                         TpsFasade tpsFasade,
                         ProsessinstansRepository prosessinstansRepo,
                         Binge binge) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingService = behandlingService;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.tpsFasade = tpsFasade;
        this.prosessinstansRepo = prosessinstansRepo;
        this.binge = binge;
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

    public void henleggFagsak(String saksnummer, String begrunnelse, String fritekst) throws TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);

        if (fagsak.getBehandlinger().isEmpty()) {
            throw new TekniskException("Fagsak med saksnummer " + saksnummer + " har ingen tilknyttede behandlinger.");
        }

        //hent siste behandling
        Behandling sisteBehandling = fagsak.getBehandlinger()
            .stream()
            .max(Comparator.comparing(RegistreringsInfo::getRegistrertDato))
            .get();


        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findOne(sisteBehandling.getId());

        Henleggelsesgrunner henleggelsesgrunn = Henleggelsesgrunner.valueOf(begrunnelse.toUpperCase());
        behandlingsresultat.setHenleggelsesgrunn(henleggelsesgrunn);

        if (Henleggelsesgrunner.ANNET == henleggelsesgrunn) {
            behandlingsresultat.setHenleggelseFritekst(fritekst);
        }

        behandlingsresultatRepository.save(behandlingsresultat);

        prosessinstans.setBehandling(sisteBehandling);
        prosessinstans.setType(ProsessType.HENLEGG_SAK);   //skal vi ha en egen prosesstype for henleggelse?
        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);

        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, SubjectHandler.getInstance().getUserID());
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, BehandlingsresultatType.HENLEGGELSE);

        prosessinstans.setSteg(HENLEGG_SAK);

        prosessinstansRepo.save(prosessinstans);
        binge.leggTil(prosessinstans);
    }
}
