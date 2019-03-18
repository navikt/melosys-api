package no.nav.melosys.service.dokument;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.TilleggsBestemmelser_883_2004;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.util.SoeknadUtils.hentMaritimtArbeid;

@Service
public class LandvelgerService {

    private AvklartefaktaService avklartefaktaService;
    private LovvalgsperiodeService lovvalgsperiodeService;

    @Autowired
    public LandvelgerService(AvklartefaktaService avklartefaktaService,
                             LovvalgsperiodeService lovvalgsperiodeService) {
        this.avklartefaktaService = avklartefaktaService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    public String hentArbeidsland(Behandling behandling) throws FunksjonellException, TekniskException {
        SoeknadDokument søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);

        Lovvalgsperiode periode = lovvalgsperiodeService.hentLovvalgsperioder(behandling.getId()).iterator().next();
        LovvalgBestemmelse lovvalgBestemmelse = periode.getBestemmelse();
        LovvalgBestemmelse tilleggsBestemmelse = periode.getTilleggsbestemmelse();

        if (lovvalgBestemmelse == null &&
            periode.getInnvilgelsesresultat() != InnvilgelsesResultat.AVSLAATT) {
            throw new FunksjonellException("Lovvalgsbestemmelse lik null er kun gyldig ved avslag");
        }

        // Artikklene 12.1, 12.2, 16.1 bruker oppholdsland
        Landkoder arbeidsland = Landkoder.valueOf(søknad.oppholdUtland.oppholdslandKoder.get(0));

        if (lovvalgBestemmelse == LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1 &&
            tilleggsBestemmelse == TilleggsBestemmelser_883_2004.FO_883_2004_ART11_4_1) {
            Optional<Landkoder> avklarteFlaggland = avklartefaktaService.hentFlaggland(behandling.getId());
            arbeidsland = avklarteFlaggland.orElseThrow(() -> new FunksjonellException("ART12_1 + ART11_4_1: Trenger avklart flaggland"));
        }
        else if (lovvalgBestemmelse == LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_3A) {
            if (tilleggsBestemmelse == TilleggsBestemmelser_883_2004.FO_883_2004_ART11_4_1) {
                Optional<Landkoder> avklarteFlaggland = avklartefaktaService.hentFlaggland(behandling.getId());
                arbeidsland = avklarteFlaggland.orElseThrow(() -> new FunksjonellException("ART11_3A + ART11_4_1: Trenger avklart flaggland"));
            } else {
                arbeidsland = Landkoder.valueOf(hentMaritimtArbeid(søknad).territorialfarvann);
            }
        }
        else if (lovvalgBestemmelse == LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_4_2) {
            Optional<Landkoder> avklarteFlaggland = avklartefaktaService.hentFlaggland(behandling.getId());
            arbeidsland = avklarteFlaggland.orElseThrow(() -> new FunksjonellException("ART11_4_2: Trenger Avklart flaggland"));
        }

        return arbeidsland.getBeskrivelse();
    }

    public String hentTrygdemyndighetsland(Behandling behandling) throws FunksjonellException, TekniskException {
        SoeknadDokument søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);

        Lovvalgsperiode periode = lovvalgsperiodeService.hentLovvalgsperioder(behandling.getId()).iterator().next();
        LovvalgBestemmelse lovvalgBestemmelse = periode.getBestemmelse();
        LovvalgBestemmelse tilleggsBestemmelse = periode.getTilleggsbestemmelse();

        // Artikklene 12.1, 12.2, 16.1 bruker oppholdsland
        Landkoder trygdemyndighetsland = Landkoder.valueOf(søknad.oppholdUtland.oppholdslandKoder.get(0));

        if (lovvalgBestemmelse == LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1 &&
            tilleggsBestemmelse == TilleggsBestemmelser_883_2004.FO_883_2004_ART11_4_1) {
            Optional<Landkoder> avklarteFlaggland = avklartefaktaService.hentFlaggland(behandling.getId());
            trygdemyndighetsland = avklarteFlaggland.orElseThrow(() -> new FunksjonellException("ART12_1 + ART11_4_1: Trenger avklart flaggland"));
        }
        else if (lovvalgBestemmelse == LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_3A ||
            lovvalgBestemmelse == LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_4_2) {
            trygdemyndighetsland = hentBostedsland(behandling, søknad);
        }

        return trygdemyndighetsland.getBeskrivelse();
    }

    private Landkoder hentBostedsland(Behandling behandling, SoeknadDokument søknad) {
        Optional<Landkoder> bostedslandOpt = avklartefaktaService.hentBostedland(behandling.getId());
        return bostedslandOpt.orElseGet(() -> Landkoder.valueOf(søknad.bosted.oppgittAdresse.landKode));
    }
}