package no.nav.melosys.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakStatus;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.RolleType;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
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

    private FagsakRepository fagsakRepository;

    private SaksopplysningerService saksopplysningerService;

    @Autowired
    public FagsakService(FagsakRepository fagsakRepository, SaksopplysningerService saksopplysningerService) {
        this.fagsakRepository = fagsakRepository;
        this.saksopplysningerService = saksopplysningerService;
    }

    public List<Fagsak> hentFagsaker(RolleType rolleType, String aktørID) {
        return fagsakRepository.findByRolleAndAktør(rolleType, aktørID);
    }

    // FIXME: Den metoden er bare for å hjelpe frontend midlertidig. Må slettes.
    public Iterable<Fagsak> hentAlle() {
        return fagsakRepository.findAll();
    }

    public Fagsak hentFagsak(String saksnummer) {
        return fagsakRepository.findBySaksnummer(saksnummer);
    }

    @Transactional
    public Fagsak lagre(Fagsak sak) {
        if (sak.getSaksnummer() == null) {
            sak.setSaksnummer(hentNesteSaksnummer());
        }
        fagsakRepository.save(sak);
        return sak;
    }

    /**
     * Oppretter en fagsak og en behandling ut fra et fødselsnummer.
     */
    public Fagsak nyFagsakOgBehandling(String fnr, BehandlingType behandlingType, boolean erTestData) throws SikkerhetsbegrensningException {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(hentNesteSaksnummer());
        Behandling behandling = new Behandling();

        Aktoer aktoer = new Aktoer();
        aktoer.setAktørId(fnr);
        aktoer.setEksternId(fnr);
        aktoer.setFagsak(fagsak);
        aktoer.setRolle(RolleType.BRUKER);

        LocalDateTime dato = LocalDateTime.now();

        behandling.setFagsak(fagsak);
        behandling.setRegistrertDato(dato);
        behandling.setEndretDato(dato);
        // FIXME Saksopplysninger hentes separat når saksflyt tar seg av det. hentOpplysninger fjernes
        if (erTestData) {
            fagsak.setType(FagsakType.EU_EØS);
            Set<Saksopplysning> saksopplysninger = saksopplysningerService.hentSaksopplysninger(fnr);
            saksopplysninger.forEach(x -> x.setBehandling(behandling));
            behandling.setSaksopplysninger(saksopplysninger);
        }
        behandling.setStatus(BehandlingStatus.OPPRETTET);
        behandling.setType(behandlingType);

        fagsak.setAktører(new HashSet<>(Collections.singletonList(aktoer)));
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        fagsak.setRegistrertDato(dato);
        fagsak.setEndretDato(dato);
        fagsak.setStatus(FagsakStatus.OPPRETTET);

        return lagre(fagsak);
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
