package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntak;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;

import static no.nav.melosys.domain.kodeverk.Vilkaar.*;

public class BrevDataByggerAnmodningUnntak implements BrevDataBygger {
    private final LandvelgerService landvelgerService;
    private final VilkaarsresultatRepository vilkaarsresultatRepository;

    public BrevDataByggerAnmodningUnntak(LandvelgerService landvelgerService,
                                         VilkaarsresultatRepository vilkaarsresultatRepository) {
        this.landvelgerService = landvelgerService;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataAnmodningUnntak brevData = new BrevDataAnmodningUnntak(saksbehandler);
        long behandlingID = dataGrunnlag.getBehandling().getId();
        if (dataGrunnlag.getAvklarteVirksomheterGrunnlag().antallVirksomheter() != 1) {
            throw new TekniskException("Ingen eller flere enn én norsk eller utenlandsk virksomhet oppgitt for avslag eller ART16.1");
        }

        brevData.hovedvirksomhet = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentHovedvirksomhet();
        brevData.yrkesaktivitet = brevData.hovedvirksomhet.yrkesaktivitet;
        brevData.arbeidsland = landvelgerService.hentArbeidsland(behandlingID).getBeskrivelse();

        Vilkaarsresultat art16Vilkaar = hentFørsteGyldigeVilkaarsresultatArt16(behandlingID);
        Set<VilkaarBegrunnelse> art16Begrunnelser = art16Vilkaar.getBegrunnelser();
        if (harVilkaarForArtikkel12(behandlingID)) {
            brevData.anmodningBegrunnelser = art16Begrunnelser;
            brevData.anmodningUtenArt12Begrunnelser = Collections.emptySet();
        } else {
            brevData.anmodningBegrunnelser = Collections.emptySet();
            brevData.anmodningUtenArt12Begrunnelser = art16Begrunnelser;
        }

        brevData.anmodningFritekst = art16Vilkaar.getBegrunnelseFritekst();

        return brevData;
    }

    private boolean harVilkaarForArtikkel12(long behandlingID) {
        Optional<Vilkaarsresultat> art121Vilkaar = vilkaarsresultatRepository.findByBehandlingsresultatIdAndVilkaar(behandlingID, FO_883_2004_ART12_1);
        Optional<Vilkaarsresultat> art122Vilkaar = vilkaarsresultatRepository.findByBehandlingsresultatIdAndVilkaar(behandlingID, FO_883_2004_ART12_2);
        return art121Vilkaar.isPresent() || art122Vilkaar.isPresent();
    }

    // Vilkåret for art16 er både oppfylt og har begrunnelser ved anmodning om unntak
    private Vilkaarsresultat hentFørsteGyldigeVilkaarsresultatArt16(long behandlingID) throws TekniskException {
        return vilkaarsresultatRepository.findByBehandlingsresultatIdAndVilkaar(behandlingID, FO_883_2004_ART16_1)
            .filter(v -> v.isOppfylt() && !v.getBegrunnelser().isEmpty())
            .orElseThrow(() -> new TekniskException("Ingen oppfylte art16-vilkår med vilkårbegrunnelser funnet for brev om orientering anmodning om unntak"));
    }
}