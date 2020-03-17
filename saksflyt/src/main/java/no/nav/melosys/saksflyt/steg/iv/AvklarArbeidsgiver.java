package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.steg.AbstraktAvklarArbeidsgiveraktoer;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.*;
import static no.nav.melosys.domain.saksflyt.ProsessType.IVERKSETT_VEDTAK_FORKORT_PERIODE;

/**
 * Oppdaterer aktør med avklart arbeidsgiver i saken.
 *
 * Transisjoner:
 *  IV_AVKLAR_ARBEIDSGIVER -> IV_OPPDATER_MEDL eller FEILET_MASKINELT hvis feil
 */
@Component("IverksettVedtakAvklarArbeidsgiver")
public class AvklarArbeidsgiver extends AbstraktAvklarArbeidsgiveraktoer {
    private static final Logger log = LoggerFactory.getLogger(AvklarArbeidsgiver.class);

    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public AvklarArbeidsgiver(AktoerService aktoerService,
                              AvklarteVirksomheterSystemService avklarteVirksomheterService,
                              BehandlingRepository behandlingRepository,
                              BehandlingsresultatService behandlingsresultatService) {
        super(aktoerService, avklarteVirksomheterService, behandlingRepository);
        this.behandlingsresultatService = behandlingsresultatService;

        log.info("AvklarArbeidsgiver initialisert");
    }

    public ProsessSteg inngangsSteg() {
        return IV_AVKLAR_ARBEIDSGIVER;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandlingsresultat resultat = behandlingsresultatService.hentBehandlingsresultat(prosessinstans.getBehandling().getId());
        ProsessType prosessType = prosessinstans.getType();
        if (arbeidsgiverAvklares(prosessType, resultat)) {
            super.utfør(prosessinstans);
        }

        if (resultat.medlOppdateres()) {
            prosessinstans.setSteg(IV_OPPDATER_MEDL);
        } else {
            prosessinstans.setSteg(IV_SEND_BREV);
        }
    }

    // Ved forkortet periode har allerede arbeidsgiver blitt avklart
    private static boolean arbeidsgiverAvklares(ProsessType prosessType, Behandlingsresultat resultat) {
        return resultat.getType() == Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL ||
            (prosessType != IVERKSETT_VEDTAK_FORKORT_PERIODE && !resultat.hentValidertLovvalgsperiode().erArtikkel13());
    }
}