package no.nav.melosys.service.dokument.brev.datagrunnlag;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class BrevdataGrunnlagFactory {
    private final AvklartefaktaService avklartefaktaService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final KodeverkService kodeverkService;
    private final PersondataFasade persondataFasade;

    @Autowired
    public BrevdataGrunnlagFactory(AvklartefaktaService avklartefaktaService,
                                   AvklarteVirksomheterSystemService avklarteVirksomheterService,
                                   KodeverkService kodeverkService, PersondataFasade persondataFasade) {
        this.avklartefaktaService = avklartefaktaService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.kodeverkService = kodeverkService;
        this.persondataFasade = persondataFasade;
    }

    public BrevDataGrunnlag av(DoksysBrevbestilling brevbestilling) {
        return new BrevDataGrunnlag(brevbestilling,
            kodeverkService,
            avklarteVirksomheterService,
            avklartefaktaService,
            hentPersondata(brevbestilling.getBehandling())
        );
    }

    private Persondata hentPersondata(Behandling behandling) {
        return persondataFasade.hentPerson(behandling.getFagsak().hentAktørID(),
            Informasjonsbehov.MED_FAMILIERELASJONER);
    }
}
