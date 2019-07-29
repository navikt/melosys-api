package no.nav.melosys.service.unntak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.AnmodningsperiodeSvarType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.repository.AnmodningsperiodeSvarRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnmodningsperiodeService {
    private final AnmodningsperiodeRepository anmodningsperiodeRepository;
    private final BehandlingsresultatService behandlingsresultatService;
    private final AnmodningsperiodeSvarRepository anmodningsperiodeSvarRepository;

    @Autowired
    public AnmodningsperiodeService(AnmodningsperiodeRepository anmodningsperiodeRepository, BehandlingsresultatService behandlingsresultatService, AnmodningsperiodeSvarRepository anmodningsperiodeSvarRepository) {
        this.anmodningsperiodeRepository = anmodningsperiodeRepository;
        this.behandlingsresultatService = behandlingsresultatService;
        this.anmodningsperiodeSvarRepository = anmodningsperiodeSvarRepository;
    }

    public Optional<Anmodningsperiode> hentAnmodningsperiode(long anmodningsperiodeID) {
        return anmodningsperiodeRepository.findById(anmodningsperiodeID);
    }

    public Collection<Anmodningsperiode> hentAnmodningsperioder(long behandlingID) {
        return anmodningsperiodeRepository.findByBehandlingsresultatId(behandlingID);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public Collection<Anmodningsperiode> lagreAnmodningsperioder(long behandlingID, Collection<Anmodningsperiode> anmodningsperioder) throws FunksjonellException {
        List<Anmodningsperiode> eksisterende = anmodningsperiodeRepository.findByBehandlingsresultatId(behandlingID);

        for (Anmodningsperiode anmodningsperiode : eksisterende) {
            if (anmodningsperiode.getAnmodningsperiodeSvar() != null) {
                throw new FunksjonellException("Kan ikke oppdatere anmodningsperiode etter at svar er registrert!");
            }
        }

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        anmodningsperiodeRepository.deleteByBehandlingsresultat(behandlingsresultat);
        anmodningsperiodeRepository.flush();
        anmodningsperioder.forEach(a -> a.setBehandlingsresultat(behandlingsresultat));
        return anmodningsperiodeRepository.saveAll(anmodningsperioder);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public AnmodningsperiodeSvar lagreAnmodningsperiodeSvar(long anmodningsperiodeId, AnmodningsperiodeSvar anmodningsperiodeSvar) throws FunksjonellException {
        Anmodningsperiode anmodningsperiode = anmodningsperiodeRepository.findById(anmodningsperiodeId)
            .orElseThrow(() -> new IkkeFunnetException("Anmodningsperiode med id " + anmodningsperiodeId + " finnes ikke"));

        return lagreAnmodningsperiodeSvar(anmodningsperiode, anmodningsperiodeSvar);
    }

    public void lagreAnmodningsperiodeSvarForBehandling(long behandlingID, AnmodningsperiodeSvar anmodningsperiodeSvar) throws FunksjonellException {
        lagreAnmodningsperiodeSvar(hentFørsteAnmodningsperiode(behandlingID), anmodningsperiodeSvar);
    }

    private AnmodningsperiodeSvar lagreAnmodningsperiodeSvar(Anmodningsperiode anmodningsperiode, AnmodningsperiodeSvar anmodningsperiodeSvar) throws FunksjonellException {
        validerSvar(anmodningsperiodeSvar);

        if (anmodningsperiode.getAnmodningsperiodeSvar() != null) {
            anmodningsperiodeSvar = oppdaterOpprinneligSvar(anmodningsperiode.getAnmodningsperiodeSvar(), anmodningsperiodeSvar);
        }

        anmodningsperiodeSvar.setAnmodningsperiode(anmodningsperiode);
        return anmodningsperiodeSvarRepository.save(anmodningsperiodeSvar);
    }

    private Anmodningsperiode hentFørsteAnmodningsperiode(Long behandlingID) throws FunksjonellException {
        Collection<Anmodningsperiode> anmodningsperioder = hentAnmodningsperioder(behandlingID);

        if (anmodningsperioder.size() != 1) {
            throw new FunksjonellException("Forventet èn anmodningsperiode, fant " + anmodningsperioder.size());
        }

        return anmodningsperioder.iterator().next();
    }

    private AnmodningsperiodeSvar oppdaterOpprinneligSvar(AnmodningsperiodeSvar opprinnelig, AnmodningsperiodeSvar oppdatert) {
        opprinnelig.setAnmodningsperiodeSvarType(oppdatert.getAnmodningsperiodeSvarType());
        opprinnelig.setRegistrertDato(LocalDate.now());
        opprinnelig.setBegrunnelseFritekst(oppdatert.getBegrunnelseFritekst());
        opprinnelig.setInnvilgetFom(oppdatert.getInnvilgetFom());
        opprinnelig.setInnvilgetTom(oppdatert.getInnvilgetTom());
        return opprinnelig;
    }

    private void validerSvar(AnmodningsperiodeSvar anmodningsperiodeSvar) throws FunksjonellException {
        if (anmodningsperiodeSvar.getAnmodningsperiodeSvarType() == null) {
            throw new FunksjonellException("Må spesifiseres svarType for svar på anmodningsperiode");

        } else if (anmodningsperiodeSvar.getAnmodningsperiodeSvarType() == AnmodningsperiodeSvarType.DELVIS_INNVILGELSE
                        && !anmodningsperiodeSvar.erGyldigDelvisInnvilgelse()) {
            throw new FunksjonellException("Periode og begrunnelse må være fyllt ut ved " + AnmodningsperiodeSvarType.DELVIS_INNVILGELSE);

        } else if (anmodningsperiodeSvar.getAnmodningsperiodeSvarType() == AnmodningsperiodeSvarType.AVSLAG
                        && StringUtils.isEmpty(anmodningsperiodeSvar.getBegrunnelseFritekst())) {
            throw new FunksjonellException("Begrunnelse må være fyllt ut ved " + AnmodningsperiodeSvarType.AVSLAG);
        }
    }
}
