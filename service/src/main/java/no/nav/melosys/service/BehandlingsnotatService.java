package no.nav.melosys.service;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsnotat;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingsnotatRepository;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BehandlingsnotatService {

    private final BehandlingsnotatRepository behandlingsnotatRepository;
    private final FagsakService fagsakService;

    public BehandlingsnotatService(BehandlingsnotatRepository behandlingsnotatRepository, FagsakService fagsakService) {
        this.behandlingsnotatRepository = behandlingsnotatRepository;
        this.fagsakService = fagsakService;
    }

    @Transactional(readOnly = true)
    public Collection<Behandlingsnotat> hentNotatForFagsak(String saksnummer) throws IkkeFunnetException {
        return fagsakService.hentFagsak(saksnummer).getBehandlinger().stream()
            .flatMap(b -> b.getBehandlingsnotater().stream())
            .collect(Collectors.toList());
    }

    private Behandlingsnotat hentNotat(Long id) throws IkkeFunnetException {
        return behandlingsnotatRepository.findById(id)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke notat med id " + id));
    }

    @Transactional
    public Behandlingsnotat opprettNotat(String saksnummer, String tekst) throws FunksjonellException, TekniskException {
        Behandling behandling = Optional.ofNullable(fagsakService.hentFagsak(saksnummer).getAktivBehandling())
            .orElseThrow(() -> new FunksjonellException("Fagsak " + saksnummer + " har ingen aktive behandlinger"));

        Behandlingsnotat behandlingsnotat = new Behandlingsnotat();
        behandlingsnotat.setBehandling(behandling);
        behandlingsnotat.setTekst(tekst);
        return behandlingsnotatRepository.save(behandlingsnotat);
    }

    @Transactional
    public Behandlingsnotat oppdaterNotat(Long notatID, String tekst) throws FunksjonellException {
        Behandlingsnotat behandlingsnotat = hentNotat(notatID);

        if (!behandlingsnotat.erRedigerbar()) {
            throw new FunksjonellException("Notat med id " + notatID + " kan ikke oppdateres, da den tilhører en behandling som er avsluttet");
        } else if (!brukerKanRedigereNotat(behandlingsnotat)) {
            throw new FunksjonellException("Et notat kan ikke endres av andre!");
        }

        behandlingsnotat.setTekst(tekst);
        return behandlingsnotatRepository.save(behandlingsnotat);
    }

    public boolean kanRedigereNotat(Behandlingsnotat behandlingsnotat) {
        return behandlingsnotat.erRedigerbar() && brukerKanRedigereNotat(behandlingsnotat);
    }

    private boolean brukerKanRedigereNotat(Behandlingsnotat behandlingsnotat) {
        String innloggetBruker = SubjectHandler.getInstance().getUserID();
        return innloggetBruker.equals(behandlingsnotat.getRegistrertAv());
    }
}
