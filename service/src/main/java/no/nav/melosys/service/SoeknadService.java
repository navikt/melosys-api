package no.nav.melosys.service;

import java.time.Instant;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.SaksopplysningRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoeknadService {

    private final BehandlingRepository behandlingRepo;

    private static final String SØKNAD_VERSJON = "1.0";

    private final SaksopplysningRepository saksopplysningRepo;

    private final DokumentFactory dokumentFactory;

    @Autowired
    public SoeknadService(BehandlingRepository behandlingRepo, SaksopplysningRepository saksopplysningRepo, DokumentFactory dokumentFactory) {
        this.behandlingRepo = behandlingRepo;
        this.saksopplysningRepo = saksopplysningRepo;
        this.dokumentFactory = dokumentFactory;
    }

    public SoeknadDokument hentSoeknad(long behandlingID) throws IkkeFunnetException {
        Behandling behandling = behandlingRepo.findWithSaksopplysningerById(behandlingID);
        if (behandling == null) {
            throw new IkkeFunnetException("Behandling ikke funnet");
        }

        Optional<Saksopplysning> soeknadOpt = behandling.getSaksopplysninger().stream()
                .filter(s -> s.getType().equals(SaksopplysningType.SØKNAD))
                .findFirst();

        Saksopplysning soeknad = soeknadOpt.orElseThrow(() -> 
            new IkkeFunnetException(String.format("Søknad ikke funnet for behandlingsid %s.", behandlingID)));
        return (SoeknadDokument) soeknad.getDokument();
    }

    @Transactional
    public SoeknadDokument registrerSøknad(long behandlingID, SoeknadDokument soeknadDokument) throws IkkeFunnetException {
        Behandling behandling = behandlingRepo.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Registrering av søknad feilet fordi behandling med ID " + behandlingID + " er ikke funnet"));

        Optional<Saksopplysning> eksisterendeSaksopplysning = saksopplysningRepo.findByBehandlingAndType(behandling, SaksopplysningType.SØKNAD);
        Saksopplysning saksopplysning = eksisterendeSaksopplysning.orElse(opprettSaksopplysning(behandling));
        saksopplysning.setDokument(soeknadDokument);
        saksopplysning.setEndretDato(Instant.now());

        String internXml = dokumentFactory.lagInternXml(saksopplysning);
        // N.B. Det er ingen forskjell mellom dokumentXml og internXml her så langt,
        // og dokument_xml må ikke være NULL i databasen.
        saksopplysning.setDokumentXml(internXml);

        // Lagrer søknaden
        saksopplysningRepo.save(saksopplysning);

        return (SoeknadDokument) saksopplysning.getDokument();
    }

    private Saksopplysning opprettSaksopplysning(Behandling behandling) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        saksopplysning.setKilde(SaksopplysningKilde.SBH);
        saksopplysning.setVersjon(SØKNAD_VERSJON);
        saksopplysning.setRegistrertDato(Instant.now());
        saksopplysning.setBehandling(behandling);
        return saksopplysning;
    }

}
