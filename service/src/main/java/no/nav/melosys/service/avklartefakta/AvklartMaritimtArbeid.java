package no.nav.melosys.service.avklartefakta;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.Maritimtyper;

public class AvklartMaritimtArbeid {
    private final String navn;
    private String land;
    private Maritimtyper maritimtyper;

    public AvklartMaritimtArbeid(String navn) {
        this.navn = navn;
    }

    public void leggTilFakta(Avklartefakta avklartefakta) {
        String fakta = avklartefakta.getFakta();
        switch (avklartefakta.getType()) {
            case SOKKEL_ELLER_SKIP:
                maritimtyper = Maritimtyper.valueOf(fakta);
                break;
            case ARBEIDSLAND:
                land = fakta;
                break;
            default:
        }
    }

    public String getLand() {
        return land;
    }

    public String getNavn() {
        return navn;
    }

    public Maritimtyper getMaritimtype() {
        return maritimtyper;
    }
}