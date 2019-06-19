package no.nav.melosys.service.unntak;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnmodningsperiodeService {
    private AnmodningsperiodeRepository anmodningsperiodeRepository;
    private BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public AnmodningsperiodeService(AnmodningsperiodeRepository anmodningsperiodeRepository, BehandlingsresultatService behandlingsresultatService) {
        this.anmodningsperiodeRepository = anmodningsperiodeRepository;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    public Collection<Anmodningsperiode> hentAnmodningsperioder(long behandlingID) {
        return anmodningsperiodeRepository.findByBehandlingsresultatId(behandlingID);
    }

    public Collection<Anmodningsperiode> lagreAnmodningsperioder(long behandlingID, Collection<Anmodningsperiode> anmodningsperioder) throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        anmodningsperioder.forEach(a -> a.setBehandlingsresultat(behandlingsresultat));
        return  StreamSupport.stream(anmodningsperiodeRepository.saveAll(anmodningsperioder).spliterator(), false).collect(Collectors.toList());
    }
}
