package no.nav.melosys.domain.dokument.soeknad;

import javax.xml.bind.annotation.XmlRootElement;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;

//TODO: flytte til no.nav.melosys.domain.behandlingsgrunnlag
@XmlRootElement
public class SoeknadDokument extends BehandlingsgrunnlagData {
    // Opplysninger om arbeidsinntekt
    public Arbeidsinntekt arbeidsinntekt = new Arbeidsinntekt();

    // Bekreftelser fra arbeidsgiveren
    public ArbeidsgiversBekreftelse arbeidsgiversBekreftelse = new ArbeidsgiversBekreftelse();

}