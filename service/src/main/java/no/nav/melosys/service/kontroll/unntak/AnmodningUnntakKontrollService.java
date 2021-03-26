package no.nav.melosys.service.kontroll.unntak;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kontroll.AdresseUtlandKontroller;
import no.nav.melosys.service.kontroll.PersonKontroller;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AnmodningUnntakKontrollService extends AdresseUtlandKontroller {

    private final BehandlingService behandlingService;
    private final AnmodningsperiodeService anmodningsperiodeService;

    public AnmodningUnntakKontrollService(BehandlingService behandlingService, AnmodningsperiodeService anmodningsperiodeService) {
        this.behandlingService = behandlingService;
        this.anmodningsperiodeService = anmodningsperiodeService;
    }

    public Collection<Kontrollfeil> utførKontroller(long behandlingID) throws FunksjonellException, TekniskException {
        return utførKontroller(
            behandlingService.hentBehandling(behandlingID),
            anmodningsperiodeService.hentFørsteAnmodningsperiode(behandlingID),
            anmodningUnntakKontroller()
        );
    }

    private static Set<Function<AnmodningUnntakKontrollData, Kontrollfeil>> anmodningUnntakKontroller() {
        return Set.of(
            AnmodningUnntakKontrollService::bostedsadresseForOrienteringAnmodningUnntak,
            AnmodningUnntakKontrollService::anmodningsperiodeManglerSluttdato,
            kontrollData -> AdresseUtlandKontroller.arbeidsstedManglerFelter(kontrollData.getBehandlingsgrunnlagData()),
            kontrollData -> AdresseUtlandKontroller.foretakUtlandManglerFelter(kontrollData.getBehandlingsgrunnlagData())
        );
    }

    private Collection<Kontrollfeil> utførKontroller(
        Behandling behandling,
        Anmodningsperiode anmodningsperiode,
        Set<Function<AnmodningUnntakKontrollData, Kontrollfeil>> kontroller
    ) throws TekniskException {
        AnmodningUnntakKontrollData kontrollData = new AnmodningUnntakKontrollData(
            behandling.hentPersonDokument(),
            behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata(),
            anmodningsperiode
        );
        return kontroller.stream()
            .map(f -> f.apply(kontrollData))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    static Kontrollfeil bostedsadresseForOrienteringAnmodningUnntak(AnmodningUnntakKontrollData kontrollData) {
        return PersonKontroller.harRegistrertBostedsadresse(kontrollData.getPersonDokument(), kontrollData.getBehandlingsgrunnlagData())
            ? null : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_BOSTEDSADRESSE);
    }

    static Kontrollfeil anmodningsperiodeManglerSluttdato(AnmodningUnntakKontrollData kontrollData) {
        return kontrollData.getAnmodningsperiode().getTom() == null
            ? new Kontrollfeil(Kontroll_begrunnelser.INGEN_SLUTTDATO) : null;
    }
}
