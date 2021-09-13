package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeBarn;
import no.nav.melosys.domain.person.familie.IkkeOmfattetBarn;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import org.springframework.beans.factory.annotation.Qualifier;

public class BrevDataByggerInnvilgelse implements BrevDataBygger {
    private final LandvelgerService landvelgerService;
    private final AvklartefaktaService avklartefaktaService;
    private final BrevbestillingRequest brevbestillingRequest;
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
                                     BrevbestillingRequest brevbestillingRequest,
                                     VilkaarsresultatService vilkaarsresultatService,
                                     @Qualifier("system") PersondataFasade persondataFasade,
                                     BehandlingsgrunnlagService behandlingsgrunnlagService) {
        this.landvelgerService = landvelgerService;
        this.avklartefaktaService = avklartefaktaService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.brevbestillingRequest = brevbestillingRequest;
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
                                     BrevbestillingRequest brevbestillingRequest,
                                     BrevDataByggerA1 brevbyggerA1,
                                     VilkaarsresultatService vilkaarsresultatService,
                                     PersondataFasade persondataFasade,
                                     BehandlingsgrunnlagService behandlingsgrunnlagService) {
        this.landvelgerService = landvelgerService;
        this.avklartefaktaService = avklartefaktaService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.brevbestillingRequest = brevbestillingRequest;
        this.brevbyggerA1 = brevbyggerA1;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.vilkaarsresultatService = vilkaarsresultatService;
        this.persondataFasade = persondataFasade;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        long behandlingID = dataGrunnlag.getBehandling().getId();

        // Bruker skal ha A1 som vedlegg - Arbeidsgiver skal ikke
        var brevDataInnvilgelse = new BrevDataInnvilgelse(brevbestillingRequest, saksbehandler);
        if (brevbyggerA1 != null) {
            brevDataInnvilgelse.vedleggA1 = (BrevDataA1) brevbyggerA1.lag(dataGrunnlag, saksbehandler);
        }

        brevDataInnvilgelse.personNavn = dataGrunnlag.getPerson().getSammensattNavn();
        brevDataInnvilgelse.lovvalgsperiode = lovvalgsperiodeService.hentValidertLovvalgsperiode(behandlingID);
        brevDataInnvilgelse.arbeidsland = landvelgerService.hentArbeidsland(behandlingID).getBeskrivelse();
        brevDataInnvilgelse.bostedsland = Landkoder.valueOf(landvelgerService.hentBostedsland(behandlingID, dataGrunnlag.getBehandlingsgrunnlagData())).getBeskrivelse();

        brevDataInnvilgelse.trygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID).stream()
            .findFirst()
            .map(Landkoder::getBeskrivelse)
            .orElse(null);

        if (dataGrunnlag.getAvklarteVirksomheterGrunnlag().antallVirksomheter() != 1) {
            throw new FunksjonellException("Ingen eller flere enn én norsk eller utenlandsk virksomhet forsøkt brukt i innvilgelsesbrev");
        }

        brevDataInnvilgelse.hovedvirksomhet = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentHovedvirksomhet();

        Set<Maritimtyper> maritimTyper = avklartefaktaService.hentMaritimTyper(behandlingID);
        if (!maritimTyper.isEmpty()) {
            brevDataInnvilgelse.avklartMaritimType = maritimTyper.iterator().next();
        }

        brevDataInnvilgelse.setAnmodningsperiodesvar(anmodningsperiodeService.hentAnmodningsperioder(behandlingID)
            .stream()
            .filter(Anmodningsperiode::erSendtUtland)
            .findFirst()
            .map(Anmodningsperiode::getAnmodningsperiodeSvar)
            .orElse(null));

        brevDataInnvilgelse.erArt16UtenArt12 = vilkaarsresultatService.harVilkaarForArtikkel16(behandlingID) && !vilkaarsresultatService.harVilkaarForArtikkel12(behandlingID);
        brevDataInnvilgelse.erTuristskip = vilkaarsresultatService.oppfyllerVilkaar(behandlingID, Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP);
        brevDataInnvilgelse.avklarteMedfolgendeBarn = hentAvklarteMedfølgendeBarn(behandlingID);

        return brevDataInnvilgelse;
    }

    private AvklarteMedfolgendeBarn hentAvklarteMedfølgendeBarn(long behandlingID) {
        var avklarteMedfolgendeBarn = avklartefaktaService.hentAvklarteMedfølgendeBarn(behandlingID);
        Map<String, MedfolgendeFamilie> medfølgendeBarn = hentMedfølgendeBarn(behandlingID);
        for (OmfattetFamilie omfattetBarn : avklarteMedfolgendeBarn.barnOmfattetAvNorskTrygd) {
            if (!medfølgendeBarn.containsKey(omfattetBarn.getUuid())) {
                throw new FunksjonellException("Avklart medfølgende barn " + omfattetBarn.getUuid() + " finnes ikke i behandlingsgrunnlaget");
            }
            MedfolgendeFamilie barn = medfølgendeBarn.get(omfattetBarn.getUuid());
            omfattetBarn.setIdent(barn.fnr);
            omfattetBarn.setSammensattNavn(barn.fnr != null ? persondataFasade.hentSammensattNavn(barn.fnr) : barn.navn);
        }
        for (IkkeOmfattetBarn ikkeOmfattetBarn : avklarteMedfolgendeBarn.barnIkkeOmfattetAvNorskTrygd) {
            if (!medfølgendeBarn.containsKey(ikkeOmfattetBarn.uuid)) {
                throw new FunksjonellException("Avklart medfølgende barn " + ikkeOmfattetBarn.uuid + " finnes ikke i behandlingsgrunnlaget");
            }
            MedfolgendeFamilie barn = medfølgendeBarn.get(ikkeOmfattetBarn.uuid);
            ikkeOmfattetBarn.sammensattNavn = barn.fnr != null ? persondataFasade.hentSammensattNavn(barn.fnr) : barn.navn;
            ikkeOmfattetBarn.ident = barn.fnr;
        }
        return avklarteMedfolgendeBarn;
    }

    private Map<String, MedfolgendeFamilie> hentMedfølgendeBarn(long behandlingID) {
        var behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID);
        return behandlingsgrunnlag == null ? Collections.emptyMap()
            : behandlingsgrunnlag.getBehandlingsgrunnlagdata().hentMedfølgendeBarn();
    }
}
