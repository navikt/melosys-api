package no.nav.melosys.service;

import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MedlemAvFolketrygdenService {

    private final MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository;
    private final BehandlingsresultatService behandlingsresultatService;

    public MedlemAvFolketrygdenService(MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository, BehandlingsresultatService behandlingsresultatService) {
        this.medlemAvFolketrygdenRepository = medlemAvFolketrygdenRepository;
        this.behandlingsresultatService = behandlingsresultatService;
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

    public MedlemAvFolketrygden lagreBestemmelse(long behandlingsresultatID, Folketrygdloven_kap2_bestemmelser bestemmelse) {
        var medlemAvFolketrygden = finnMedlemAvFolketrygden(behandlingsresultatID);

        if (medlemAvFolketrygden.isPresent()) {
            medlemAvFolketrygden.get().setBestemmelse(bestemmelse);
            return lagre(medlemAvFolketrygden.get());
        }

        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID);
        var nyMedlemAvFolketrygden = new MedlemAvFolketrygden();
        nyMedlemAvFolketrygden.setBehandlingsresultat(behandlingsresultat);
        nyMedlemAvFolketrygden.setBestemmelse(bestemmelse);

        return lagre(nyMedlemAvFolketrygden);
    }
}
