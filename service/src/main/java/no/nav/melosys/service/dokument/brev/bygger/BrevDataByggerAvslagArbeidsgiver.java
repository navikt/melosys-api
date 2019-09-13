package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAvslagArbeidsgiver;
import no.nav.melosys.service.dokument.brev.datagrunnlag.DokumentdataGrunnlag;

import static no.nav.melosys.domain.kodeverk.Vilkaar.ART12_1_VESENTLIG_VIRKSOMHET;
import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART12_1;

public class BrevDataByggerAvslagArbeidsgiver implements BrevDataBygger {
    private final VilkaarsresultatRepository vilkaarsresultatRepository;
    private final LandvelgerService landvelgerService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    public BrevDataByggerAvslagArbeidsgiver(LandvelgerService landvelgerService,
                                            LovvalgsperiodeService lovvalgsperiodeService,
                                            VilkaarsresultatRepository vilkaarsresultatRepository) {
        this.landvelgerService = landvelgerService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
    }

    @Override
    public BrevData lag(DokumentdataGrunnlag dataGrunnlag, String saksbehandler) throws FunksjonellException, TekniskException {
        long behandlingID = dataGrunnlag.getBehandling().getId();

        BrevDataAvslagArbeidsgiver brevData = new BrevDataAvslagArbeidsgiver(saksbehandler);
        brevData.person = dataGrunnlag.getPerson();

        brevData.hovedvirksomhet = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentHovedvirksomhet();
        brevData.lovvalgsperiode = lovvalgsperiodeService.hentLovvalgsperiode(behandlingID);
        brevData.arbeidsland = landvelgerService.hentArbeidsland(behandlingID).getBeskrivelse();

        brevData.vilkårbegrunnelser121 = hentVilkaarbegrunnelser(behandlingID, FO_883_2004_ART12_1);
        brevData.vilkårbegrunnelser121VesentligVirksomhet = hentVilkaarbegrunnelser(behandlingID, ART12_1_VESENTLIG_VIRKSOMHET);

        return brevData;
    }

    private Set<VilkaarBegrunnelse> hentVilkaarbegrunnelser(long behandlingID, Vilkaar vilkaarType) throws TekniskException {
        Optional<Vilkaarsresultat> vilkårsresultat = vilkaarsresultatRepository.findByBehandlingsresultatIdAndVilkaar(behandlingID, vilkaarType);
        Vilkaarsresultat resultat = vilkårsresultat.orElseThrow(() ->
            new TekniskException("Fant ingen vilkårbegrunnelse for " + vilkaarType));

        return resultat.getBegrunnelser();
    }
}