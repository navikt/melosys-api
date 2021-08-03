package no.nav.melosys.service.dokument.sed;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlag;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagMedSoknad;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagUtenSoknad;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SedDataGrunnlagFactory {
    private final AvklartefaktaService avklartefaktaService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final KodeverkService kodeverkService;
    private final PersondataFasade persondataFasade;
    private final Unleash unleash;

    @Autowired
    public SedDataGrunnlagFactory(AvklartefaktaService avklartefaktaService,
                                  AvklarteVirksomheterSystemService avklarteVirksomheterService,
                                  KodeverkService kodeverkService, PersondataFasade persondataFasade, Unleash unleash) {
        this.avklartefaktaService = avklartefaktaService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.kodeverkService = kodeverkService;
        this.persondataFasade = persondataFasade;
        this.unleash = unleash;
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

    private Persondata hentPersondata(Behandling behandling) {
        if (unleash.isEnabled("melosys.sed.adresser.pdl")) {
            return persondataFasade.hentPerson(behandling.getFagsak().hentAktørID());
        }
        return behandling.hentPersonDokument();
    }
}
