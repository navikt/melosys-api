package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Optional;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;

public class BrevDataByggerInnvilgelse implements BrevDataBygger {
    private final LandvelgerService landvelgerService;
    private final AvklartefaktaService avklartefaktaService;
    private final BrevbestillingDto brevbestillingDto;
    private final BrevDataByggerA1 brevbyggerA1;
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final VilkaarsresultatService vilkaarsresultatService;

    public BrevDataByggerInnvilgelse(AvklartefaktaService avklartefaktaService,
                                     LandvelgerService landvelgerService,
                                     LovvalgsperiodeService lovvalgsperiodeService,
                                     AnmodningsperiodeService anmodningsperiodeService,
                                     BrevbestillingDto brevbestillingDto,
                                     VilkaarsresultatService vilkaarsresultatService) {
        this.landvelgerService = landvelgerService;
        this.avklartefaktaService = avklartefaktaService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.brevbestillingDto = brevbestillingDto;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.vilkaarsresultatService = vilkaarsresultatService;
        this.brevbyggerA1 = null;
    }

    public BrevDataByggerInnvilgelse(AvklartefaktaService avklartefaktaService,
                                     LandvelgerService landvelgerService,
                                     LovvalgsperiodeService lovvalgsperiodeService,
                                     AnmodningsperiodeService anmodningsperiodeService,
                                     BrevbestillingDto brevbestillingDto,
                                     BrevDataByggerA1 brevbyggerA1,
                                     VilkaarsresultatService vilkaarsresultatService) {
        this.landvelgerService = landvelgerService;
        this.avklartefaktaService = avklartefaktaService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.brevbestillingDto = brevbestillingDto;
        this.brevbyggerA1 = brevbyggerA1;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.vilkaarsresultatService = vilkaarsresultatService;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) throws FunksjonellException, TekniskException {
        long behandlingID = dataGrunnlag.getBehandling().getId();

        // Bruker skal ha A1 som vedlegg - Arbeidsgiver skal ikke
        BrevDataInnvilgelse brevdata = new BrevDataInnvilgelse(brevbestillingDto, saksbehandler);
        if (brevbyggerA1 != null) {
            brevdata.vedleggA1 = (BrevDataA1) brevbyggerA1.lag(dataGrunnlag, saksbehandler);
        }

        brevdata.personNavn = dataGrunnlag.getPerson().sammensattNavn;
        brevdata.lovvalgsperiode = lovvalgsperiodeService.hentValidertLovvalgsperiode(behandlingID);
        brevdata.arbeidsland = landvelgerService.hentArbeidsland(behandlingID).getBeskrivelse();
        brevdata.bostedsland = landvelgerService.hentBostedsland(behandlingID, dataGrunnlag.getBehandlingsgrunnlagData()).getBeskrivelse();

        brevdata.trygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID).stream()
            .findFirst()
            .map(Landkoder::getBeskrivelse)
            .orElse(null);

        if (dataGrunnlag.getAvklarteVirksomheterGrunnlag().antallVirksomheter() != 1) {
            throw new FunksjonellException("Ingen eller flere enn én norsk eller utenlandsk virksomhet forsøkt brukt i innvilgelsesbrev");
        }

        brevdata.hovedvirksomhet = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentHovedvirksomhet();

        Optional<Maritimtyper> maritimType = avklartefaktaService.hentMaritimType(behandlingID);
        maritimType.ifPresent(mt -> brevdata.avklartMaritimType = mt);

        brevdata.setAnmodningsperiodesvar(anmodningsperiodeService.hentAnmodningsperioder(behandlingID)
            .stream()
            .filter(Anmodningsperiode::erSendtUtland)
            .findFirst()
            .map(Anmodningsperiode::getAnmodningsperiodeSvar)
            .orElse(null));

        brevdata.erArt16UtenArt12 = vilkaarsresultatService.harVilkaarForArtikkel16(behandlingID) && !vilkaarsresultatService.harVilkaarForArtikkel12(behandlingID);
        brevdata.erTuristskip = vilkaarsresultatService.oppfyllerVilkaar(behandlingID, Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP);

        return brevdata;
    }
}