package no.nav.melosys.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.util.BehandlingsgrunnlagUtils.*;

@Service
public class LandvelgerService {

    private final AvklartefaktaService avklartefaktaService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;

    public LandvelgerService(AvklartefaktaService avklartefaktaService,
                             BehandlingsresultatService behandlingsresultatService,
                             BehandlingsgrunnlagService behandlingsgrunnlagService) {
        this.avklartefaktaService = avklartefaktaService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
    }

    public Land_iso2 hentArbeidsland(long behandlingID) {
        Collection<Land_iso2> alleArbeidsland = hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);
        if (alleArbeidsland.size() != 1) {
            throw new FunksjonellException("Fant ingen eller flere enn ett arbeidsland");
        }
        return alleArbeidsland.iterator().next();
    }

    public Collection<Land_iso2> hentAlleArbeidsland(long behandlingID) {
        Collection<Land_iso2> alleArbeidsland = avklartefaktaService.hentAlleAvklarteArbeidsland(behandlingID);
        if (alleArbeidsland.isEmpty() || erArtikkel13(behandlingID)) {
            Behandlingsgrunnlag behandlingsgrunnlag =  behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID);
            BehandlingsgrunnlagData grunnlagData = behandlingsgrunnlag.getBehandlingsgrunnlagdata();
            Behandling behandling = behandlingsgrunnlag.getBehandling();
            var søknadsland = grunnlagData.soeknadsland;

            if (behandling.erAnmodningOmUnntak() && søknadsland.landkoder.isEmpty()) {
                alleArbeidsland.add(Land_iso2.valueOf(behandling.hentSedDokument().getUnntakFraLovvalgslandKode().getKode()));
            } else {
                alleArbeidsland.addAll(hentSøknadslandkoder(grunnlagData));
            }
        }

        return alleArbeidsland;
    }

    public boolean erUkjenteEllerAlleEosLand(long behandlingID) {
        BehandlingsgrunnlagData grunnlagData = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID).getBehandlingsgrunnlagdata();
        return hentSøknadsland(grunnlagData).erUkjenteEllerAlleEosLand;
    }

    public Collection<Land_iso2> hentAlleArbeidslandUtenMarginaltArbeid(long behandlingID) {
        Collection<Land_iso2> alleArbeidsland = hentAlleArbeidsland(behandlingID);
        Collection<Land_iso2> landMedMarginaltArbeid = avklartefaktaService.hentLandkoderMedMarginaltArbeid(behandlingID);
        alleArbeidsland.removeAll(landMedMarginaltArbeid);

        return alleArbeidsland;
    }

    private boolean erArtikkel13(long behandlingId) {
        return erArtikkel13(behandlingsresultatService.hentBehandlingsresultat(behandlingId));
    }

    private boolean erArtikkel13(Behandlingsresultat behandlingsresultat) {
        if (behandlingsresultat.harPeriodeOmLovvalg()) {
            PeriodeOmLovvalg periodeOmLovvalg = behandlingsresultat.hentValidertPeriodeOmLovvalg();
            return periodeOmLovvalg.erArtikkel13();
        } else {
            return erVideresendt(behandlingsresultat);
        }
    }

    private boolean erArtikkel11_3aMed11_5Tilleggsbestemmelse(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.finnLovvalgsperiode()
            .filter(Lovvalgsperiode::erArtikkel11_3aMed11_5Tilleggsbestemmelse)
            .isPresent();
    }

    private boolean erVideresendt(Behandlingsresultat behandlingsresultat) {
        Fagsak fagsak = behandlingsresultat.getBehandling().getFagsak();
        return fagsak.getStatus() == Saksstatuser.VIDERESENDT;
    }

    public Collection<Land_iso2> hentUtenlandskTrygdemyndighetsland(long behandlingID) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        if (erArtikkel13(behandlingsresultat) && !erVideresendt(behandlingsresultat)) {
            return hentUtenlandskTrygdemyndighetslandArtikkel13(behandlingsresultat);
        } else if (erArtikkel11_3aMed11_5Tilleggsbestemmelse(behandlingsresultat)) {
            return avklartefaktaService.hentInformertMyndighet(behandlingID).stream()
                .filter(landkode -> landkode != Land_iso2.NO).collect(Collectors.toSet());
        }

        Collection<Land_iso2> trygdemyndighetsland = hentTrygdemyndighetsland(behandlingsresultat);
        trygdemyndighetsland.remove(Land_iso2.NO);
        return trygdemyndighetsland;
    }

    private Collection<Land_iso2> hentUtenlandskTrygdemyndighetslandArtikkel13(Behandlingsresultat behandlingsresultat) {
        final long behandlingID = behandlingsresultat.getId();
        Set<Land_iso2> landkoderMedMarginaltArbeid = avklartefaktaService.hentLandkoderMedMarginaltArbeid(behandlingID);
        Behandlingsgrunnlag behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID);

        Stream<Land_iso2> marginaleArbeidslandMedUtenlandskArbeid = Stream.concat(
            behandlingsgrunnlag.getBehandlingsgrunnlagdata().hentUtenlandskeArbeidsstederLandkode().stream(),
            behandlingsgrunnlag.getBehandlingsgrunnlagdata().hentUtenlandskeArbeidsgivereLandkode().stream()
        ).map(Land_iso2::valueOf).filter(landkoderMedMarginaltArbeid::contains);

        Stream<Land_iso2> utpektLovvalgsland = behandlingsresultat.getUtpekingsperioder().stream()
            .map(Utpekingsperiode::getLovvalgsland)
            .map(landkoder -> Land_iso2.valueOf(landkoder.getKode()));

        return Streams.concat(
            marginaleArbeidslandMedUtenlandskArbeid,
            utpektLovvalgsland,
            hentTrygdemyndighetsland(behandlingsresultat).stream()
        ).filter(landkoder -> landkoder != Land_iso2.NO).collect(Collectors.toSet());
    }

    private Collection<Land_iso2> hentTrygdemyndighetsland(Behandlingsresultat behandlingsresultat) {
        final long behandlingID = behandlingsresultat.getId();
        BehandlingsgrunnlagData grunnlagdata = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID).getBehandlingsgrunnlagdata();

        if (behandlingsresultat.erInnvilgetArbeidPåSkipOmfattetAvArbeidsland() || erVideresendt(behandlingsresultat)) {
            return Lists.newArrayList(Land_iso2.valueOf(hentBostedsland(behandlingID, grunnlagdata).landkode()));
        } else {
            return new ArrayList<>(hentAlleArbeidslandUtenMarginaltArbeid(behandlingID));
        }
    }

    public Bostedsland hentBostedsland(Behandling behandling) {
        return hentBostedsland(behandling.getId(), behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata());
    }

    public Bostedsland hentBostedsland(long behandlingID, BehandlingsgrunnlagData grunnlagData) {
        Optional<Bostedsland> bostedslandOppgittAvSaksbehandler = hentBostedslandOppgittAvSaksbehandler(behandlingID, grunnlagData);
        return bostedslandOppgittAvSaksbehandler.orElse(new Bostedsland(Landkoder.NO));
    }

    private Optional<Bostedsland> hentBostedslandOppgittAvSaksbehandler(long behandlingID, BehandlingsgrunnlagData grunnlagData) {
        Optional<Bostedsland> bostedsland = avklartefaktaService.hentBostedland(behandlingID);
        if (bostedsland.isPresent()) {
            return bostedsland;
        } else {
            return hentOppgittBostedsland(grunnlagData);
        }
    }
}
