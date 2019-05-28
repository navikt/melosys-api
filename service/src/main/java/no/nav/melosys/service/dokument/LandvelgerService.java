package no.nav.melosys.service.dokument;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import static no.nav.melosys.domain.util.SoeknadUtils.hentSøknadslandkoder;

@Service
public class LandvelgerService {
    private static final String KAN_IKKE_HENTE_BOSTEDSLAND = "Kan ikke hente bostedsland";

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
        Optional<Landkoder> bostedslandOpt = avklartefaktaService.hentBostedland(behandling.getId());
        return bostedslandOpt.orElseGet(() -> hentBostedslandFraOppgittAdresse(søknad));
    }

    private Landkoder hentBostedslandFraOppgittAdresse(SoeknadDokument søknad) {
        Objects.requireNonNull(søknad, KAN_IKKE_HENTE_BOSTEDSLAND + ", søknad er null.");
        Objects.requireNonNull(søknad.bosted.oppgittAdresse.landkode, KAN_IKKE_HENTE_BOSTEDSLAND + " fra oppgitt adresse, landkode er null.");
        return Landkoder.valueOf(søknad.bosted.oppgittAdresse.landkode);
    }
}