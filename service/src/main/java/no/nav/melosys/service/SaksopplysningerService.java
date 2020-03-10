package no.nav.melosys.service;

import java.time.Instant;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
public class SaksopplysningerService {
    private static final Logger log = LoggerFactory.getLogger(SaksopplysningerService.class);

    private final TpsFasade tpsFasade;
    private final ProsessinstansService prosessinstansService;
    private final BehandlingRepository behandlingRepository;
    private final BehandlingsresultatService behandlingsresultatService;
    private final SaksopplysningRepository saksopplysningRepo;


    @Autowired
    public SaksopplysningerService(TpsFasade tpsFasade,
                                   ProsessinstansService prosessinstansService,
                                   BehandlingRepository behandlingRepository,
                                   BehandlingsresultatService behandlingsresultatService,
                                   SaksopplysningRepository saksopplysningRepo) {
        this.tpsFasade = tpsFasade;
        this.prosessinstansService = prosessinstansService;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatService = behandlingsresultatService;
        this.saksopplysningRepo = saksopplysningRepo;
    }

    public Optional<PersonDokument> finnPersonOpplysninger(long behandlingID) {
        return saksopplysningRepo.findByBehandling_IdAndType(behandlingID, SaksopplysningType.PERSOPL)
            .map(s -> (PersonDokument) s.getDokument());
    }

    public PersonDokument hentPersonOpplysninger(long behandlingID) throws IkkeFunnetException {
        return finnPersonOpplysninger(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke persondokument for behandling " + behandlingID));
    }

    public Optional<SedDokument> finnSedOpplysninger(long behandlingID) {
        return saksopplysningRepo.findByBehandling_IdAndType(behandlingID, SaksopplysningType.SEDOPPL)
            .map(s -> (SedDokument) s.getDokument());
    }

    public SedDokument hentSedOpplysninger(long behandlingID) throws IkkeFunnetException {
        return finnSedOpplysninger(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke seddokument for behandling " + behandlingID));
    }

    public Optional<ArbeidsforholdDokument> finnArbeidsforholdsopplysninger(long behandlingID) {
        return saksopplysningRepo.findByBehandling_IdAndType(behandlingID, SaksopplysningType.ARBFORH)
            .map(s -> (ArbeidsforholdDokument) s.getDokument());
    }

    public Optional<InntektDokument> finnInntektsopplysninger(long behandlingID) {
        return saksopplysningRepo.findByBehandling_IdAndType(behandlingID, SaksopplysningType.INNTK)
            .map(s -> (InntektDokument) s.getDokument());
    }

    public PersonhistorikkDokument hentPersonhistorikk(long behandlingID) throws IkkeFunnetException {
        return saksopplysningRepo.findByBehandling_IdAndType(behandlingID, SaksopplysningType.PERSHIST)
            .map(s -> (PersonhistorikkDokument) s.getDokument())
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke personhistorikkDokument for behandling " + behandlingID));
    }

    /***
     * Metoden sjekker om en behandling med ID {@code behandlingID} har en oppfrisking i gang.
     * Oppfrisking betyr å hente saksopplysninger på nytt for en gitt behandling.
     */
    public boolean harAktivOppfrisking(Long behandlingID) {
        Assert.notNull(behandlingID, "behandlingID må ikke være null");
        return prosessinstansService.erUnderOppfriskning(behandlingID);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void oppfriskSaksopplysning(long behandlingID) throws IkkeFunnetException, TekniskException {
        log.info("Starter oppfrisking av behandlingID: {} ", behandlingID);

        if (prosessinstansService.harAktivProsessinstans(behandlingID)) {
            log.warn("Aktiv prosessinstans finnes allerede. Ikke mulig å oppfriske saksopplysning.");
            return;
        }

        Behandling behandling = behandlingRepository.findWithSaksopplysningerById(behandlingID);
        if (behandling == null) {
            log.error("Behandling ikke funnet med behandlingID {}", behandlingID);
            throw new IkkeFunnetException("Behandling ikke funnet med behandlingID: " + behandlingID);
        }


        behandling.getSaksopplysninger().clear();
        behandlingRepository.save(behandling);

        behandlingsresultatService.tømBehandlingsresultat(behandlingID);

        opprettOppfriskningsprosess(behandling);
    }

    private void opprettOppfriskningsprosess(Behandling behandling) throws IkkeFunnetException, TekniskException {
        String aktørID = behandling.getFagsak().hentBruker().getAktørId();
        String brukerID = tpsFasade.hentIdentForAktørId(aktørID);
        prosessinstansService.opprettProsessinstansOppfriskning(behandling, aktørID, brukerID);
    }

    public void lagreSaksopplysning(Saksopplysning saksopplysning, Behandling behandling) {
        Instant nå = Instant.now();
        saksopplysning.setBehandling(behandling);
        saksopplysning.setRegistrertDato(nå);
        saksopplysning.setEndretDato(nå);
        saksopplysningRepo.save(saksopplysning);
    }
}