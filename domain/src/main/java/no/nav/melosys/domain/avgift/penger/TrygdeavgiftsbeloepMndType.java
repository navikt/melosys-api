package no.nav.melosys.domain.avgift.penger;

public class TrygdeavgiftsbeloepMndType extends PengerType {
    @Override
    public String[] getPropertyNames() {
        return new String[]{"trygdeavgift_beloep_mnd_verdi", "trygdeavgift_beloep_mnd_valuta"};
    }
}
