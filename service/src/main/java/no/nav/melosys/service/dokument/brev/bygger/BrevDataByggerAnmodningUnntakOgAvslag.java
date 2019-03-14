package no.nav.melosys.service.dokument.brev.bygger;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.Adresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART11_4_1;

public class BrevDataByggerAnmodningUnntakOgAvslag extends AbstraktDokumentDataBygger implements BrevDataBygger {

    private AvklarteVirksomheterService avklarteVirksomheterService;
    private VilkaarsresultatRepository vilkaarsresultatRepository;

    public BrevDataByggerAnmodningUnntakOgAvslag(AvklartefaktaService avklartefaktaService,
                                                 AvklarteVirksomheterService avklarteVirksomheterService, VilkaarsresultatRepository vilkaarsresultatRepository) {
        super(null, null, avklartefaktaService);
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
    }

    Function<OrganisasjonDokument, Adresse> ingenAdresse = org -> null;

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);

        BrevDataAnmodningUnntakOgAvslag brevData = new BrevDataAnmodningUnntakOgAvslag(saksbehandler);

        List<AvklartVirksomhet> avklarteVirksomheter = avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, ingenAdresse);
        if (avklarteVirksomheter.size() != 1) {
            throw new TekniskException("Trenger minst en norsk virksomhet for avslag og ART16.1");
        }

        brevData.hovedvirksomhet = avklarteVirksomheter.iterator().next();
        brevData.arbeidsland = avklarArbeidsLand(behandling);

        return brevData;
    }

    private Landkoder avklarArbeidsLand(Behandling behandling) throws FunksjonellException, TekniskException {

        List<Vilkaar> oppfylteVilkår = vilkaarsresultatRepository.findByBehandlingsresultatId(behandling.getId()).stream()
            .filter(Vilkaarsresultat::isOppfylt).map(Vilkaarsresultat::getVilkaar).collect(Collectors.toList());

        if (oppfylteVilkår.contains(FO_883_2004_ART11_4_1)) {
            // Bruker FLAGGLAND
            return Landkoder.valueOf(avklartefaktaService.hentAvklarteFakta(behandling.getId(), Avklartefaktatype.FLAGGLAND).getFakta());
        } else {
            SoeknadDokument soeknadDokument = SaksopplysningerUtils.hentSøknadDokument(behandling);
            if (!soeknadDokument.oppholdUtland.oppholdslandKoder.isEmpty()) {
                return Landkoder.valueOf(soeknadDokument.oppholdUtland.oppholdslandKoder.get(0)); //FIXME må erstattes med søknadDokument.land (søknadsland).
            }
        }
        throw new TekniskException("Kunne ikke hente ut arbeidsland for behandlingsresultat " + behandling.getId());
    }
}