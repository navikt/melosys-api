package no.nav.melosys.service.vilkaar;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.StatsborgerskapPeriode;
import no.nav.melosys.domain.inngangsvilkar.Feilmelding;
import no.nav.melosys.domain.inngangsvilkar.InngangsvilkarResponse;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.begrunnelser.Inngangsvilkaar;
import no.nav.melosys.domain.person.Statsborgerskap;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.inngangsvilkar.InngangsvilkaarConsumer;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_INNGANGSVILKAAR;
import static no.nav.melosys.domain.util.LandkoderUtils.tilIso3;
import static no.nav.melosys.service.registeropplysninger.RegisteropplysningerPeriodeFactory.REGISTEROPPLYSNINGER_DEFAULT_SLUTTDATO_ANTALL_ÅR;

@Service
public class InngangsvilkaarService {
    private static final Logger log = LoggerFactory.getLogger(InngangsvilkaarService.class);

    private final BehandlingService behandlingService;
    private final InngangsvilkaarConsumer inngangsvilkaarConsumer;
    private final PersondataFasade persondataFasade;
    private final SaksopplysningerService saksopplysningerService;
    private final Unleash unleash;
    private final VilkaarsresultatService vilkaarsresultatService;

    @Autowired
    public InngangsvilkaarService(BehandlingService behandlingService,
                                  InngangsvilkaarConsumer inngangsvilkaarConsumer,
                                  @Qualifier("system") PersondataFasade persondataFasade,
                                  SaksopplysningerService saksopplysningerService,
                                  Unleash unleash,
                                  VilkaarsresultatService vilkaarsresultatService) {
        this.behandlingService = behandlingService;
        this.inngangsvilkaarConsumer = inngangsvilkaarConsumer;
        this.persondataFasade = persondataFasade;
        this.saksopplysningerService = saksopplysningerService;
        this.unleash = unleash;
        this.vilkaarsresultatService = vilkaarsresultatService;
    }

    public boolean oppfyllervurderingEF_883_2004(long behandlingID) {
        return vilkaarsresultatService.oppfyllerVilkaar(behandlingID, FO_883_2004_INNGANGSVILKAAR);
    }

    public boolean vurderOgLagreInngangsvilkår(long behandlingID,
                                               Collection<String> søknadsland,
                                               ErPeriode søknadsperiode) {
        final InngangsvilkaarVurdering vurderingEF_883_2004 = vurderInngangsvilkår(behandlingID, søknadsland, søknadsperiode);
        final boolean erEF_883_2004 = vurderingEF_883_2004.isOppfylt();

        vilkaarsresultatService.oppdaterVilkaarsresultat(behandlingID, FO_883_2004_INNGANGSVILKAAR,
            erEF_883_2004,
            vurderingEF_883_2004.getBegrunnelseKode() == null ? Collections.emptySet() : Set.of(vurderingEF_883_2004.getBegrunnelseKode()));
        return erEF_883_2004;
    }

    private InngangsvilkaarVurdering vurderInngangsvilkår(long behandlingID,
                                                          Collection<String> søknadsland,
                                                          ErPeriode søknadsperiode) {
        Set<Land> statsborgerskap = hentStatsborgerskapForPerioden(behandlingID, søknadsperiode);
        if (statsborgerskap.isEmpty()) {
            return new InngangsvilkaarVurdering(false, Inngangsvilkaar.MANGLER_STATSBORGERSKAP);
        }
        if (søknadsperiode.getTom() == null) {
            søknadsperiode = new Periode(søknadsperiode.getFom(), søknadsperiode.getFom().plusYears(REGISTEROPPLYSNINGER_DEFAULT_SLUTTDATO_ANTALL_ÅR));
        }

        var landkoderISO3 = Set.copyOf(tilIso3(søknadsland));
        InngangsvilkarResponse res = inngangsvilkaarConsumer.vurderInngangsvilkår(statsborgerskap, landkoderISO3, søknadsperiode);

        List<String> feilmeldinger = res.getFeilmeldinger().stream().map(Feilmelding::getMelding).collect(Collectors.toList());

        if (!feilmeldinger.isEmpty()) {
            if (log.isErrorEnabled()) {
                log.error("Vurdering av inngangsvilkår feilet: {}", String.join(System.lineSeparator(), feilmeldinger));
            }
            return new InngangsvilkaarVurdering(false, Inngangsvilkaar.TEKNISK_FEIL);
        } else {
            // Vurdering fra regelmodul gir ikke begrunnelser så langt.
            return new InngangsvilkaarVurdering(res.getKvalifisererForEf883_2004(), null);
        }
    }

