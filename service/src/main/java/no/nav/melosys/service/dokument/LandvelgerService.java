package no.nav.melosys.service.dokument;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Vilkaar.*;
import static no.nav.melosys.domain.util.SoeknadUtils.hentMaritimtArbeid;

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

    public List<Vilkaar> hentOppfylteVilkår(Behandling behandling) {
        return vilkaarsresultatRepository.findByBehandlingsresultatId(behandling.getId()).stream()
                    .filter(Vilkaarsresultat::isOppfylt)
                    .map(Vilkaarsresultat::getVilkaar)
                    .collect(Collectors.toList());
    }

    public Landkoder hentArbeidsland(Behandling behandling) throws FunksjonellException, TekniskException {
        SoeknadDokument søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        List<Vilkaar> oppfylteVilkår = hentOppfylteVilkår(behandling);

        if (oppfylteVilkår.contains(FO_883_2004_ART12_1) ||
            oppfylteVilkår.contains(FO_883_2004_ART12_2) ||
            oppfylteVilkår.contains(FO_883_2004_ART16_1)) {
             if (oppfylteVilkår.contains(FO_883_2004_ART11_4_1)) {
                 Optional<Landkoder> avklarteFlaggland = avklartefaktaService.hentFlaggland(behandling.getId());
                 return avklarteFlaggland.orElseThrow(() -> new FunksjonellException("ART12_1 + ART11_4_1: Trenger avklart flaggland"));
             }
        }

        if (oppfylteVilkår.contains(FO_883_2004_ART11_3A)) {
            if (oppfylteVilkår.contains(FO_883_2004_ART11_4_1)) {
                Optional<Landkoder> avklarteFlaggland = avklartefaktaService.hentFlaggland(behandling.getId());
                return avklarteFlaggland.orElseThrow(() -> new FunksjonellException("ART11_3A + ART11_4_1: Trenger avklart flaggland"));
            } else {
                return Landkoder.valueOf(hentMaritimtArbeid(søknad).territorialfarvann);
            }
        }

        if (oppfylteVilkår.contains(FO_883_2004_ART11_4_2)) {
            Optional<Landkoder> avklarteFlaggland = avklartefaktaService.hentFlaggland(behandling.getId());
            return avklarteFlaggland.orElseThrow(() -> new FunksjonellException("ART11_4_2: Trenger Avklart flaggland"));
        }

        // Artiklene 12.1, 12.2, 16.1 bruker oppholdsland
        return Landkoder.valueOf(søknad.oppholdUtland.oppholdslandKoder.get(0));
    }

    public Landkoder hentTrygdemyndighetsland(Behandling behandling) throws FunksjonellException, TekniskException {
        SoeknadDokument søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);

        List<Vilkaar> oppfylteVilkår = hentOppfylteVilkår(behandling);

        if (oppfylteVilkår.contains(FO_883_2004_ART12_1) ||
            oppfylteVilkår.contains(FO_883_2004_ART12_2) ||
            oppfylteVilkår.contains(FO_883_2004_ART16_1)) {
            if (oppfylteVilkår.contains(FO_883_2004_ART11_4_1)) {
                Optional<Landkoder> avklarteFlaggland = avklartefaktaService.hentFlaggland(behandling.getId());
                return avklarteFlaggland.orElseThrow(() -> new FunksjonellException("ART12_1 + ART11_4_1: Trenger avklart flaggland"));
            }
        }

        if (oppfylteVilkår.contains(FO_883_2004_ART11_3A) || oppfylteVilkår.contains(FO_883_2004_ART11_4_2)) {
            return hentBostedsland(behandling, søknad);
        }

        // Artikklene 12.1, 12.2, 16.1 bruker oppholdsland
        return Landkoder.valueOf(søknad.oppholdUtland.oppholdslandKoder.get(0));
    }

    private Landkoder hentBostedsland(Behandling behandling, SoeknadDokument søknad) {
        Optional<Landkoder> bostedslandOpt = avklartefaktaService.hentBostedland(behandling.getId());
        return bostedslandOpt.orElseGet(() -> Landkoder.valueOf(søknad.bosted.oppgittAdresse.landKode));
    }
}