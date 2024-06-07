package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.behandling.VilkaarsresultatService;

public class BrevDataByggerInnvilgelse implements BrevDataBygger {
    private final LandvelgerService landvelgerService;
    private final AvklartefaktaService avklartefaktaService;
    private final BrevbestillingDto brevbestillingDto;
    private final BrevDataByggerA1 brevbyggerA1;
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final VilkaarsresultatService vilkaarsresultatService;
    private final PersondataFasade persondataFasade;
    private final MottatteOpplysningerService mottatteOpplysningerService;

    public BrevDataByggerInnvilgelse(AvklartefaktaService avklartefaktaService,
                                     LandvelgerService landvelgerService,
                                     LovvalgsperiodeService lovvalgsperiodeService,
                                     AnmodningsperiodeService anmodningsperiodeService,
                                     BrevbestillingDto brevbestillingDto,
                                     VilkaarsresultatService vilkaarsresultatService,
                                     PersondataFasade persondataFasade,
                                     MottatteOpplysningerService mottatteOpplysningerService) {
        this.landvelgerService = landvelgerService;
        this.avklartefaktaService = avklartefaktaService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.brevbestillingDto = brevbestillingDto;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.vilkaarsresultatService = vilkaarsresultatService;
        this.brevbyggerA1 = null;
        this.persondataFasade = persondataFasade;
        this.mottatteOpplysningerService = mottatteOpplysningerService;
    }

    public BrevDataByggerInnvilgelse(AvklartefaktaService avklartefaktaService,
                                     LandvelgerService landvelgerService,
                                     LovvalgsperiodeService lovvalgsperiodeService,
                                     AnmodningsperiodeService anmodningsperiodeService,
                                     BrevbestillingDto brevbestillingDto,
                                     BrevDataByggerA1 brevbyggerA1,
                                     VilkaarsresultatService vilkaarsresultatService,
                                     PersondataFasade persondataFasade,
                                     MottatteOpplysningerService mottatteOpplysningerService) {
        this.landvelgerService = landvelgerService;
        this.avklartefaktaService = avklartefaktaService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.brevbestillingDto = brevbestillingDto;
        this.brevbyggerA1 = brevbyggerA1;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.vilkaarsresultatService = vilkaarsresultatService;
        this.persondataFasade = persondataFasade;
        this.mottatteOpplysningerService = mottatteOpplysningerService;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        long behandlingID = dataGrunnlag.getBehandling().getId();

        // Bruker skal ha A1 som vedlegg - Arbeidsgiver skal ikke
        var brevDataInnvilgelse = new BrevDataInnvilgelse(brevbestillingDto, saksbehandler);
        if (brevbyggerA1 != null) {
            brevDataInnvilgelse.setVedleggA1((BrevDataA1) brevbyggerA1.lag(dataGrunnlag, saksbehandler));
        }

        brevDataInnvilgelse.setPersonNavn(dataGrunnlag.getPerson().getSammensattNavn());
        brevDataInnvilgelse.setLovvalgsperiode(lovvalgsperiodeService.hentLovvalgsperiode(behandlingID));
        brevDataInnvilgelse.setArbeidsland(landvelgerService.hentArbeidsland(behandlingID).getBeskrivelse());
        brevDataInnvilgelse.setBostedsland(landvelgerService.hentBostedsland(behandlingID, dataGrunnlag.getMottatteOpplysningerData()).getLandkodeobjekt().getBeskrivelse());

        brevDataInnvilgelse.setTrygdemyndighetsland(landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID).stream()
            .findFirst()
            .map(Land_iso2::getBeskrivelse)
            .orElse(null));

        if (dataGrunnlag.getAvklarteVirksomheterGrunnlag().antallVirksomheter() != 1) {
            throw new FunksjonellException("Ingen eller flere enn én norsk eller utenlandsk virksomhet forsøkt brukt i innvilgelsesbrev");
        }

        brevDataInnvilgelse.setHovedvirksomhet(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentHovedvirksomhet());

        Set<Maritimtyper> maritimTyper = avklartefaktaService.hentMaritimTyper(behandlingID);
        if (!maritimTyper.isEmpty()) {
            brevDataInnvilgelse.setAvklartMaritimType(maritimTyper.iterator().next());
        }

        brevDataInnvilgelse.setAnmodningsperiodesvar(anmodningsperiodeService.hentAnmodningsperioder(behandlingID)
            .stream()
            .filter(Anmodningsperiode::erSendtUtland)
            .findFirst()
            .map(Anmodningsperiode::getAnmodningsperiodeSvar)
            .orElse(null));

        brevDataInnvilgelse.setArt16UtenArt12(vilkaarsresultatService.harVilkaarForUnntak(behandlingID) && !vilkaarsresultatService.harVilkaarForUtsending(behandlingID));
        brevDataInnvilgelse.setTuristskip(vilkaarsresultatService.oppfyllerVilkaar(behandlingID, Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP));
        brevDataInnvilgelse.setAvklarteMedfolgendeBarn(hentAvklarteMedfølgendeBarn(behandlingID));

        return brevDataInnvilgelse;
    }

    private AvklarteMedfolgendeFamilie hentAvklarteMedfølgendeBarn(long behandlingID) {
        var avklarteMedfolgendeBarn = avklartefaktaService.hentAvklarteMedfølgendeBarn(behandlingID);
        Map<String, MedfolgendeFamilie> medfølgendeBarn = hentMedfølgendeBarn(behandlingID);
        for (OmfattetFamilie omfattetBarn : avklarteMedfolgendeBarn.getFamilieOmfattetAvNorskTrygd()) {
            if (!medfølgendeBarn.containsKey(omfattetBarn.getUuid())) {
                throw new FunksjonellException("Avklart medfølgende barn " + omfattetBarn.getUuid() + " finnes ikke i mottatteOpplysningeret");
            }
            MedfolgendeFamilie barn = medfølgendeBarn.get(omfattetBarn.getUuid());
            omfattetBarn.setIdent(barn.getFnr());
            omfattetBarn.setSammensattNavn(barn.getFnr() != null ? persondataFasade.hentSammensattNavn(barn.getFnr()) : barn.getNavn());
        }
        for (IkkeOmfattetFamilie ikkeOmfattetBarn : avklarteMedfolgendeBarn.getFamilieIkkeOmfattetAvNorskTrygd()) {
            if (!medfølgendeBarn.containsKey(ikkeOmfattetBarn.getUuid())) {
                throw new FunksjonellException("Avklart medfølgende barn " + ikkeOmfattetBarn.getUuid() + " finnes ikke i mottatteOpplysningeret");
            }
            MedfolgendeFamilie barn = medfølgendeBarn.get(ikkeOmfattetBarn.getUuid());
            ikkeOmfattetBarn.setSammensattNavn(barn.getFnr() != null ? persondataFasade.hentSammensattNavn(barn.getFnr()) : barn.getNavn());
            ikkeOmfattetBarn.setIdent(barn.getFnr());
        }
        return avklarteMedfolgendeBarn;
    }

    private Map<String, MedfolgendeFamilie> hentMedfølgendeBarn(long behandlingID) {
        var mottatteOpplysninger = mottatteOpplysningerService.hentMottatteOpplysninger(behandlingID);
        return mottatteOpplysninger == null ? Collections.emptyMap()
            : mottatteOpplysninger.getMottatteOpplysningerData().hentMedfølgendeBarn();
    }
}
