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
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART11_3A;
import static no.nav.melosys.domain.util.BehandlingsgrunnlagUtils.hentOppgittBostedsland;
import static no.nav.melosys.domain.util.BehandlingsgrunnlagUtils.hentSøknadslandkoder;

@Service
public class LandvelgerService {
    private final AvklartefaktaService avklartefaktaService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final VilkaarsresultatRepository vilkaarsresultatRepository;

    @Autowired
    public LandvelgerService(AvklartefaktaService avklartefaktaService,
                             BehandlingsresultatService behandlingsresultatService,
                             BehandlingsgrunnlagService behandlingsgrunnlagService, VilkaarsresultatRepository vilkaarsresultatRepository) {
        this.avklartefaktaService = avklartefaktaService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
    }

    public Landkoder hentArbeidsland(long behandlingID) throws FunksjonellException {
        Collection<Landkoder> alleArbeidsland = hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);
        if (alleArbeidsland.size() != 1) {
            throw new FunksjonellException("Fant ingen eller flere enn ett arbeidsland");
        }
        return alleArbeidsland.iterator().next();
    }

    private Collection<Landkoder> hentAlleArbeidsland(long behandlingID) throws IkkeFunnetException {
        Collection<Landkoder> alleArbeidsland = avklartefaktaService.hentAlleAvklarteArbeidsland(behandlingID);
        if (alleArbeidsland.isEmpty() || erArtikkel13(behandlingID)) {
            BehandlingsgrunnlagData grunnlagData = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID).getBehandlingsgrunnlagdata();
            alleArbeidsland.addAll(hentSøknadslandkoder(grunnlagData));
        }

        return alleArbeidsland;
    }

    public Collection<Landkoder> hentAlleArbeidslandUtenMarginaltArbeid(long behandlingID) throws IkkeFunnetException {
        Collection<Landkoder> alleArbeidsland = hentAlleArbeidsland(behandlingID);
        Collection<Landkoder> landMedMarginaltArbeid = avklartefaktaService.hentLandkoderMedMarginaltArbeid(behandlingID);
        alleArbeidsland.removeAll(landMedMarginaltArbeid);

        return alleArbeidsland;
    }

    private boolean erArtikkel13(long behandlingId) throws IkkeFunnetException {
        return erArtikkel13(behandlingsresultatService.hentBehandlingsresultat(behandlingId));
    }

    private boolean erArtikkel13(Behandlingsresultat behandlingsresultat) {
        if (behandlingsresultat.harMedlemskapsperiode()) {
            Medlemskapsperiode medlemskapsperiode = behandlingsresultat.hentValidertMedlemskapsperiode();
            return medlemskapsperiode.erArtikkel13();
        } else {
            return erVideresendt(behandlingsresultat);
        }
    }

    private boolean erArtikkel11_5(Behandlingsresultat behandlingsresultat) throws IkkeFunnetException {
        return behandlingsresultat.finnValidertLovvalgsperiode()
            .filter(Lovvalgsperiode::erArtikkel11_5)
            .isPresent();
    }

    private boolean erVideresendt(Behandlingsresultat behandlingsresultat) {
        Fagsak fagsak = behandlingsresultat.getBehandling().getFagsak();
        return fagsak.getStatus() == Saksstatuser.VIDERESENDT;
    }

    public Collection<Landkoder> hentUtenlandskTrygdemyndighetsland(long behandlingID) throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        if (erArtikkel13(behandlingsresultat) && !erVideresendt(behandlingsresultat)) {
            return hentUtenlandskTrygdemyndighetslandArtikkel13(behandlingsresultat);
        } else if (erArtikkel11_5(behandlingsresultat)) {
            return avklartefaktaService.hentInformertMyndighet(behandlingID).stream()
                .filter(landkode -> landkode != Landkoder.NO).collect(Collectors.toList());
        }

        Collection<Landkoder> trygdemyndighetsland = hentTrygdemyndighetsland(behandlingsresultat);
        trygdemyndighetsland.remove(Landkoder.NO);
        return trygdemyndighetsland;
    }

    private Collection<Landkoder> hentUtenlandskTrygdemyndighetslandArtikkel13(Behandlingsresultat behandlingsresultat) throws IkkeFunnetException {
        final long behandlingID = behandlingsresultat.getId();
        Set<Landkoder> landkoderMedMarginaltArbeid = avklartefaktaService.hentLandkoderMedMarginaltArbeid(behandlingID);
        Behandlingsgrunnlag behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID);

        Stream<Landkoder> marginaleArbeidslandMedUtenlandskArbeid = Stream.concat(
            behandlingsgrunnlag.getBehandlingsgrunnlagdata().hentUtenlandskeArbeidsstederLandkode().stream(),
            behandlingsgrunnlag.getBehandlingsgrunnlagdata().hentUtenlandskeArbeidsgivereLandkode().stream()
        ).map(Landkoder::valueOf).filter(landkoderMedMarginaltArbeid::contains);

        Stream<Landkoder> utpektLovvalgsland = behandlingsresultat.getUtpekingsperioder().stream()
            .map(Utpekingsperiode::getLovvalgsland);

        return Streams.concat(
            marginaleArbeidslandMedUtenlandskArbeid,
            utpektLovvalgsland,
            hentTrygdemyndighetsland(behandlingsresultat).stream()
        ).filter(landkoder -> landkoder != Landkoder.NO).collect(Collectors.toSet());
    }

    private Collection<Landkoder> hentTrygdemyndighetsland(Behandlingsresultat behandlingsresultat) throws IkkeFunnetException {
        final long behandlingID = behandlingsresultat.getId();
        Collection<Vilkaar> oppfylteVilkår = hentOppfylteVilkår(behandlingID);
        BehandlingsgrunnlagData grunnlagdata = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID).getBehandlingsgrunnlagdata();
        if (oppfylteVilkår.contains(FO_883_2004_ART11_3A)) {
            return Lists.newArrayList(hentBostedsland(behandlingID, grunnlagdata));
        }

        if (erVideresendt(behandlingsresultat)) {
            return Lists.newArrayList(hentBostedsland(behandlingID, grunnlagdata));
        }

        Collection<Landkoder> alleArbeidsland = hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);
        return new ArrayList<>(alleArbeidsland);
    }

    private Collection<Vilkaar> hentOppfylteVilkår(long behandlingID) {
        return vilkaarsresultatRepository.findByBehandlingsresultatId(behandlingID).stream()
            .filter(Vilkaarsresultat::isOppfylt)
            .map(Vilkaarsresultat::getVilkaar)
            .collect(Collectors.toSet());
    }

    public Landkoder hentBostedsland(Behandling behandling) {
        return hentBostedsland(behandling.getId(), behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata());
    }

    public Landkoder hentBostedsland(long behandlingID, BehandlingsgrunnlagData grunnlagData) {
        Optional<Landkoder> bostedslandOppgittAvSaksbehandler = hentBostedslandOppgittAvSaksbehandler(behandlingID, grunnlagData);
        return bostedslandOppgittAvSaksbehandler.orElse(Landkoder.NO);
    }

    private Optional<Landkoder> hentBostedslandOppgittAvSaksbehandler(long behandlingID, BehandlingsgrunnlagData grunnlagData) {
        Optional<Landkoder> bostedsland = avklartefaktaService.hentBostedland(behandlingID);
        if (bostedsland.isPresent()) {
            return bostedsland;
        } else {
            return hentOppgittBostedsland(grunnlagData);
        }
    }
}