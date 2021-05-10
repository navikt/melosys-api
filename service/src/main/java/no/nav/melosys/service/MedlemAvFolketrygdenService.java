package no.nav.melosys.service;

import java.util.Optional;

import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MedlemAvFolketrygdenService {

    private final MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository;

    @Autowired
    public MedlemAvFolketrygdenService(MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository) {
        this.medlemAvFolketrygdenRepository = medlemAvFolketrygdenRepository;
    }

    public MedlemAvFolketrygden hentMedlemAvFolketrygden(long behandlingsresultatID) throws IkkeFunnetException {
        return finnMedlemAvFolketrygden(behandlingsresultatID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke medlemAvFolketrygden for behandlingsresultatID " + behandlingsresultatID));
    }

    public Optional<MedlemAvFolketrygden> finnMedlemAvFolketrygden(long behandlingsresultatID) {
        return medlemAvFolketrygdenRepository.findByBehandlingsresultatId(behandlingsresultatID);
    }
}
