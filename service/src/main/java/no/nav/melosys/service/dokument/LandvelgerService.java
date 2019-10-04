package no.nav.melosys.service.dokument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART11_3A;
import static no.nav.melosys.domain.util.SoeknadUtils.hentOppgittBostedsland;
import static no.nav.melosys.domain.util.SoeknadUtils.hentSøknadslandkoder;

@Service
public class LandvelgerService {

    private AvklartefaktaService avklartefaktaService;
    private VilkaarsresultatRepository vilkaarsresultatRepository;
    private BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public LandvelgerService(AvklartefaktaService avklartefaktaService,
                             VilkaarsresultatRepository vilkaarsresultatRepository,
                            BehandlingsresultatService behandlingsresultatService) {
        this.avklartefaktaService = avklartefaktaService;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    public Landkoder hentArbeidsland(Behandling behandling) throws TekniskException, FunksjonellException {
        Collection<Landkoder> alleArbeidsland = hentAlleArbeidsland(behandling);
        if (alleArbeidsland.size() != 1) {
            throw new FunksjonellException("Fant ingen eller flere enn ett arbeidsland");
        }
        return alleArbeidsland.iterator().next();
    }

    public Collection<Landkoder> hentAlleArbeidsland(Behandling behandling) throws TekniskException {
        Collection<Landkoder> alleArbeidsland = avklartefaktaService.hentAlleAvklarteArbeidsland(behandling.getId());
        if (alleArbeidsland.isEmpty() || erArtikkel13(behandling)) {
            SoeknadDokument søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
            alleArbeidsland.addAll(hentSøknadslandkoder(søknad));
        }

        Collection<Landkoder> landMedMarginaltArbeid = avklartefaktaService.hentLandkoderMedMarginaltArbeid(behandling.getId());
        alleArbeidsland.removeAll(landMedMarginaltArbeid);
        return alleArbeidsland;
    }

    private boolean erArtikkel13(Behandling behandling) {
        try {
            Lovvalgsperiode lovvalgsperiode = behandlingsresultatService.hentBehandlingsresultat(behandling.getId()).hentValidertLovvalgsperiode();
            return lovvalgsperiode.erArtikkel13();
        } catch (IkkeFunnetException e) {
            // Ignorer
        }
        return false;
    }

    public Collection<Landkoder> hentUtenlandskTrygdemyndighetsland(Behandling behandling) throws TekniskException {
        Collection<Vilkaar> oppfylteVilkår = hentOppfylteVilkår(behandling);

        SoeknadDokument søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        if (oppfylteVilkår.contains(FO_883_2004_ART11_3A)) {
            return Collections.singletonList(hentBostedsland(behandling, søknad));
        }

        Collection<Landkoder> alleArbeidsland = hentAlleArbeidsland(behandling);
        alleArbeidsland.remove(Landkoder.NO);
        return new ArrayList<>(alleArbeidsland);
    }

    private Collection<Vilkaar> hentOppfylteVilkår(Behandling behandling) {
        return vilkaarsresultatRepository.findByBehandlingsresultatId(behandling.getId()).stream()
            .filter(Vilkaarsresultat::isOppfylt)
            .map(Vilkaarsresultat::getVilkaar)
            .collect(Collectors.toSet());
    }

    public Landkoder hentBostedsland(Behandling behandling, SoeknadDokument søknad) {
        Optional<Landkoder> bostedslandOppgittAvSaksbehandler = hentBostedslandOppgittAvSaksbehandler(behandling, søknad);
        return bostedslandOppgittAvSaksbehandler.orElse(Landkoder.NO);
    }

    private Optional<Landkoder> hentBostedslandOppgittAvSaksbehandler(Behandling behandling, SoeknadDokument søknad) {
        Optional<Landkoder> bostedsland = avklartefaktaService.hentBostedland(behandling.getId());
        if (bostedsland.isPresent()) {
            return bostedsland;
        } else {
            return hentOppgittBostedsland(søknad);
        }
    }
}