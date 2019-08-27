package no.nav.melosys.service.dokument.brev.ressurser;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class Dokumentressurser {
    private final Behandling behandling;
    private final SoeknadDokument søknad;
    private PersonDokument person;

    private final AvklarteVirksomheter avklarteVirksomheter;
    private final Bosted bosted;
    private final ArbeidsstedRessurs arbeidssteder;

    public Dokumentressurser(Behandling behandling,
                             KodeverkService kodeverkService,
                             AvklarteVirksomheterService avklarteVirksomheterService,
                             AvklartefaktaService avklartefaktaService) throws TekniskException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);
        this.avklarteVirksomheter = new AvklarteVirksomheter(behandling, avklarteVirksomheterService, kodeverkService);
        this.bosted = new Bosted(getSøknad(), getPerson(), kodeverkService);
        this.arbeidssteder = new ArbeidsstedRessurs(behandling, getSøknad(), getAvklarteVirksomheter(), avklartefaktaService);
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

    public AvklarteVirksomheter getAvklarteVirksomheter() {
        return avklarteVirksomheter;
    }

    public Bosted getBosted() {
        return bosted;
    }

    public ArbeidsstedRessurs getArbeidssteder() {
        return arbeidssteder;
    }
}