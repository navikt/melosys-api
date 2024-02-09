package no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.kodeverk.Landkoder;

public class RepresentantIUtlandet {
    private String representantNavn;
    private List<String> adresselinjer = new ArrayList<>();
    private String representantLand;

    public static RepresentantIUtlandet av(String representantNavn, List<String> adresselinjer, Landkoder representantLand) {
        RepresentantIUtlandet representantIUtlandet = new RepresentantIUtlandet();
        representantIUtlandet.representantNavn = representantNavn;
        representantIUtlandet.adresselinjer = adresselinjer;
        representantIUtlandet.representantLand = representantLand.getKode();
        return representantIUtlandet;
    }

    public String getRepresentantNavn() {
        return representantNavn;
    }

    public void setRepresentantNavn(String representantNavn) {
        this.representantNavn = representantNavn;
    }

    public List<String> getAdresselinjer() {
        return adresselinjer;
    }

    public void setAdresselinjer(List<String> adresselinjer) {
        this.adresselinjer = adresselinjer;
    }

    public String getRepresentantLand() {
        return representantLand;
    }

    public void setRepresentantLand(String representantLand) {
        this.representantLand = representantLand;
    }
}
