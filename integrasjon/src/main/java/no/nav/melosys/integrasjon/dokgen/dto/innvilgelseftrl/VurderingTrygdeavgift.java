package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

public class VurderingTrygdeavgift {
    private TrygdeavgiftInfo norsk;
    private TrygdeavgiftInfo utenlandsk;

    public VurderingTrygdeavgift(TrygdeavgiftInfo norsk, TrygdeavgiftInfo utenlandsk) {
        this.norsk = norsk;
        this.utenlandsk = utenlandsk;
    }

    public TrygdeavgiftInfo getNorsk() {
        return norsk;
    }

    public TrygdeavgiftInfo getUtenlandsk() {
        return utenlandsk;
    }
}
