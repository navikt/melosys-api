package no.nav.melosys.service.abac;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.journalforing.dto.JournalfoeringDto;
import no.nav.melosys.sikkerhet.abac.Pep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Tilgang {

    private BehandlingRepository behandlingRepository;
    private JoarkFasade joarkFasade;
    private Pep pep;

    @Autowired
    public Tilgang(BehandlingRepository behandlingRepository, JoarkFasade joarkFasade, Pep pep) {
        this.behandlingRepository = behandlingRepository;
        this.joarkFasade = joarkFasade;
        this.pep = pep;
    }

    // Behandling
    public void sjekk(long behandlingsId) throws SikkerhetsbegrensningException, TekniskException {
        Behandling behandling = behandlingRepository.findOne(behandlingsId);
        if (behandling == null) {
            throw new TekniskException("Klarte ikke å finne brukerident fra fagsak knyttet til behandlingid");
        }

        Fagsak fagsak  = behandling.getFagsak();
        Aktoer aktør = fagsak.hentAktørMedRolleType(RolleType.BRUKER);
        pep.sjekkTilgangTilAktoerId(aktør.getAktørId());
    }

    // Fagsak
    public void sjekk(Fagsak fagsak) throws SikkerhetsbegrensningException, TekniskException {
        Aktoer aktør = fagsak.hentAktørMedRolleType(RolleType.BRUKER);
        pep.sjekkTilgangTilAktoerId(aktør.getAktørId());
    }

    public void sjekkFnr(String fnr) throws SikkerhetsbegrensningException {
        pep.sjekkTilgangTilFnr(fnr);
    }

    // Journal
    public void sjekk(Journalpost journalpost) throws SikkerhetsbegrensningException, FunksjonellException, IntegrasjonException {
        sjekkJournalId(journalpost.getBrukerId());
    }

    public void sjekk(JournalfoeringDto journalfoeringDto) throws SikkerhetsbegrensningException, FunksjonellException, IntegrasjonException {
        sjekkJournalId(journalfoeringDto.getBrukerID());
    }

    public void sjekkJournalId(String journalId) throws SikkerhetsbegrensningException, FunksjonellException, IntegrasjonException {
        Journalpost journalpost = joarkFasade.hentJournalpost(journalId);
        pep.sjekkTilgangTilFnr(journalpost.getBrukerId());
    }
}
