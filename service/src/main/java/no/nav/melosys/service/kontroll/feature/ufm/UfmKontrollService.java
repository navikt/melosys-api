package no.nav.melosys.service.kontroll.feature.ufm;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.micrometer.core.instrument.Metrics;
import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Kontrollresultat;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.repository.KontrollresultatRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.kontroll.feature.ufm.data.UfmKontrollData;
import no.nav.melosys.service.kontroll.feature.ufm.kontroll.UfmKontrollsett;
import no.nav.melosys.service.kontroll.regler.PeriodeRegler;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.metrics.MetrikkerNavn.TAG_BEGRUNNELSE;
import static no.nav.melosys.metrics.MetrikkerNavn.UNNTAKSPERIODE_KONTROLL_TREFF;

@Service
@Primary
public class UfmKontrollService {
    private static final Logger log = LoggerFactory.getLogger(UfmKontrollService.class);

    static {
        Arrays.stream(Kontroll_begrunnelser.values())
            .forEach(b -> Metrics.counter(UNNTAKSPERIODE_KONTROLL_TREFF, TAG_BEGRUNNELSE, b.getKode()));
    }

    private final KontrollresultatRepository kontrollresultatRepository;
    private final BehandlingsresultatService behandlingsresultatService;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final BehandlingService behandlingService;
    private final PersondataFasade persondataFasade;
    private final Unleash unleash;

    public UfmKontrollService(KontrollresultatRepository kontrollresultatRepository,
                              BehandlingsresultatService behandlingsresultatService,
                              BehandlingsgrunnlagService behandlingsgrunnlagService, BehandlingService behandlingService,
                              PersondataFasade persondataFasade,
                              Unleash unleash) {
        this.kontrollresultatRepository = kontrollresultatRepository;
        this.behandlingsresultatService = behandlingsresultatService;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.behandlingService = behandlingService;
        this.persondataFasade = persondataFasade;
        this.unleash = unleash;
    }

    @Transactional
    public void utførKontrollerOgRegistrerFeil(long behandlingId) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        List<Kontroll_begrunnelser> registrerteTreff = utførKontroller(behandling);

        log.info("Treff ved validering av periode for behandling {}: {}", behandlingId, registrerteTreff);
        lagreKontrollresultater(behandlingId, registrerteTreff);
    }

    List<Kontroll_begrunnelser> utførKontroller(Behandling behandling) {
        var sedDokument = behandling.hentSedDokument();
        if (harFeilIPeriode(sedDokument)) {
            return Collections.singletonList(Kontroll_begrunnelser.FEIL_I_PERIODEN);
        }
        var ufmKontrollData = lagUfmKontrollData(behandling, sedDokument);
        var sedType = sedDokument.getSedType();
        return utførKontroller(ufmKontrollData, sedType);
    }

    private UfmKontrollData lagUfmKontrollData(Behandling behandling, SedDokument sedDokument) {
        var persondata = persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID());
        var medlemskapDokument = behandling.hentMedlemskapDokument();
        var inntektDokument = behandling.hentInntektDokument();
        var utbetalingDokument = behandling.finnUtbetalingDokument().orElse(null);
        var optionalBehandlingsgrunnlagData = behandlingsgrunnlagService.finnBehandlingsgrunnlagdata(behandling.getId());
        return new UfmKontrollData(sedDokument, persondata, medlemskapDokument, inntektDokument, utbetalingDokument, optionalBehandlingsgrunnlagData);
    }

    private List<Kontroll_begrunnelser> utførKontroller(UfmKontrollData kontrollData, SedType sedType) {
        return UfmKontrollsett.hentRegelsettForSedType(sedType, unleash).stream()
            .map(f -> f.apply(kontrollData))
            .filter(Objects::nonNull)
            .peek(this::registrerMetrikk) //NOSONAR
            .toList();
    }

    private void lagreKontrollresultater(Long behandlingID, List<Kontroll_begrunnelser> kontrollBegrunnelser) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        kontrollresultatRepository.deleteByBehandlingsresultat(behandlingsresultat);
        kontrollresultatRepository.flush();

        List<Kontrollresultat> kontrollresultater = kontrollBegrunnelser.stream()
            .map(kontrollBegrunnelse -> lagKontrollresultat(behandlingsresultat, kontrollBegrunnelse))
            .toList();

        kontrollresultatRepository.saveAll(kontrollresultater);
    }

    private Kontrollresultat lagKontrollresultat(Behandlingsresultat behandlingsresultat, Kontroll_begrunnelser kontrollBegrunnelse) {
        Kontrollresultat kontrollresultat = new Kontrollresultat();
        kontrollresultat.setBegrunnelse(kontrollBegrunnelse);
        kontrollresultat.setBehandlingsresultat(behandlingsresultat);

        return kontrollresultat;
    }

    private void registrerMetrikk(Kontroll_begrunnelser unntak_periode_begrunnelse) {
        Metrics.counter(UNNTAKSPERIODE_KONTROLL_TREFF, TAG_BEGRUNNELSE, unntak_periode_begrunnelse.getKode()).increment();
    }

    private boolean harFeilIPeriode(SedDokument sedDokument) {
        return PeriodeRegler.feilIPeriode(
            sedDokument.getLovvalgsperiode().getFom(),
            sedDokument.getLovvalgsperiode().getTom());
    }
}
