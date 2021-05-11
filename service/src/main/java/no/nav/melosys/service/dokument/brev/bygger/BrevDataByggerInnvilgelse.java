package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeBarn;
import no.nav.melosys.domain.person.familie.IkkeOmfattetBarn;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
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
    private final PersondataFasade persondataFasade;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;

    public BrevDataByggerInnvilgelse(AvklartefaktaService avklartefaktaService,
                                     LandvelgerService landvelgerService,
                                     LovvalgsperiodeService lovvalgsperiodeService,
                                     AnmodningsperiodeService anmodningsperiodeService,
                                     BrevbestillingDto brevbestillingDto,
                                     VilkaarsresultatService vilkaarsresultatService,
                                     PersondataFasade persondataFasade,
                                     BehandlingsgrunnlagService behandlingsgrunnlagService) {
        this.landvelgerService = landvelgerService;
        this.avklartefaktaService = avklartefaktaService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.brevbestillingDto = brevbestillingDto;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.vilkaarsresultatService = vilkaarsresultatService;
        this.brevbyggerA1 = null;
        this.persondataFasade = persondataFasade;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
    }

    public BrevDataByggerInnvilgelse(AvklartefaktaService avklartefaktaService,
                                     LandvelgerService landvelgerService,
                                     LovvalgsperiodeService lovvalgsperiodeService,
                                     AnmodningsperiodeService anmodningsperiodeService,
                                     BrevbestillingDto brevbestillingDto,
                                     BrevDataByggerA1 brevbyggerA1,
                                     VilkaarsresultatService vilkaarsresultatService,
                                     PersondataFasade persondataFasade,
                                     BehandlingsgrunnlagService behandlingsgrunnlagService) {
        this.landvelgerService = landvelgerService;
        this.avklartefaktaService = avklartefaktaService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.brevbestillingDto = brevbestillingDto;
        this.brevbyggerA1 = brevbyggerA1;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.vilkaarsresultatService = vilkaarsresultatService;
        this.persondataFasade = persondataFasade;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
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

        Set<Maritimtyper> maritimTyper = avklartefaktaService.hentMaritimTyper(behandlingID);
        if (!maritimTyper.isEmpty()) {
            brevdata.avklartMaritimType = maritimTyper.iterator().next();
        }

        brevdata.setAnmodningsperiodesvar(anmodningsperiodeService.hentAnmodningsperioder(behandlingID)
            .stream()
            .filter(Anmodningsperiode::erSendtUtland)
            .findFirst()
            .map(Anmodningsperiode::getAnmodningsperiodeSvar)
            .orElse(null));

        brevdata.erArt16UtenArt12 = vilkaarsresultatService.harVilkaarForArtikkel16(behandlingID) && !vilkaarsresultatService.harVilkaarForArtikkel12(behandlingID);
        brevdata.erTuristskip = vilkaarsresultatService.oppfyllerVilkaar(behandlingID, Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP);
        brevdata.avklarteMedfolgendeBarn = hentAvklarteMedfølgendeBarn(behandlingID);

        return brevdata;
    }

    private AvklarteMedfolgendeBarn hentAvklarteMedfølgendeBarn(long behandlingID) throws FunksjonellException, IntegrasjonException {
        AvklarteMedfolgendeBarn avklarteMedfolgendeBarn = avklartefaktaService.hentAvklarteMedfølgendeBarn(behandlingID);
        Map<String, MedfolgendeFamilie> medfølgendeBarn = hentMedfølgendeBarn(behandlingID);
        for (OmfattetFamilie omfattetBarn : avklarteMedfolgendeBarn.barnOmfattetAvNorskTrygd) {
            if (!medfølgendeBarn.containsKey(omfattetBarn.getUuid())) {
                throw new FunksjonellException("Avklart medfølgende barn " + omfattetBarn.getUuid() + " finnes ikke i behandlingsgrunnlaget");
            }
            MedfolgendeFamilie barn = medfølgendeBarn.get(omfattetBarn.getUuid());
            omfattetBarn.setSammensattNavn(barn.fnr != null ? persondataFasade.hentSammensattNavn(barn.fnr) : barn.navn);
        }
        for (IkkeOmfattetBarn ikkeOmfattetBarn : avklarteMedfolgendeBarn.barnIkkeOmfattetAvNorskTrygd) {
            if (!medfølgendeBarn.containsKey(ikkeOmfattetBarn.uuid)) {
                throw new FunksjonellException("Avklart medfølgende barn " + ikkeOmfattetBarn.uuid + " finnes ikke i behandlingsgrunnlaget");
            }
            MedfolgendeFamilie barn = medfølgendeBarn.get(ikkeOmfattetBarn.uuid);
            ikkeOmfattetBarn.sammensattNavn = barn.fnr != null ? persondataFasade.hentSammensattNavn(barn.fnr) : barn.navn;
        }
        return avklarteMedfolgendeBarn;
    }

    private Map<String, MedfolgendeFamilie> hentMedfølgendeBarn(long behandlingID) throws IkkeFunnetException {
        Behandlingsgrunnlag behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID);
        return behandlingsgrunnlag == null ? Collections.emptyMap()
            : behandlingsgrunnlag.getBehandlingsgrunnlagdata().hentMedfølgendeBarn();
    }
}
