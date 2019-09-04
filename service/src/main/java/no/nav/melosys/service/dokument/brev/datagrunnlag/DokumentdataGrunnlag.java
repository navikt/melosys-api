package no.nav.melosys.service.dokument.brev.datagrunnlag;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class DokumentdataGrunnlag {
    private final Behandling behandling;
    private final SoeknadDokument søknad;
    private PersonDokument person;

    private final AvklarteVirksomheterGrunnlag avklarteVirksomheterGrunnlag;
    private final BostedGrunnlag bostedGrunnlag;
    private final ArbeidsstedGrunnlag arbeidssteder;

    public DokumentdataGrunnlag(Behandling behandling,
                                KodeverkService kodeverkService,
                                AvklarteVirksomheterService avklarteVirksomheterService,
                                AvklartefaktaService avklartefaktaService) throws TekniskException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);
        this.avklarteVirksomheterGrunnlag = new AvklarteVirksomheterGrunnlag(behandling, avklarteVirksomheterService, kodeverkService);
        this.bostedGrunnlag = new BostedGrunnlag(getSøknad(), getPerson(), kodeverkService);
        this.arbeidssteder = new ArbeidsstedGrunnlag(behandling, getSøknad(), getAvklarteVirksomheterGrunnlag(), avklartefaktaService);
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public SoeknadDokument getSøknad() {
        return søknad;
    }

    public PersonDokument getPerson() {
        return person;
    }

    public AvklarteVirksomheterGrunnlag getAvklarteVirksomheterGrunnlag() {
        return avklarteVirksomheterGrunnlag;
    }

    public BostedGrunnlag getBostedGrunnlag() {
        return bostedGrunnlag;
    }

    public ArbeidsstedGrunnlag getArbeidssteder() {
        return arbeidssteder;
    }
}