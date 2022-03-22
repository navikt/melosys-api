package no.nav.melosys.service.unntak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.repository.AnmodningsperiodeSvarRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.util.StringUtils.hasText;

@Service
public class AnmodningsperiodeService {
    private final AnmodningsperiodeRepository anmodningsperiodeRepository;
    private final BehandlingsresultatService behandlingsresultatService;
    private final AnmodningsperiodeSvarRepository anmodningsperiodeSvarRepository;

    public AnmodningsperiodeService(AnmodningsperiodeRepository anmodningsperiodeRepository, BehandlingsresultatService behandlingsresultatService, AnmodningsperiodeSvarRepository anmodningsperiodeSvarRepository) {
        this.anmodningsperiodeRepository = anmodningsperiodeRepository;
        this.behandlingsresultatService = behandlingsresultatService;
        this.anmodningsperiodeSvarRepository = anmodningsperiodeSvarRepository;
    }

    public Optional<Anmodningsperiode> finnAnmodningsperiode(long anmodningsperiodeID) {
        return anmodningsperiodeRepository.findById(anmodningsperiodeID);
    }

    public Collection<Anmodningsperiode> hentAnmodningsperioder(long behandlingID) {
        return anmodningsperiodeRepository.findByBehandlingsresultatId(behandlingID);
    }

    public Optional<AnmodningsperiodeSvar> hentAnmodningsperiodeSvar(long anmodningsperiodeID) {
        return anmodningsperiodeSvarRepository.findById(anmodningsperiodeID);
    }

    public Collection<AnmodningsperiodeSvar> hentAnmodningsperiodeSvarForBehandling(long behandlingID) {
        return hentAnmodningsperioder(behandlingID).stream()
            .map(Anmodningsperiode::getId)
            .map(this::hentAnmodningsperiodeSvar)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    @Transactional
    public Collection<Anmodningsperiode> lagreAnmodningsperioder(long behandlingID, Collection<Anmodningsperiode> anmodningsperioder) {
        List<Anmodningsperiode> eksisterende = anmodningsperiodeRepository.findByBehandlingsresultatId(behandlingID);

        for (Anmodningsperiode anmodningsperiode : eksisterende) {
            if (anmodningsperiode.getAnmodningsperiodeSvar() != null) {
                throw new FunksjonellException("Kan ikke oppdatere anmodningsperiode etter at svar er registrert!");
            } else if (anmodningsperiode.erSendtUtland()) {
                throw new FunksjonellException("Kan ikke oppdatere anmodningsperiode etter A001 er sendt!");
            }
        }

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        anmodningsperiodeRepository.deleteByBehandlingsresultat(behandlingsresultat);
        anmodningsperiodeRepository.flush();
        anmodningsperioder.forEach(a -> a.setBehandlingsresultat(behandlingsresultat));
        return anmodningsperiodeRepository.saveAll(anmodningsperioder);
    }

    @Transactional
    public AnmodningsperiodeSvar lagreAnmodningsperiodeSvar(long anmodningsperiodeId, AnmodningsperiodeSvar anmodningsperiodeSvar) {
        Anmodningsperiode anmodningsperiode = anmodningsperiodeRepository.findById(anmodningsperiodeId)
            .orElseThrow(() -> new IkkeFunnetException("Anmodningsperiode med id " + anmodningsperiodeId + " finnes ikke"));

        return lagreAnmodningsperiodeSvar(anmodningsperiode, anmodningsperiodeSvar);
    }

    public void lagreAnmodningsperiodeSvarForBehandling(long behandlingID, AnmodningsperiodeSvar anmodningsperiodeSvar) {
        lagreAnmodningsperiodeSvar(hentFørsteAnmodningsperiode(behandlingID), anmodningsperiodeSvar);
    }

    public void oppdaterAnmodningsperiodeSendtForBehandling(long behandlingID) {
        Anmodningsperiode anmodningsperiode = hentFørsteAnmodningsperiode(behandlingID);
        anmodningsperiode.setSendtUtland(true);
        anmodningsperiodeRepository.save(anmodningsperiode);
    }

    public Anmodningsperiode hentFørsteAnmodningsperiode(Long behandlingID) {
        Collection<Anmodningsperiode> anmodningsperioder = hentAnmodningsperioder(behandlingID);

        if (anmodningsperioder.size() != 1) {
            throw new FunksjonellException("Forventet én anmodningsperiode på behandling" + behandlingID + ", fant " + anmodningsperioder.size());
        }

        return anmodningsperioder.iterator().next();
    }

    private AnmodningsperiodeSvar lagreAnmodningsperiodeSvar(Anmodningsperiode anmodningsperiode, AnmodningsperiodeSvar anmodningsperiodeSvar) {
        validerSvar(anmodningsperiodeSvar);

        if (anmodningsperiode.getAnmodningsperiodeSvar() != null) {
            anmodningsperiodeSvar = oppdaterOpprinneligSvar(anmodningsperiode.getAnmodningsperiodeSvar(), anmodningsperiodeSvar);
        }

        anmodningsperiodeSvar.setAnmodningsperiode(anmodningsperiode);
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);
        return anmodningsperiodeSvarRepository.save(anmodningsperiodeSvar);
    }

