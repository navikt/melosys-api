package no.nav.melosys.service.dokument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
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
        Collection<Landkoder> alleArbeidsland = hentAlleArbeidsland(behandlingID);
        if (alleArbeidsland.size() != 1) {
            throw new FunksjonellException("Fant ingen eller flere enn ett arbeidsland");
        }
        return alleArbeidsland.iterator().next();
    }

    public Collection<Landkoder> hentAlleArbeidsland(long behandlingID) throws IkkeFunnetException {
        Collection<Landkoder> alleArbeidsland = avklartefaktaService.hentAlleAvklarteArbeidsland(behandlingID);
        if (alleArbeidsland.isEmpty() || erArtikkel13(behandlingID)) {
            BehandlingsgrunnlagData grunnlagData = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID).getBehandlingsgrunnlagdata();
            alleArbeidsland.addAll(hentSøknadslandkoder(grunnlagData));
        }

        Collection<Landkoder> landMedMarginaltArbeid = avklartefaktaService.hentLandkoderMedMarginaltArbeid(behandlingID);
        alleArbeidsland.removeAll(landMedMarginaltArbeid);
        return alleArbeidsland;
    }

    private boolean erArtikkel13(long behandlingId) throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId);
        if (behandlingsresultat.harMedlemskapsperiode()) {
            Medlemskapsperiode medlemskapsperiode = behandlingsresultat.hentValidertMedlemskapsperiode();
            return medlemskapsperiode.erArtikkel13();
        } else {
            return erVideresendt(behandlingsresultat);
        }
    }

    private boolean erVideresendt(Behandlingsresultat behandlingsresultat) {
        Fagsak fagsak = behandlingsresultat.getBehandling().getFagsak();
        return fagsak.getStatus() == Saksstatuser.VIDERESENDT;
    }

    public Collection<Landkoder> hentUtenlandskTrygdemyndighetsland(long behandlingID) throws IkkeFunnetException {
        Collection<Landkoder> trygdemyndighetsland = hentTrygdemyndighetsland(behandlingID);
        trygdemyndighetsland.remove(Landkoder.NO);
        return trygdemyndighetsland;
    }

    private Collection<Landkoder> hentTrygdemyndighetsland(long behandlingID) throws IkkeFunnetException {
        Collection<Vilkaar> oppfylteVilkår = hentOppfylteVilkår(behandlingID);
        BehandlingsgrunnlagData grunnlagdata = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID).getBehandlingsgrunnlagdata();
        if (oppfylteVilkår.contains(FO_883_2004_ART11_3A)) {
            return Lists.newArrayList(hentBostedsland(behandlingID, grunnlagdata));
        }

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        if (erVideresendt(behandlingsresultat)) {
            return Lists.newArrayList(hentBostedsland(behandlingID, grunnlagdata));
        }

        Collection<Landkoder> alleArbeidsland = hentAlleArbeidsland(behandlingID);
        return new ArrayList<>(alleArbeidsland);
    }

    private Collection<Vilkaar> hentOppfylteVilkår(long behandlingID) {
        return vilkaarsresultatRepository.findByBehandlingsresultatId(behandlingID).stream()
            .filter(Vilkaarsresultat::isOppfylt)
            .map(Vilkaarsresultat::getVilkaar)
            .collect(Collectors.toSet());
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