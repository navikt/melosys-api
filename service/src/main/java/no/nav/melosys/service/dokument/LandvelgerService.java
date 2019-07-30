package no.nav.melosys.service.dokument;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART11_3A;
import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART11_4_2;
import static no.nav.melosys.domain.util.SoeknadUtils.hentOppgittBostedsland;
import static no.nav.melosys.domain.util.SoeknadUtils.hentSøknadslandkoder;

@Service
public class LandvelgerService {


    private AvklartefaktaService avklartefaktaService;
    private VilkaarsresultatRepository vilkaarsresultatRepository;

    @Autowired
    public LandvelgerService(AvklartefaktaService avklartefaktaService,
                             VilkaarsresultatRepository vilkaarsresultatRepository) {
        this.avklartefaktaService = avklartefaktaService;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
    }

    public Landkoder hentArbeidsland(Behandling behandling) throws TekniskException {
        Optional<Landkoder> arbeidslandOpt = avklartefaktaService.hentArbeidsland(behandling.getId());
        SoeknadDokument søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        return arbeidslandOpt.orElseGet(() -> hentSøknadslandkoder(søknad).get(0));
    }

    public Set<Landkoder> hentAlleArbeidsland(Behandling behandling) throws TekniskException {
        Set<Landkoder> alleArbeidsland = avklartefaktaService.hentAlleArbeidsland(behandling.getId());
        SoeknadDokument søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        alleArbeidsland.addAll(hentSøknadslandkoder(søknad));
        return alleArbeidsland;
    }

    public Landkoder hentTrygdemyndighetsland(Behandling behandling) throws TekniskException {
        List<Vilkaar> oppfylteVilkår = hentOppfylteVilkår(behandling);

        SoeknadDokument søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        if (oppfylteVilkår.contains(FO_883_2004_ART11_3A) || oppfylteVilkår.contains(FO_883_2004_ART11_4_2)) {
            return hentBostedsland(behandling, søknad);
        }

        return hentArbeidsland(behandling);
    }

    private List<Vilkaar> hentOppfylteVilkår(Behandling behandling) {
        return vilkaarsresultatRepository.findByBehandlingsresultatId(behandling.getId()).stream()
            .filter(Vilkaarsresultat::isOppfylt)
            .map(Vilkaarsresultat::getVilkaar)
            .collect(Collectors.toList());
    }

    private Landkoder hentBostedsland(Behandling behandling, SoeknadDokument søknad) {
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