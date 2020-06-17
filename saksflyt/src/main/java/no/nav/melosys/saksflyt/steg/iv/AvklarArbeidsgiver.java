package no.nav.melosys.saksflyt.steg.iv;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.adresse.Adresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.*;
import static no.nav.melosys.domain.saksflyt.ProsessType.IVERKSETT_VEDTAK_FORKORT_PERIODE;

/**
 * Oppdaterer aktør med avklart arbeidsgiver i saken.
 */
@Component
public class AvklarArbeidsgiver extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(AvklarArbeidsgiver.class);
    private static final Function<OrganisasjonDokument, Adresse> INGEN_ADRESSE = org -> null;

    private final AktoerService aktoerService;
    private final AvklarteVirksomheterService avklarteVirksomheterSystemService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public AvklarArbeidsgiver(AktoerService aktoerService,
                              AvklarteVirksomheterSystemService avklarteVirksomheterService,
                              BehandlingService behandlingService,
                              BehandlingsresultatService behandlingsresultatService) {
        this.aktoerService = aktoerService;
        this.avklarteVirksomheterSystemService = avklarteVirksomheterService;
        this.behandlingService = behandlingService;
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
            long behandlingID = prosessinstans.getBehandling().getId();
            Behandling behandling = behandlingService.hentBehandling(behandlingID);
            Fagsak fagsak = behandling.getFagsak();
            String saksnummer = fagsak.getSaksnummer();

            List<AvklartVirksomhet> avklarteNorskeArbeidsgivere = avklarteVirksomheterSystemService.hentNorskeArbeidsgivere(behandling, INGEN_ADRESSE);
            List<String> norskeOrgnumre = avklarteNorskeArbeidsgivere.stream()
                .map(avklartVirksomhet -> avklartVirksomhet.orgnr)
                .collect(Collectors.toList());

            aktoerService.erstattEksisterendeArbeidsgiveraktører(fagsak, norskeOrgnumre);

            if (avklarteNorskeArbeidsgivere.isEmpty()) {
                log.info("Eksisterende arbeidsgiveraktør fjernet, og ingen nye lagt til for sak {}.", saksnummer);
            } else {
                log.info("Avklart arbeidsgivere lagt til for sak {}.", saksnummer);
            }
        }

        prosessinstans.setSteg(IV_OPPDATER_MEDL);
    }

    // Ved forkortet periode har allerede arbeidsgiver blitt avklart
    private static boolean arbeidsgiverAvklares(ProsessType prosessType, Behandlingsresultat resultat) {
        return resultat.getType() == Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL ||
            !(prosessType == IVERKSETT_VEDTAK_FORKORT_PERIODE || resultat.hentValidertLovvalgsperiode().erArtikkel13());
    }
}