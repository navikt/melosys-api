package no.nav.melosys.service.dokument.brev.bygger;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.Adresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAvslagArbeidsgiver;

import static no.nav.melosys.domain.kodeverk.Vilkaar.ART12_1_VESENTLIG_VIRKSOMHET;
import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART12_1;

public class BrevDataByggerAvslagArbeidsgiver extends AbstraktDokumentDataBygger implements BrevDataBygger {

    private AvklarteVirksomheterService avklarteVirksomheterService;
    private LandvelgerService landvelgerService;
    private final VilkaarsresultatRepository vilkaarsresultatRepository;

    public BrevDataByggerAvslagArbeidsgiver(AvklartefaktaService avklartefaktaService,
                                            AvklarteVirksomheterService avklarteVirksomheterService,
                                            LandvelgerService landvelgerService,
                                            LovvalgsperiodeService lovvalgsperiodeService,
                                            VilkaarsresultatRepository vilkaarsresultatRepository) {
        super(null, lovvalgsperiodeService, avklartefaktaService);
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.landvelgerService = landvelgerService;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
    }

    Function<OrganisasjonDokument, Adresse> utenAdresse = org -> null;

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        this.behandling = behandling;
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);

        BrevDataAvslagArbeidsgiver brevData = new BrevDataAvslagArbeidsgiver(saksbehandler);
        brevData.mottakerRolle = Aktoersroller.ARBEIDSGIVER;
        brevData.person = person;

        List<AvklartVirksomhet> norskeVirksomheter = avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, utenAdresse);
        brevData.hovedvirksomhet = norskeVirksomheter.iterator().next();
        brevData.lovvalgsperiode = hentLovvalgsperiode();
        brevData.arbeidsland =landvelgerService.hentArbeidsland(behandling).getBeskrivelse();

        brevData.vilkårbegrunnelser121 = hentVilkaarbegrunnelser(FO_883_2004_ART12_1);
        brevData.vilkårbegrunnelser121VesentligVirksomhet = hentVilkaarbegrunnelser(ART12_1_VESENTLIG_VIRKSOMHET);

        return brevData;
    }

    private Set<VilkaarBegrunnelse> hentVilkaarbegrunnelser(Vilkaar vilkaarType) throws TekniskException {
        Optional<Vilkaarsresultat> vilkårsresultat = vilkaarsresultatRepository.findByBehandlingsresultatIdAndVilkaar(behandling.getId(), vilkaarType);
        Vilkaarsresultat resultat = vilkårsresultat.orElseThrow(() ->
            new TekniskException("Fant ingen vilkårbegrunnelse for " + vilkaarType));

        if (resultat.getBegrunnelser().isEmpty()) {
            throw new TekniskException("Brevet Orientering til arbeidsgiver om avslag trenger en begrunnelsekode for " + vilkaarType);
        }
        return resultat.getBegrunnelser();
    }
}