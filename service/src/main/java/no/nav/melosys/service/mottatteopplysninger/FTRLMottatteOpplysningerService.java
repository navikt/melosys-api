package no.nav.melosys.service.mottatteopplysninger;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Midlertidig løsning for å fikse behandlinger laget ut motatte opplysninger
@Service
public class FTRLMottatteOpplysningerService {
    private static final Logger log = LoggerFactory.getLogger(FTRLMottatteOpplysningerService.class);

    private final BehandlingRepository behandlingRepository;

    public FTRLMottatteOpplysningerService(BehandlingRepository behandlingService) {
        this.behandlingRepository = behandlingService;
    }

    @Transactional
    public void opprettSøknadOgleggTilEksisterendeBehandlingOmMangler(
        MottatteOpplysningerService mottatteOpplysningerService, long behandlingID) {
        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke behandling med id " + behandlingID));

        if (behandling.getFagsak().getType() != Sakstyper.FTRL) {
            throw new IkkeFunnetException("Finner ikke mottatteOpplysninger for behandling " + behandlingID);
        }
        log.info("oppretter søknad og legger til eksisterende behandling: {}", behandlingID);
        mottatteOpplysningerService.opprettSøknad(behandling, new Periode(), new Soeknadsland());
    }
}
