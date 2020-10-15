package no.nav.melosys.domain.behandlingsgrunnlag;

import no.nav.melosys.domain.dokument.soeknad.ArbeidsgiversBekreftelse;
import no.nav.melosys.domain.dokument.soeknad.Arbeidsinntekt;

public class Soeknad extends BehandlingsgrunnlagData {
    // Opplysninger om arbeidsinntekt
    public Arbeidsinntekt arbeidsinntekt = new Arbeidsinntekt();

    // Bekreftelser fra arbeidsgiveren
    public ArbeidsgiversBekreftelse arbeidsgiversBekreftelse = new ArbeidsgiversBekreftelse();

}