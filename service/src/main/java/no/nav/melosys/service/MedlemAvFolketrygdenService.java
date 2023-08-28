package no.nav.melosys.service;

import java.util.Optional;

import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import org.springframework.stereotype.Service;

@Service
public class MedlemAvFolketrygdenService {

    private final MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository;

    public MedlemAvFolketrygdenService(MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository) {
        this.medlemAvFolketrygdenRepository = medlemAvFolketrygdenRepository;
    }

    public MedlemAvFolketrygden hentMedlemAvFolketrygden(long behandlingsresultatID) {
        return finnMedlemAvFolketrygden(behandlingsresultatID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke medlemAvFolketrygden for behandlingsresultatID " + behandlingsresultatID));
    }

    public Optional<MedlemAvFolketrygden> finnMedlemAvFolketrygden(long behandlingsresultatID) {
        return medlemAvFolketrygdenRepository.findByBehandlingsresultatId(behandlingsresultatID);
    }

    public Optional<MedlemAvFolketrygden> finnMedlemAvFolketrygdenMedMedlemskapsperioder(long behandlingsresultatID) {
        return medlemAvFolketrygdenRepository.findWithMedlemskapsperioderByBehandlingsresultatId(behandlingsresultatID);
    }

    public MedlemAvFolketrygden lagre(MedlemAvFolketrygden medlemAvFolketrygden) {
        return medlemAvFolketrygdenRepository.save(medlemAvFolketrygden);
    }
}
