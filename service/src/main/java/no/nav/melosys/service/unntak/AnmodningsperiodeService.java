package no.nav.melosys.service.unntak;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.anmodningsperiode.AnmodningsperiodeSvar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(rollbackFor = MelosysException.class)
    public Collection<Anmodningsperiode> lagreAnmodningsperioder(long behandlingID, Collection<Anmodningsperiode> anmodningsperioder) throws FunksjonellException {

        List<Anmodningsperiode> eksisterende = anmodningsperiodeRepository.findByBehandlingsresultatId(behandlingID);

        if (!eksisterende.isEmpty()) {
            for (Anmodningsperiode anmodningsperiode : eksisterende) {
                if (anmodningsperiode.getAnmodningsperiodeSvar() != null) {
                    throw new FunksjonellException("Kan ikke oppdatere anmodningsperiode etter at svar er registrert!");
                }
            }
        }

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        anmodningsperiodeRepository.deleteByBehandlingsresultat(behandlingsresultat);
        anmodningsperioder.forEach(a -> a.setBehandlingsresultat(behandlingsresultat));
        return  StreamSupport.stream(anmodningsperiodeRepository.saveAll(anmodningsperioder).spliterator(), false).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = MelosysException.class)
    public AnmodningsperiodeSvar lagreAnmodningsperiodeSvar(long anmodningsperiodeId, AnmodningsperiodeSvar anmodningsperiodeSvar) throws IkkeFunnetException {
        Anmodningsperiode anmodningsperiode = anmodningsperiodeRepository.findById(anmodningsperiodeId)
            .orElseThrow(() -> new IkkeFunnetException("Anmodningsperiode med id " + anmodningsperiodeId + " finnes ikke"));
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);
        return anmodningsperiodeRepository.save(anmodningsperiode).getAnmodningsperiodeSvar();
    }
}
