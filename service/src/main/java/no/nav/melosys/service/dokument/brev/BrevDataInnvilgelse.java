package no.nav.melosys.service.dokument.brev;

import java.util.Optional;

import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;

public class BrevDataInnvilgelse extends BrevData {
    private AnmodningsperiodeSvar anmodningsperiodesvar;
    public Lovvalgsperiode lovvalgsperiode;
    public String arbeidsland;
    public String bostedsland;
    public AvklartVirksomhet hovedvirksomhet;
    public boolean harAvklartMaritimTypeSokkel;
    public boolean harAvklartMaritimTypeSkip;
    public String trygdemyndighetsland;
    public BrevDataA1 vedleggA1;
    public String personNavn;
    public boolean erArt16UtenArt12;
    public boolean erTuristskip;

    public BrevDataInnvilgelse(BrevbestillingDto brevbestillingDto, String saksbehandler) {
        super(brevbestillingDto, saksbehandler);
    }

    public Optional<AnmodningsperiodeSvar> getAnmodningsperiodesvar() {
        return Optional.ofNullable(anmodningsperiodesvar);
    }

    public void setAnmodningsperiodesvar(AnmodningsperiodeSvar anmodningsperiodesvar) {
        this.anmodningsperiodesvar = anmodningsperiodesvar;
    }
}