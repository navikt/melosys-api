package no.nav.melosys.service;

import java.util.Comparator;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
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

    private final SaksopplysningRepository saksopplysningRepo;

    private final DokumentFactory dokumentFactory;

    @Autowired
    public SoeknadService(BehandlingRepository behandlingRepo, SaksopplysningRepository saksopplysningRepo, DokumentFactory dokumentFactory) {
        this.behandlingRepo = behandlingRepo;
        this.saksopplysningRepo = saksopplysningRepo;
        this.dokumentFactory = dokumentFactory;
    }

    public SoeknadDokument hentSoeknad(long behandlingID) throws IkkeFunnetException {
        Behandling behandling = behandlingRepo.findOne(behandlingID);
        if (behandling == null) {
            throw new IkkeFunnetException("Behandling ikke funnet");
        }

        Comparator<? super Saksopplysning> comparator = Comparator.comparing(Saksopplysning::getRegistrertDato);

        // Vi henter den nyeste søknaden
        Optional<Saksopplysning> nyeste = behandling.getSaksopplysninger().stream().filter(s -> s.getType().equals(SaksopplysningType.SØKNAD)).sorted(comparator.reversed()).findFirst();

        if (nyeste.isPresent()) {
            Saksopplysning saksopplysning = nyeste.get();
            return (SoeknadDokument) saksopplysning.getDokument();
        } else {
            return null; // Behandlingen har ingen søknader
        }

    }

    @Transactional
    public SoeknadDokument registrerSøknad(long behandlingID, SoeknadDokument soeknadDokument) throws IkkeFunnetException {
        // Finner behandlingen som er relatert til søkndaden
        Behandling behandling = behandlingRepo.findOne(behandlingID);

        if (behandling == null) {
            throw new IkkeFunnetException("Registrering av søknad feilet fordi behandling med ID " + behandlingID + " er ikke funnet");
        }

        Saksopplysning saksopplysning = new Saksopplysning(soeknadDokument);
        saksopplysning.setBehandling(behandling);
        saksopplysning.setDokument(soeknadDokument);
        String internXml = dokumentFactory.lagInternXml(saksopplysning);
        // N.B. Det er ingen forskjell mellom dokumentXml og internXml her så langt,
        // og dokument_xml må ikke være NULL i databasen.
        saksopplysning.setDokumentXml(internXml);

        // Lagrer søknaden
        saksopplysningRepo.save(saksopplysning);

        return (SoeknadDokument) saksopplysning.getDokument();
    }

}
