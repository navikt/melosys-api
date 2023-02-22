package no.nav.melosys.saksflyt.steg.behandling;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import no.finn.unleash.Unleash;
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
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.AVKLAR_ARBEIDSGIVER;
import static no.nav.melosys.featuretoggle.ToggleName.IKKEYRKESAKTIV_FLYT;
import static no.nav.melosys.featuretoggle.ToggleName.REGISTRERING_UNNTAK_MEDLEMSKAP;
import static no.nav.melosys.service.saksbehandling.SaksbehandlingRegler.harTomFlyt;

@Component
public class AvklarArbeidsgiver implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(AvklarArbeidsgiver.class);
    private static final Function<OrganisasjonDokument, Adresse> INGEN_ADRESSE = org -> null;

    private final AktoerService aktoerService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final Unleash unleash;

    public AvklarArbeidsgiver(AktoerService aktoerService,
                              AvklarteVirksomheterService avklarteVirksomheterService,
                              BehandlingService behandlingService,
                              BehandlingsresultatService behandlingsresultatService,
                              Unleash unleash) {
        this.aktoerService = aktoerService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.unleash = unleash;
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

        List<AvklartVirksomhet> avklarteNorskeArbeidsgivere = avklarteVirksomheterService.hentNorskeArbeidsgivere(behandling, INGEN_ADRESSE);
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
        return harTomFlyt(behandling, unleash.isEnabled("melosys.folketrygden.mvp"), unleash.isEnabled(IKKEYRKESAKTIV_FLYT), unleash.isEnabled(REGISTRERING_UNNTAK_MEDLEMSKAP))
            || resultat.erAvslagManglendeOpplysninger() || erEøsMedArtikkel13(behandling, resultat);
    }

    private static boolean erEøsMedArtikkel13(Behandling behandling, Behandlingsresultat resultat) {
        if (!behandling.getFagsak().erSakstypeEøs()) {
            return false;
        }
        Optional<Lovvalgsperiode> lovvalgsperiodeOptional = resultat.finnLovvalgsperiode();
        return lovvalgsperiodeOptional.isPresent() && lovvalgsperiodeOptional.get().erArtikkel13();
    }
}
