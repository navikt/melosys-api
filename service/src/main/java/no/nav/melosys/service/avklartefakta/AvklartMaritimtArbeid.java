package no.nav.melosys.service.avklartefakta;

import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Maritimtyper;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.ARBEIDSLAND;
import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.SOKKEL_ELLER_SKIP;

public class AvklartMaritimtArbeid {
    private final String navn;
    private String land;
    private Maritimtyper maritimtype;

    public static AvklartMaritimtArbeid av(Map.Entry<String, List<Avklartefakta>> entry) {
        return new AvklartMaritimtArbeid(entry.getKey(), entry.getValue());
    }

    public AvklartMaritimtArbeid(String navn, List<Avklartefakta> maritimeFakta) {
        this.navn = navn;
        maritimeFakta.forEach(this::leggTilFakta);
    }

    private void leggTilFakta(Avklartefakta avklartefakta) {
        String fakta = avklartefakta.getFakta();
        Avklartefaktatyper type = avklartefakta.getType();

        if (type == SOKKEL_ELLER_SKIP) {
            maritimtype = Maritimtyper.valueOf(fakta);
        } else if (type == ARBEIDSLAND) {
            land = fakta;
        }
    }

    public String getLand() {
        return land;
    }

    public String getNavn() {
        return navn;
    }

    public Maritimtyper getMaritimtype() {
        return maritimtype;
    }
}