package no.nav.melosys.domain.avgift.penger;

public class AvgiftspliktigInntektMndType extends PengerType {
    @Override
    public String[] getPropertyNames() {
        return new String[]{"avgiftspliktig_inntekt_mnd_verdi", "avgiftspliktig_inntekt_mnd_valuta"};
    }
}
