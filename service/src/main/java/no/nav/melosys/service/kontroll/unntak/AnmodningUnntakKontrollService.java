package no.nav.melosys.service.kontroll.unntak;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kontroll.AdresseUtlandKontroller;
import no.nav.melosys.service.kontroll.PersonKontroller;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.springframework.stereotype.Component;

@Component
public class AnmodningUnntakKontrollService implements AdresseUtlandKontroller {

    private final AnmodningsperiodeService anmodningsperiodeService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final BehandlingService behandlingService;
    private final PersondataFasade persondataFasade;

    public AnmodningUnntakKontrollService(AnmodningsperiodeService anmodningsperiodeService,
                                          AvklarteVirksomheterService avklarteVirksomheterService, BehandlingService behandlingService,
                                          PersondataFasade persondataFasade) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.behandlingService = behandlingService;
        this.persondataFasade = persondataFasade;
    }

    public Collection<Kontrollfeil> utførKontroller(long behandlingID) {
        return utførKontroller(
            behandlingService.hentBehandlingMedSaksopplysninger(behandlingID),
            anmodningsperiodeService.hentFørsteAnmodningsperiode(behandlingID),
            anmodningUnntakKontroller()
        );
    }

    private static Set<Function<AnmodningUnntakKontrollData, Kontrollfeil>> anmodningUnntakKontroller() {
        return Set.of(
            AnmodningUnntakKontrollService::harRegistrertAdresse,
            AnmodningUnntakKontrollService::anmodningsperiodeManglerSluttdato,
            AnmodningUnntakKontrollService::kunEnArbeidsgiver,
            kontrollData -> AdresseUtlandKontroller.arbeidsstedManglerFelter(kontrollData.getBehandlingsgrunnlagData()),
            kontrollData -> AdresseUtlandKontroller.foretakUtlandManglerFelter(kontrollData.getBehandlingsgrunnlagData())
        );
    }

    private Collection<Kontrollfeil> utførKontroller(
        Behandling behandling,
        Anmodningsperiode anmodningsperiode,
        Set<Function<AnmodningUnntakKontrollData, Kontrollfeil>> kontroller
    ) {
        final var persondata = hentPersondata(behandling);
        final int antallArbeidsgivere = avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling);
        AnmodningUnntakKontrollData kontrollData = new AnmodningUnntakKontrollData(persondata,
            behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata(),
            anmodningsperiode,
            antallArbeidsgivere
        );
        return kontroller.stream()
            .map(f -> f.apply(kontrollData))
            .filter(Objects::nonNull)
            .toList();
    }

    private Persondata hentPersondata(Behandling behandling) {
        return persondataFasade.hentPerson(behandling.getFagsak().hentAktørID());
    }

    static Kontrollfeil harRegistrertAdresse(AnmodningUnntakKontrollData kontrollData) {
        return PersonKontroller.harRegistrertAdresse(kontrollData.getPersonDokument(), kontrollData.getBehandlingsgrunnlagData())
            ? null : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE);
    }

    static Kontrollfeil anmodningsperiodeManglerSluttdato(AnmodningUnntakKontrollData kontrollData) {
        return kontrollData.getAnmodningsperiode().getTom() == null
            ? new Kontrollfeil(Kontroll_begrunnelser.INGEN_SLUTTDATO) : null;
    }
    static Kontrollfeil kunEnArbeidsgiver(AnmodningUnntakKontrollData kontrollData) {
        return (kontrollData.getAntallArbeidsgivere() != 1)
            ? new Kontrollfeil(Kontroll_begrunnelser.IKKE_KUN_EN_VIRKSOMHET) : null;
    }
}
