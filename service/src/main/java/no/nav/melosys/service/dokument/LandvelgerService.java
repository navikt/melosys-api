package no.nav.melosys.service.dokument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.SoeknadService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART11_3A;
import static no.nav.melosys.domain.util.SoeknadUtils.hentOppgittBostedsland;
import static no.nav.melosys.domain.util.SoeknadUtils.hentSøknadslandkoder;

@Service
public class LandvelgerService {
    private AvklartefaktaService avklartefaktaService;
    private BehandlingsresultatService behandlingsresultatService;
    private SoeknadService søknadService;
    private VilkaarsresultatRepository vilkaarsresultatRepository;

    @Autowired
    public LandvelgerService(AvklartefaktaService avklartefaktaService,
                             BehandlingsresultatService behandlingsresultatService,
                             SoeknadService søknadService,
                             VilkaarsresultatRepository vilkaarsresultatRepository) {
        this.avklartefaktaService = avklartefaktaService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.søknadService = søknadService;
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
            SoeknadDokument søknad = søknadService.hentSøknad(behandlingID);
            alleArbeidsland.addAll(hentSøknadslandkoder(søknad));
        }

        Collection<Landkoder> landMedMarginaltArbeid = avklartefaktaService.hentLandkoderMedMarginaltArbeid(behandlingID);
        alleArbeidsland.removeAll(landMedMarginaltArbeid);
        return alleArbeidsland;
    }

    private boolean erArtikkel13(long behandlingId) {
        try {
            Lovvalgsperiode lovvalgsperiode = behandlingsresultatService.hentBehandlingsresultat(behandlingId).hentValidertLovvalgsperiode();
            return lovvalgsperiode.erArtikkel13();
        } catch (IkkeFunnetException e) {
            // Ignorer
        }
        return false;
    }

    public Collection<Landkoder> hentUtenlandskTrygdemyndighetsland(long behandlingID) throws IkkeFunnetException {
        Collection<Vilkaar> oppfylteVilkår = hentOppfylteVilkår(behandlingID);
        SoeknadDokument søknad = søknadService.hentSøknad(behandlingID);
        if (oppfylteVilkår.contains(FO_883_2004_ART11_3A)) {
            return Collections.singletonList(hentBostedsland(behandlingID, søknad));
        }

        Medlemskapsperiode medlemskapsperiode = behandlingsresultatService.hentBehandlingsresultat(behandlingID).hentValidertMedlemskapsperiode();
        if (medlemskapsperiode.erArtikkel13()) {
            Landkoder bostedsland = hentBostedsland(behandlingID, søknad);
            if (bostedsland != Landkoder.NO) {
                return Collections.singletonList(bostedsland);
            }
        }

        Collection<Landkoder> alleArbeidsland = hentAlleArbeidsland(behandlingID);
        alleArbeidsland.remove(Landkoder.NO);
        return new ArrayList<>(alleArbeidsland);
    }

    private Collection<Vilkaar> hentOppfylteVilkår(long behandlingID) {
        return vilkaarsresultatRepository.findByBehandlingsresultatId(behandlingID).stream()
            .filter(Vilkaarsresultat::isOppfylt)
            .map(Vilkaarsresultat::getVilkaar)
            .collect(Collectors.toSet());
    }

    public Landkoder hentBostedsland(long behandlingID, SoeknadDokument søknad) {
        Optional<Landkoder> bostedslandOppgittAvSaksbehandler = hentBostedslandOppgittAvSaksbehandler(behandlingID, søknad);
        return bostedslandOppgittAvSaksbehandler.orElse(Landkoder.NO);
    }

    private Optional<Landkoder> hentBostedslandOppgittAvSaksbehandler(long behandlingID, SoeknadDokument søknad) {
        Optional<Landkoder> bostedsland = avklartefaktaService.hentBostedland(behandlingID);
        if (bostedsland.isPresent()) {
            return bostedsland;
        } else {
            return hentOppgittBostedsland(søknad);
        }
    }
}