package no.nav.melosys.service.unntak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.AnmodningsperiodeSvarType;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.repository.AnmodningsperiodeSvarRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.LovvalgsperiodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnmodningsperiodeService {
    private final AnmodningsperiodeRepository anmodningsperiodeRepository;
    private final BehandlingsresultatService behandlingsresultatService;
    private final AnmodningsperiodeSvarRepository anmodningsperiodeSvarRepository;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    @Autowired
    public AnmodningsperiodeService(AnmodningsperiodeRepository anmodningsperiodeRepository, BehandlingsresultatService behandlingsresultatService, AnmodningsperiodeSvarRepository anmodningsperiodeSvarRepository, LovvalgsperiodeService lovvalgsperiodeService) {
        this.anmodningsperiodeRepository = anmodningsperiodeRepository;
        this.behandlingsresultatService = behandlingsresultatService;
        this.anmodningsperiodeSvarRepository = anmodningsperiodeSvarRepository;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
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

    @Transactional(rollbackFor = MelosysException.class)
    public AnmodningsperiodeSvar lagreAnmodningsperiodeSvar(long anmodningsperiodeId, AnmodningsperiodeSvar anmodningsperiodeSvar) throws FunksjonellException {
        Anmodningsperiode anmodningsperiode = anmodningsperiodeRepository.findById(anmodningsperiodeId)
            .orElseThrow(() -> new IkkeFunnetException("Anmodningsperiode med id " + anmodningsperiodeId + " finnes ikke"));

        return lagreAnmodningsperiodeSvar(anmodningsperiode, anmodningsperiodeSvar);
    }

    public void lagreAnmodningsperiodeSvarForBehandling(long behandlingID, AnmodningsperiodeSvar anmodningsperiodeSvar) throws FunksjonellException {
        lagreAnmodningsperiodeSvar(hentFørsteAnmodningsperiode(behandlingID), anmodningsperiodeSvar);
    }

    public void oppdaterAnmodningsperiodeSendtForBehandling(long behandlingID) throws FunksjonellException {
        Anmodningsperiode anmodningsperiode = hentFørsteAnmodningsperiode(behandlingID);
        anmodningsperiode.setSendtUtland(true);
        anmodningsperiodeRepository.save(anmodningsperiode);
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
            throw new FunksjonellException("Forventet èn anmodningsperiode på behandling" + behandlingID + ", fant " + anmodningsperioder.size());
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
            throw new FunksjonellException("Periode må være fyllt ut ved " + AnmodningsperiodeSvarType.DELVIS_INNVILGELSE);
        }
    }

    public Collection<Lovvalgsperiode> opprettLovvalgsperiodeFraAnmodningsperiode(long behandlingID,
                                                                                  Medlemskapstyper medlemskapstype) throws TekniskException, FunksjonellException {
        final Anmodningsperiode anmodningsperiode = anmodningsperiodeRepository.findByBehandlingsresultatId(behandlingID).stream().findFirst()
            .orElseThrow(() -> new TekniskException("Finner ingen anmodningsperiode for behandling " + behandlingID));
        return opprettLovvalgsperiodeFraAnmodningsperiode(anmodningsperiode, medlemskapstype);
    }

    public Collection<Lovvalgsperiode> opprettLovvalgsperiodeFraAnmodningsperiode(Anmodningsperiode anmodningsperiode,
                                                                                  Medlemskapstyper medlemskapstype) throws FunksjonellException {
        Lovvalgsperiode lovvalgsperiode = opprettLovvalgsperiode(anmodningsperiode, medlemskapstype);
        return lovvalgsperiodeService.lagreLovvalgsperioder(anmodningsperiode.getBehandlingsresultat().getId(), Collections.singleton(lovvalgsperiode));
    }

    private Lovvalgsperiode opprettLovvalgsperiode(Anmodningsperiode anmodningsperiode,
                                                   Medlemskapstyper medlemskapstype) throws FunksjonellException {

        AnmodningsperiodeSvar anmodningsperiodeSvar = anmodningsperiode.getAnmodningsperiodeSvar();

        if (anmodningsperiodeSvar == null) {
            throw new FunksjonellException("Kan ikke opprette lovvalgsperiode fra anmodningsperiode " +
                "uten at et svar er registrert!");
        }

        InnvilgelsesResultat innvilgelsesResultat = anmodningsperiodeSvar.getAnmodningsperiodeSvarType() == AnmodningsperiodeSvarType.AVSLAG ?
            InnvilgelsesResultat.AVSLAATT : InnvilgelsesResultat.INNVILGET;

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(anmodningsperiode.getBestemmelse());

        if (anmodningsperiodeSvar.getAnmodningsperiodeSvarType() == AnmodningsperiodeSvarType.DELVIS_INNVILGELSE) {
            lovvalgsperiode.setFom(anmodningsperiodeSvar.getInnvilgetFom());
            lovvalgsperiode.setTom(anmodningsperiodeSvar.getInnvilgetTom());
        } else {
            lovvalgsperiode.setFom(anmodningsperiode.getFom());
            lovvalgsperiode.setTom(anmodningsperiode.getTom());
        }

        lovvalgsperiode.setInnvilgelsesresultat(innvilgelsesResultat);
        lovvalgsperiode.setMedlemskapstype(medlemskapstype);
        lovvalgsperiode.setMedlPeriodeID(anmodningsperiode.getMedlPeriodeID());
        lovvalgsperiode.setTilleggsbestemmelse(anmodningsperiode.getTilleggsbestemmelse());
        lovvalgsperiode.setLovvalgsland(anmodningsperiode.getLovvalgsland());
        lovvalgsperiode.setDekning(anmodningsperiode.getDekning());
        return lovvalgsperiode;
    }
}