    private AnmodningsperiodeSvar oppdaterOpprinneligSvar(AnmodningsperiodeSvar opprinnelig, AnmodningsperiodeSvar oppdatert) {
        opprinnelig.setAnmodningsperiodeSvarType(oppdatert.getAnmodningsperiodeSvarType());
        opprinnelig.setRegistrertDato(LocalDate.now());
        opprinnelig.setBegrunnelseFritekst(oppdatert.getBegrunnelseFritekst());
        opprinnelig.setInnvilgetFom(oppdatert.getInnvilgetFom());
        opprinnelig.setInnvilgetTom(oppdatert.getInnvilgetTom());
        return opprinnelig;
    }

    private void validerSvar(AnmodningsperiodeSvar anmodningsperiodeSvar) {
        validerSvartype(anmodningsperiodeSvar);

        if (anmodningsperiodeSvar.getAnmodningsperiodeSvarType() == Anmodningsperiodesvartyper.DELVIS_INNVILGELSE) {
            validerDelvisInnvilgelse(anmodningsperiodeSvar);
        }
    }

    private void validerSvartype(AnmodningsperiodeSvar anmodningsperiodeSvar) {
        if (anmodningsperiodeSvar.getAnmodningsperiodeSvarType() == null) {
            throw new FunksjonellException("Må spesifiseres svarType for svar på anmodningsperiode");
        }
    }

    private void validerDelvisInnvilgelse(AnmodningsperiodeSvar anmodningsperiodeSvar) {
        if (!anmodningsperiodeSvar.erGyldigDelvisInnvilgelse()) {
            throw new FunksjonellException("Periode må være fyllt ut ved " + Anmodningsperiodesvartyper.DELVIS_INNVILGELSE);
        }
        if (PeriodeKontroller.feilIPeriode(anmodningsperiodeSvar.getInnvilgetFom(), anmodningsperiodeSvar.getInnvilgetTom())) {
            throw new FunksjonellException("Periode er ikke gyldig");
        }
    }

    public void oppdaterAnmodetAvForBehandling(long behandlingID, String subjekt) {
        var anmodningsperiode = hentFørsteAnmodningsperiode(behandlingID);
        if (hasText(anmodningsperiode.getAnmodetAv())) {
            throw new FunksjonellException(
                "Anmodningsperiode for behandling %s er allerede anmodet av %s".formatted(behandlingID, anmodningsperiode.getAnmodetAv())
            );
        }

        anmodningsperiode.setAnmodetAv(subjekt);
        anmodningsperiodeRepository.save(anmodningsperiode);
    }
}
