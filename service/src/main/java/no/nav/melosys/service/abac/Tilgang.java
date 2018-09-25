package no.nav.melosys.service.abac;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Journalpost;
import no.nav.melosys.domain.RolleType;
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
        pep.sjekkTilgangTil(fagsak.hentAktørMedRolleType(RolleType.BRUKER));
    }

    // Fagsak
    public void sjekk(Fagsak fagsak) throws SikkerhetsbegrensningException, TekniskException {
        pep.sjekkTilgangTil(fagsak.hentAktørMedRolleType(RolleType.BRUKER));
    }

    public void sjekk(String fnr) throws SikkerhetsbegrensningException {
        pep.sjekkTilgangTil(fnr);
    }


    // Journal
    public void sjekk(Journalpost journalPost) throws SikkerhetsbegrensningException, IntegrasjonException {
        sjekkJournalId(journalPost.getBrukerId());
    }

    public void sjekk(JournalfoeringDto journalDto) throws SikkerhetsbegrensningException, IntegrasjonException {
        sjekkJournalId(journalDto.getBrukerID());
    }

    public void sjekkJournalId(String journalId) throws SikkerhetsbegrensningException, IntegrasjonException {
        Journalpost journalpost = joarkFasade.hentJournalpost(journalId);
        pep.sjekkTilgangTil(journalpost.getBrukerId());
    }
}