    private Set<Land> hentStatsborgerskapForPerioden(long behandlingID, ErPeriode periode) {
        if (unleash.isEnabled("melosys.pdl.statsborgerskap")) {
            final var behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
            final String brukerIdent = behandling.getFagsak().hentBruker().getAktørId();
            return avgjørGyldigeStatsborgerskapFraPdlForPerioden(persondataFasade.hentStatsborgerskap(brukerIdent), periode);
        }
        return Optional.ofNullable(avgjørStatsborgerskapFraTPS(behandlingID, periode))
            .stream().collect(Collectors.toUnmodifiableSet());
    }

    Set<Land> avgjørGyldigeStatsborgerskapFraPdlForPerioden(Set<Statsborgerskap> statsborgerskap, ErPeriode periode) {
        return statsborgerskap.stream().filter(s -> erGyldigStatsborgerskapForPeriode(s, periode))
            .map(s -> Land.av(s.land())).collect(Collectors.toUnmodifiableSet());
    }

    private boolean erGyldigStatsborgerskapForPeriode(Statsborgerskap s, ErPeriode periode) {
        final LocalDate søknadPeriodeFom = periode.getFom();
        if (s.master().equals("PDL")) {
            return s.erGyldigPåDato(søknadPeriodeFom) || s.erBekreftetPåDato(LocalDate.now());
        }
        return s.erGyldigPåDato(søknadPeriodeFom);
    }

    private Land avgjørStatsborgerskapFraTPS(long behandlingID, ErPeriode periode) {
        // Hent statsborgerskap fra saksopplysningene...
        // Ved søknad tilbake i tid brukes historisk statsborgerskap
        if (periode.getFom().isBefore(LocalDate.now())) {
            return avgjørStatsborgerskapPåStartDato(
                saksopplysningerService.hentPersonhistorikk(behandlingID).statsborgerskapListe, periode.getFom());
        } else {
            return saksopplysningerService.hentPersonOpplysninger(behandlingID).hentAlleStatsborgerskap().stream()
                .findFirst().orElse(null);
        }
    }

    Land avgjørStatsborgerskapPåStartDato(List<StatsborgerskapPeriode> statsborgerskapListe, LocalDate startDato) {
        if (statsborgerskapListe.isEmpty()) {
            return null;
        }
        List<StatsborgerskapPeriode> gyldigeStasborgerskap = statsborgerskapListe.stream()
            .filter(p -> p.getPeriode().inkluderer(startDato))
            .collect(Collectors.toList());
        if (gyldigeStasborgerskap.isEmpty()) {
            return null;
        } else if (gyldigeStasborgerskap.size() == 1) {
            return gyldigeStasborgerskap.get(0).statsborgerskap;
        } else {
            // Hvis det finnes flere kilder for samme dato så ønsker vi å se bort fra det som kommer fra Skattedirektoratet
            // pga. dårlig datakvalitet. Vi filterer også ukjent statsborgerskap siden det ikke hjelper å vurdere inngangsvilkår.
            return gyldigeStasborgerskap.stream().filter(p -> !p.erFraSkattedirektoratet())
                .filter(p -> !p.statsborgerskap.erUkjent())
                .max(Comparator.comparing(p -> p.endringstidspunkt))
                .map(p -> p.statsborgerskap).orElse(null);
        }
    }

    @Transactional
    public void overstyrInngangsvilkårTilOppfylt(long behandlingID) {
        final var inngangsvilkaar = vilkaarsresultatService.finnVilkaarsresultat(behandlingID, FO_883_2004_INNGANGSVILKAAR);
        if (inngangsvilkaar.isEmpty()) {
            throw new FunksjonellException("Inngangsvilkår er ikke vurdert for behandling " + behandlingID);
        }
        final var inngangsvilkaarBegrunnelseKoder = inngangsvilkaar.get().getBegrunnelser().stream()
            .map(VilkaarBegrunnelse::getKode)
            .map(Inngangsvilkaar::valueOf)
            .collect(Collectors.toSet());

        final Set<Kodeverk> begrunnelseKoder = Stream.concat(
            Set.of(Inngangsvilkaar.OVERSTYRT_AV_SAKSBEHANDLER).stream(),
            inngangsvilkaarBegrunnelseKoder.stream()
        ).collect(Collectors.toSet());

        vilkaarsresultatService.oppdaterVilkaarsresultat(behandlingID, FO_883_2004_INNGANGSVILKAAR, true, begrunnelseKoder);
    }
}
