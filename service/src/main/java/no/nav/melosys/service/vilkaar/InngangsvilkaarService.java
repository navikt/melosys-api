package no.nav.melosys.service.vilkaar;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.inngangsvilkar.Feilmelding;
import no.nav.melosys.domain.inngangsvilkar.InngangsvilkarResponse;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.begrunnelser.Inngangsvilkaar;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.person.Statsborgerskap;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.inngangsvilkar.InngangsvilkaarConsumer;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.VilkaarsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_INNGANGSVILKAAR;
import static no.nav.melosys.domain.util.IsoLandkodeKonverterer.tilIso3;

@Service
public class InngangsvilkaarService {

    private static final Logger log = LoggerFactory.getLogger(InngangsvilkaarService.class);
    private static final long DEFAULT_SLUTTDATO_ANTALL_ÅR = 1;

    private final BehandlingService behandlingService;
    private final InngangsvilkaarConsumer inngangsvilkaarConsumer;
    private final PersondataFasade persondataFasade;
    private final VilkaarsresultatService vilkaarsresultatService;
    private final SaksbehandlingRegler saksbehandlingRegler;

    public InngangsvilkaarService(BehandlingService behandlingService,
                                  InngangsvilkaarConsumer inngangsvilkaarConsumer,
                                  PersondataFasade persondataFasade,
                                  VilkaarsresultatService vilkaarsresultatService,
                                  SaksbehandlingRegler saksbehandlingRegler) {
        this.behandlingService = behandlingService;
        this.inngangsvilkaarConsumer = inngangsvilkaarConsumer;
        this.persondataFasade = persondataFasade;
        this.vilkaarsresultatService = vilkaarsresultatService;
        this.saksbehandlingRegler = saksbehandlingRegler;
    }

    public boolean oppfyllervurderingEF_883_2004(long behandlingID) {
        return vilkaarsresultatService.oppfyllerVilkaar(behandlingID, FO_883_2004_INNGANGSVILKAAR);
    }

    public boolean skalVurdereInngangsvilkår(Behandling behandling) {
        return behandling.getFagsak().erSakstypeEøs()
            && !saksbehandlingRegler.harIngenFlyt(behandling)
            && !saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling)
            && !saksbehandlingRegler.harIkkeYrkesaktivFlyt(behandling)
            && behandling.kanResultereIVedtak()
            && behandling.harPeriodeOgSøknadsland();
    }

    @Transactional
    public boolean vurderOgLagreInngangsvilkår(long behandlingID,
                                               Collection<String> søknadsland,
                                               boolean flereLandUkjentHvilke,
                                               ErPeriode søknadsperiode) {
        final InngangsvilkaarVurdering vurderingEF_883_2004 = vurderInngangsvilkår(behandlingID, søknadsland, flereLandUkjentHvilke, søknadsperiode);
        final boolean erEF_883_2004 = vurderingEF_883_2004.isOppfylt();

        vilkaarsresultatService.oppdaterVilkaarsresultat(behandlingID,
            FO_883_2004_INNGANGSVILKAAR,
            erEF_883_2004,
            vurderingEF_883_2004.getBegrunnelseKode() == null ? Collections.emptySet() : Set.of(vurderingEF_883_2004.getBegrunnelseKode()));
        return erEF_883_2004;
    }

    private InngangsvilkaarVurdering vurderInngangsvilkår(long behandlingID,
                                                          Collection<String> søknadsland,
                                                          boolean flereLandUkjentHvilke,
                                                          ErPeriode søknadsperiode) {
        Set<Land> statsborgerskap = hentStatsborgerskapForPerioden(behandlingID, søknadsperiode);
        if (statsborgerskap.isEmpty()) {
            return new InngangsvilkaarVurdering(false, Inngangsvilkaar.MANGLER_STATSBORGERSKAP);
        }
        if (søknadsperiode.getTom() == null) {
            søknadsperiode = new Periode(søknadsperiode.getFom(), søknadsperiode.getFom().plusYears(DEFAULT_SLUTTDATO_ANTALL_ÅR));
        }

        var landkoderISO3 = Set.copyOf(tilIso3(søknadsland));
        InngangsvilkarResponse res = inngangsvilkaarConsumer.vurderInngangsvilkår(statsborgerskap, landkoderISO3, flereLandUkjentHvilke, søknadsperiode);

        List<String> feilmeldinger = res.getFeilmeldinger().stream().map(Feilmelding::getMelding).toList();

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
        final var behandling = behandlingService.hentBehandling(behandlingID);
        final String aktørID = behandling.getFagsak().hentBrukersAktørID();
        return avgjørGyldigeStatsborgerskapForPerioden(persondataFasade.hentStatsborgerskap(aktørID), periode);
    }

    Set<Land> avgjørGyldigeStatsborgerskapForPerioden(Set<Statsborgerskap> statsborgerskap, ErPeriode periode) {
        return statsborgerskap.stream().filter(s -> erGyldigStatsborgerskapForPeriode(s, periode))
            .map(s -> Land.av(s.landkode())).collect(Collectors.toUnmodifiableSet());
    }

    private boolean erGyldigStatsborgerskapForPeriode(Statsborgerskap s, ErPeriode periode) {
        final LocalDate søknadPeriodeFom = periode.getFom();
        if (s.master().equals("PDL")) {
            return s.erGyldigPåDato(søknadPeriodeFom) || s.erBekreftetPåDato(LocalDate.now());
        }
        return s.erGyldigPåDato(søknadPeriodeFom);
    }

    @Transactional
    public void overstyrInngangsvilkårTilOppfylt(long behandlingID) {
        final var inngangsvilkaar = vilkaarsresultatService.finnVilkaarsresultat(behandlingID, FO_883_2004_INNGANGSVILKAAR);
        if (inngangsvilkaar.isEmpty()) {
            throw new FunksjonellException("Inngangsvilkår er ikke vurdert for behandling " + behandlingID);
        }
        var behandling = behandlingService.hentBehandling(behandlingID);
        if (!behandling.harPeriodeOgSøknadsland()) {
            throw new FunksjonellException("Mangler land eller periode for behandling " + behandlingID);
        }
        final var inngangsvilkaarBegrunnelseKoder = inngangsvilkaar.get().getBegrunnelser().stream()
            .map(VilkaarBegrunnelse::getKode)
            .map(Inngangsvilkaar::valueOf)
            .collect(Collectors.toSet());

        final Set<Kodeverk> begrunnelseKoder = Stream.concat(
            Stream.of(Inngangsvilkaar.OVERSTYRT_AV_SAKSBEHANDLER),
            inngangsvilkaarBegrunnelseKoder.stream()
        ).collect(Collectors.toSet());

        vilkaarsresultatService.oppdaterVilkaarsresultat(behandlingID, FO_883_2004_INNGANGSVILKAAR, true, begrunnelseKoder);
    }
}
