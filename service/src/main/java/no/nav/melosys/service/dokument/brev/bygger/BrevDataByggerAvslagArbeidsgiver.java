package no.nav.melosys.service.dokument.brev.bygger;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.dokument.felles.Adresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.AvklarteVirksomheter;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAvslagArbeidsgiver;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;

import static no.nav.melosys.domain.kodeverk.Vilkaar.ART12_1_VESENTLIG_VIRKSOMHET;
import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART12_1;

public class BrevDataByggerAvslagArbeidsgiver extends AbstraktDokumentDataBygger implements BrevDataBygger {

    private final RegisterOppslagService registerOppslagService;
    private final VilkaarsresultatRepository vilkaarsresultatRepository;

    public BrevDataByggerAvslagArbeidsgiver(AvklartefaktaService avklartefaktaService,
                                            RegisterOppslagService registerOppslagService,
                                            LovvalgsperiodeService lovvalgsperiodeService,
                                            VilkaarsresultatRepository vilkaarsresultatRepository) {
        super(null, lovvalgsperiodeService, avklartefaktaService);
        this.registerOppslagService = registerOppslagService;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
    }

    protected Function<OrganisasjonDokument, Adresse> utenAdresse = org -> null;

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        this.behandling = behandling;
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);
        AvklarteVirksomheter avklarteVirksomheter = new AvklarteVirksomheter(avklartefaktaService, registerOppslagService, behandling);

        BrevDataAvslagArbeidsgiver brevData = new BrevDataAvslagArbeidsgiver(saksbehandler);
        brevData.mottaker = Aktoersroller.BRUKER;
        brevData.person = person;

        List<Virksomhet> norskeVirksomheter = avklarteVirksomheter.hentAlleNorskeAvklarteVirksomheter(utenAdresse);
        brevData.hovedvirksomhet = norskeVirksomheter.iterator().next();
        brevData.lovvalgsperiode = hentLovvalgsperiode();

        brevData.vilkårbegrunnelser121 = hentVilkaarbegrunnelser(FO_883_2004_ART12_1);
        brevData.vilkårbegrunnelser121VesentligVirksomhet = hentVilkaarbegrunnelser(ART12_1_VESENTLIG_VIRKSOMHET);

        return brevData;
    }

    private Set<VilkaarBegrunnelse> hentVilkaarbegrunnelser(Vilkaar vilkaarType) throws TekniskException {
        List<Vilkaarsresultat> vilkaarresultater = vilkaarsresultatRepository.findByBehandlingsresultatId(behandling.getId());
        Optional<Vilkaarsresultat> vilkårsresultat = vilkaarresultater.stream()
            .filter(vilkaarsresultat -> vilkaarsresultat.getVilkaar() == vilkaarType)
            .findFirst();

        Vilkaarsresultat resultat = vilkårsresultat.orElseThrow(() ->
            new TekniskException("Fant ingen vilkårbegrunnelse for " + vilkaarType));

        if (resultat.getBegrunnelser().isEmpty()) {
            throw new TekniskException("Brevet Orientering til arbeidsgiver om avslag trenger en begrunnelsekode for " + vilkaarType);
        }
        return resultat.getBegrunnelser();
    }
}