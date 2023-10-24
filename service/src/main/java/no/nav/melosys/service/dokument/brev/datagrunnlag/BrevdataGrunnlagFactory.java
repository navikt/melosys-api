package no.nav.melosys.service.dokument.brev.datagrunnlag;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.avklartefakta.OppsummerteAvklarteFaktaService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class BrevdataGrunnlagFactory {
    private final AvklartefaktaService avklartefaktaService;
    private final OppsummerteAvklarteFaktaService oppsummerteAvklarteFaktaService;
    private final KodeverkService kodeverkService;
    private final PersondataFasade persondataFasade;

    public BrevdataGrunnlagFactory(AvklartefaktaService avklartefaktaService,
                                   OppsummerteAvklarteFaktaService oppsummerteAvklarteFaktaService,
                                   KodeverkService kodeverkService, PersondataFasade persondataFasade) {
        this.avklartefaktaService = avklartefaktaService;
        this.oppsummerteAvklarteFaktaService = oppsummerteAvklarteFaktaService;
        this.kodeverkService = kodeverkService;
        this.persondataFasade = persondataFasade;
    }

    public BrevDataGrunnlag av(DoksysBrevbestilling brevbestilling) {
        return new BrevDataGrunnlag(brevbestilling,
            kodeverkService,
                oppsummerteAvklarteFaktaService,
            avklartefaktaService,
            hentPersondata(brevbestilling.getBehandling())
        );
    }

    private Persondata hentPersondata(Behandling behandling) {
        if (avklartefaktaService.hentAvklarteMedfølgendeBarn(behandling.getId()).finnes()) {
            return persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID(), Informasjonsbehov.MED_FAMILIERELASJONER);
        } else {
            return persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID());
        }
    }
}
