package no.nav.melosys.saksflyt.steg.behandling;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.adresse.Adresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
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

@Component
public class AvklarArbeidsgiver implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(AvklarArbeidsgiver.class);
    private static final Function<OrganisasjonDokument, Adresse> INGEN_ADRESSE = org -> null;

    private final AktoerService aktoerService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;

    public AvklarArbeidsgiver(AktoerService aktoerService,
                              AvklarteVirksomheterService avklarteVirksomheterService,
                              BehandlingService behandlingService,
                              BehandlingsresultatService behandlingsresultatService) {
        this.aktoerService = aktoerService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    public ProsessSteg inngangsSteg() {
        return AVKLAR_ARBEIDSGIVER;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {

        Behandlingsresultat resultat = behandlingsresultatService.hentBehandlingsresultat(prosessinstans.getBehandling().getId());
        if (arbeidsgiverAvklares(resultat)) {
            long behandlingID = prosessinstans.getBehandling().getId();
            Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);
            Fagsak fagsak = behandling.getFagsak();
            String saksnummer = fagsak.getSaksnummer();

            List<AvklartVirksomhet> avklarteNorskeArbeidsgivere = avklarteVirksomheterService.hentNorskeArbeidsgivere(behandling, INGEN_ADRESSE);
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
    }

    private static boolean arbeidsgiverAvklares(Behandlingsresultat resultat) {
        return resultat.getType() == Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL ||
            !resultat.hentLovvalgsperiode().erArtikkel13();
    }
}
