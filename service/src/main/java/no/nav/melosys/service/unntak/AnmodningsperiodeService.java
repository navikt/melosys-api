package no.nav.melosys.service.unntak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.repository.AnmodningsperiodeSvarRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.regler.PeriodeRegler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.util.StringUtils.hasText;

@Service
public class AnmodningsperiodeService {
    private static final Logger log = LoggerFactory.getLogger(AnmodningsperiodeService.class);

    private final AnmodningsperiodeRepository anmodningsperiodeRepository;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final AnmodningsperiodeSvarRepository anmodningsperiodeSvarRepository;
    private final BehandlingsresultatService behandlingsresultatService;

    public AnmodningsperiodeService(AnmodningsperiodeRepository anmodningsperiodeRepository,
                                    LovvalgsperiodeService lovvalgsperiodeService, AnmodningsperiodeSvarRepository anmodningsperiodeSvarRepository,
                                    BehandlingsresultatService behandlingsresultatService) {
        this.anmodningsperiodeRepository = anmodningsperiodeRepository;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.anmodningsperiodeSvarRepository = anmodningsperiodeSvarRepository;
    }

    public Optional<Anmodningsperiode> finnAnmodningsperiode(long anmodningsperiodeID) {
        return anmodningsperiodeRepository.findById(anmodningsperiodeID);
    }

    public Collection<Anmodningsperiode> hentAnmodningsperioder(long behandlingID) {
        return anmodningsperiodeRepository.findByBehandlingsresultatId(behandlingID);
    }

    public boolean harSendtAnmodningsperiode(long behandlingID) {
        return anmodningsperiodeRepository.findByBehandlingsresultatId(behandlingID).stream()
            .anyMatch(Anmodningsperiode::erSendtUtland);
    }

    private Optional<AnmodningsperiodeSvar> finnAnmodningsperiodeSvar(long anmodningsperiodeID) {
        return anmodningsperiodeSvarRepository.findById(anmodningsperiodeID);
    }

    public AnmodningsperiodeSvar hentAnmodningsperiodeSvarForBehandling(long behandlingID) {
        return hentAnmodningsperioder(behandlingID).stream()
            .map(Anmodningsperiode::getId)
            .map(this::finnAnmodningsperiodeSvar)
            .flatMap(Optional::stream)
            .findFirst()
            .orElseThrow(() -> new FunksjonellException("Finner ingen AnmodningsperiodeSvar for behandling " + behandlingID));
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
    public AnmodningsperiodeSvar lagreAnmodningsperiodeSvarMedLovvalgsperiode(long anmodningsperiodeId, AnmodningsperiodeSvar anmodningsperiodeSvar) {
        Anmodningsperiode anmodningsperiode = anmodningsperiodeRepository.findById(anmodningsperiodeId)
            .orElseThrow(() -> new IkkeFunnetException("Anmodningsperiode med id " + anmodningsperiodeId + " finnes ikke"));

        return lagreAnmodningsperiodeSvarMedLovvalgsperiode(anmodningsperiode, anmodningsperiodeSvar);
    }

    public void lagreAnmodningsperiodeSvarForBehandling(long behandlingID, AnmodningsperiodeSvar anmodningsperiodeSvar) {
        lagreAnmodningsperiodeSvarMedLovvalgsperiode(hentFørsteAnmodningsperiode(behandlingID), anmodningsperiodeSvar);
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

    /**
     * Reparerer et manglende TWFA-flagg på anmodningsperioden. Kalles fra sendeveien når kolonnen er null, men
     * prosessdataen har verdien — se {@code SendAnmodningOmUnntak.hentErFjernarbeidTWFA}.
     * <p>
     * No-op hvis flagget allerede er satt: dette er en reparasjonssti under SED-sending, og den skal verken kunne
     * overskrive saksbehandlerens svar eller velte en A001 ved å kaste.
     * <p>
     * Eksplisitt {@code @Transactional} fordi kalleren er et saga-steg, der transaksjonen kommer fra
     * {@code @Transactional(REQUIRES_NEW)} på {@code StegBehandler}-grensesnittet i en annen modul.
     */
    @Transactional
    public void reparerManglendeErFjernarbeidTWFA(long behandlingID, boolean erFjernarbeidTWFA) {
        var anmodningsperiode = hentFørsteAnmodningsperiode(behandlingID);
        if (anmodningsperiode.getErFjernarbeidTWFA() != null) {
            log.warn("Behandling {}: er_fjernarbeid_twfa er allerede satt til {}, reparerer ikke",
                behandlingID, anmodningsperiode.getErFjernarbeidTWFA());
            return;
        }

        anmodningsperiode.setErFjernarbeidTWFA(erFjernarbeidTWFA);
        anmodningsperiodeRepository.save(anmodningsperiode);
    }

    private AnmodningsperiodeSvar lagreAnmodningsperiodeSvarMedLovvalgsperiode(Anmodningsperiode anmodningsperiode, AnmodningsperiodeSvar anmodningsperiodeSvar) {
        validerSvar(anmodningsperiodeSvar);

        if (anmodningsperiode.getAnmodningsperiodeSvar() != null) {
            anmodningsperiodeSvar = oppdaterOpprinneligSvar(anmodningsperiode.getAnmodningsperiodeSvar(), anmodningsperiodeSvar);
        }

        anmodningsperiodeSvar.setAnmodningsperiode(anmodningsperiode);
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);
        anmodningsperiodeSvarRepository.save(anmodningsperiodeSvar);

        Lovvalgsperiode lovvalgsperiode = Lovvalgsperiode.av(anmodningsperiodeSvar, Medlemskapstyper.PLIKTIG);
        lovvalgsperiodeService.lagreLovvalgsperioder(anmodningsperiode.hentBehandlingsresultatId(), Collections.singleton(lovvalgsperiode));

        return anmodningsperiodeSvar;
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
        if (PeriodeRegler.feilIPeriode(anmodningsperiodeSvar.getInnvilgetFom(), anmodningsperiodeSvar.getInnvilgetTom())) {
            throw new FunksjonellException("Periode er ikke gyldig");
        }
    }

    /**
     * Registrerer at anmodning om unntak er sendt: hvem som anmodet, og om saken behandles etter rammeavtalen om
     * fjernarbeid (TWFA).
     * <p>
     * De to feltene settes i samme operasjon med vilje. TWFA-flagget er kilde for offisiell rapportering
     * (MELOSYS-8150), og en egen metode ville kunne kalles uten sperren mot dobbel anmodning under.
     *
     * @param erFjernarbeidTWFA null når spørsmålet ikke er besvart; skilles fra et eksplisitt nei.
     */
    public void registrerAnmodning(long behandlingID, String subjekt, Boolean erFjernarbeidTWFA) {
        var anmodningsperiode = hentFørsteAnmodningsperiode(behandlingID);
        if (hasText(anmodningsperiode.getAnmodetAv())) {
            throw new FunksjonellException(
                "Anmodningsperiode for behandling %s er allerede anmodet av %s".formatted(behandlingID, anmodningsperiode.getAnmodetAv())
            );
        }

        anmodningsperiode.setAnmodetAv(subjekt);
        anmodningsperiode.setErFjernarbeidTWFA(erFjernarbeidTWFA);
        anmodningsperiodeRepository.save(anmodningsperiode);
    }
}
