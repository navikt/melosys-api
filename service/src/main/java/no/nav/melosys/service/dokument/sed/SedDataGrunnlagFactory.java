package no.nav.melosys.service.dokument.sed;

import java.util.Arrays;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlag;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagMedSoknad;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagUtenSoknad;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class SedDataGrunnlagFactory {
    private final AvklartefaktaService avklartefaktaService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final KodeverkService kodeverkService;
    private final PersondataFasade persondataFasade;

    public SedDataGrunnlagFactory(AvklartefaktaService avklartefaktaService,
                                  AvklarteVirksomheterSystemService avklarteVirksomheterService,
                                  KodeverkService kodeverkService, PersondataFasade persondataFasade) {
        this.avklartefaktaService = avklartefaktaService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.kodeverkService = kodeverkService;
        this.persondataFasade = persondataFasade;
    }

    public SedDataGrunnlag av(Behandling behandling) {
        Behandlingsgrunnlag behandlingsgrunnlag = behandling.getBehandlingsgrunnlag();
        if (behandlingsgrunnlag != null) {
            return new SedDataGrunnlagMedSoknad(behandling, kodeverkService, avklarteVirksomheterService,
                avklartefaktaService, hentPersondata(behandling));
        } else {
            return new SedDataGrunnlagUtenSoknad(behandling, kodeverkService, hentPersondata(behandling));
        }
    }

    public SedDataGrunnlag av(Behandling behandling, SedType sedType) {
        if (Arrays.asList(SedType.A002, SedType.A011).contains(sedType)) {
            return new SedDataGrunnlagUtenSoknad(behandling, kodeverkService, hentPersondata(behandling));
        }
        return av(behandling);
    }

    private Persondata hentPersondata(Behandling behandling) {
        if (avklartefaktaService.hentAvklarteMedfølgendeBarn(behandling.getId()).finnes()) {
            return persondataFasade.hentPerson(behandling.getFagsak().hentAktørID(), Informasjonsbehov.MED_FAMILIERELASJONER);
        } else {
            return persondataFasade.hentPerson(behandling.getFagsak().hentAktørID());
        }
    }
}
