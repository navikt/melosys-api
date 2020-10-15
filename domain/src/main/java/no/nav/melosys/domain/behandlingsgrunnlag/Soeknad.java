package no.nav.melosys.domain.behandlingsgrunnlag;

import no.nav.melosys.domain.behandlingsgrunnlag.soeknad.ArbeidsgiversBekreftelse;
import no.nav.melosys.domain.behandlingsgrunnlag.soeknad.Arbeidsinntekt;

public class Soeknad extends BehandlingsgrunnlagData {
    // Opplysninger om arbeidsinntekt
    public Arbeidsinntekt arbeidsinntekt = new Arbeidsinntekt();

    // Bekreftelser fra arbeidsgiveren
    public ArbeidsgiversBekreftelse arbeidsgiversBekreftelse = new ArbeidsgiversBekreftelse();

}