package no.nav.melosys.saksflyt.steg.behandling;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.adresse.Adresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.melosys.service.avklartefakta.OppsummerteAvklarteFaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.AVKLAR_ARBEIDSGIVER;

@Component
public class AvklarArbeidsgiver implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(AvklarArbeidsgiver.class);
    private static final Function<OrganisasjonDokument, Adresse> INGEN_ADRESSE = org -> null;

    private final AktoerService aktoerService;
    private final OppsummerteAvklarteFaktaService oppsummerteAvklarteFaktaService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final SaksbehandlingRegler saksbehandlingRegler;

    public AvklarArbeidsgiver(AktoerService aktoerService,
                              OppsummerteAvklarteFaktaService oppsummerteAvklarteFaktaService,
                              BehandlingService behandlingService,
                              BehandlingsresultatService behandlingsresultatService,
                              SaksbehandlingRegler saksbehandlingRegler) {
        this.aktoerService = aktoerService;
        this.oppsummerteAvklarteFaktaService = oppsummerteAvklarteFaktaService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.saksbehandlingRegler = saksbehandlingRegler;
    }

    public ProsessSteg inngangsSteg() {
        return AVKLAR_ARBEIDSGIVER;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        long behandlingID = prosessinstans.getBehandling().getId();
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);
        Behandlingsresultat resultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        if (arbeidsgiverIkkeAvklares(behandling, resultat)) {
            log.debug("Arbeidsgiver avklares ikke for behandling {}", behandling.getId());
            return;
        }

        Fagsak fagsak = behandling.getFagsak();
        String saksnummer = fagsak.getSaksnummer();

        List<AvklartVirksomhet> avklarteNorskeArbeidsgivere = oppsummerteAvklarteFaktaService.hentNorskeArbeidsgivere(behandling, INGEN_ADRESSE);
        List<String> norskeOrgnumre = avklarteNorskeArbeidsgivere.stream()
            .map(avklartVirksomhet -> avklartVirksomhet.orgnr)
            .toList();

        aktoerService.erstattEksisterendeArbeidsgiveraktører(fagsak, norskeOrgnumre);

        if (avklarteNorskeArbeidsgivere.isEmpty()) {
            log.info("Eksisterende arbeidsgiveraktør fjernet, og ingen nye lagt til for sak {}.", saksnummer);
        } else {
            log.info("Avklart arbeidsgivere lagt til for sak {}.", saksnummer);
        }
    }

    private boolean arbeidsgiverIkkeAvklares(Behandling behandling, Behandlingsresultat resultat) {
        return saksbehandlingRegler.harIngenFlyt(behandling)
            || erEøsMedArtikkel13(behandling, resultat);
    }

    private static boolean erEøsMedArtikkel13(Behandling behandling, Behandlingsresultat resultat) {
        if (!behandling.getFagsak().erSakstypeEøs()) {
            return false;
        }
        Optional<Lovvalgsperiode> lovvalgsperiodeOptional = resultat.finnLovvalgsperiode();
        return lovvalgsperiodeOptional.isPresent() && lovvalgsperiodeOptional.get().erArtikkel13();
    }
}
