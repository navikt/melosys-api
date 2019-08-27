package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
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
import no.nav.melosys.service.dokument.brev.ressurser.Dokumentressurser;

import static no.nav.melosys.domain.kodeverk.Vilkaar.ART12_1_VESENTLIG_VIRKSOMHET;
import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART12_1;

public class BrevDataByggerAvslagArbeidsgiver implements BrevDataBygger {
    private final VilkaarsresultatRepository vilkaarsresultatRepository;
    private final LandvelgerService landvelgerService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private Behandling behandling;

    public BrevDataByggerAvslagArbeidsgiver(LandvelgerService landvelgerService,
                                            LovvalgsperiodeService lovvalgsperiodeService,
                                            VilkaarsresultatRepository vilkaarsresultatRepository) {
        this.landvelgerService = landvelgerService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
    }

    @Override
    public BrevData lag(Dokumentressurser dokumentressurser, String saksbehandler) throws FunksjonellException, TekniskException {
        this.behandling = dokumentressurser.getBehandling();

        BrevDataAvslagArbeidsgiver brevData = new BrevDataAvslagArbeidsgiver(saksbehandler);
        brevData.person = dokumentressurser.getPerson();

        brevData.hovedvirksomhet = dokumentressurser.getAvklarteVirksomheter().hentHovedvirksomhet();
        brevData.lovvalgsperiode = lovvalgsperiodeService.hentLovvalgsperiode(behandling.getId());
        brevData.arbeidsland = landvelgerService.hentArbeidsland(behandling).getBeskrivelse();

        brevData.vilkårbegrunnelser121 = hentVilkaarbegrunnelser(FO_883_2004_ART12_1);
        brevData.vilkårbegrunnelser121VesentligVirksomhet = hentVilkaarbegrunnelser(ART12_1_VESENTLIG_VIRKSOMHET);

        return brevData;
    }

    private Set<VilkaarBegrunnelse> hentVilkaarbegrunnelser(Vilkaar vilkaarType) throws TekniskException {
        Optional<Vilkaarsresultat> vilkårsresultat = vilkaarsresultatRepository.findByBehandlingsresultatIdAndVilkaar(behandling.getId(), vilkaarType);
        Vilkaarsresultat resultat = vilkårsresultat.orElseThrow(() ->
            new TekniskException("Fant ingen vilkårbegrunnelse for " + vilkaarType));

        return resultat.getBegrunnelser();
    }
}