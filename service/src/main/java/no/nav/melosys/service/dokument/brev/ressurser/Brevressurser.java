package no.nav.melosys.service.dokument.brev.ressurser;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class Brevressurser {
    private final Behandling behandling;
    private final SoeknadDokument søknad;
    private PersonDokument person;

    private final AvklarteVirksomheter avklarteVirksomheter;
    private final Bosted bosted;
    private final LandvelgerService landvelger;
    private final ArbeidsstedRessurs arbeidssteder;
    private final LovvalgsperiodeRessurs lovvalgsperioder;

    public Brevressurser(Behandling behandling,
                         KodeverkService kodeverkService,
                         LandvelgerService landvelger,
                         AvklarteVirksomheterService avklarteVirksomheterService,
                         AvklartefaktaService avklartefaktaService,
                         LovvalgsperiodeService lovvalgsperiodeService) throws TekniskException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        this.landvelger = landvelger;
        this.avklarteVirksomheter = new AvklarteVirksomheter(behandling, avklarteVirksomheterService, kodeverkService);
        this.bosted = new Bosted(getSøknad(), getPerson(), kodeverkService);
        this.arbeidssteder = new ArbeidsstedRessurs(behandling, getSøknad(), getAvklarteVirksomheter(), avklartefaktaService);
        this.lovvalgsperioder = new LovvalgsperiodeRessurs(behandling, lovvalgsperiodeService);
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public SoeknadDokument getSøknad() {
        return søknad;
    }

    public PersonDokument getPerson() throws TekniskException {
        if (person == null) {
            this.person = SaksopplysningerUtils.hentPersonDokument(behandling);
        }
        return person;
    }

    public AvklarteVirksomheter getAvklarteVirksomheter() {
        return avklarteVirksomheter;
    }

    public Bosted getBosted() {
        return bosted;
    }

    public LandvelgerService getLandvelger() {
        return landvelger;
    }

    public ArbeidsstedRessurs getArbeidssteder() {
        return arbeidssteder;
    }

    public LovvalgsperiodeRessurs getLovvalgsperioder() {
        return lovvalgsperioder;
    }
}