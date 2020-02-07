package no.nav.melosys.domain.dokument.soeknad;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.bind.annotation.XmlRootElement;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.grunnlag.BehandlingsgrunnlagData;
import org.apache.commons.lang3.StringUtils;

//TODO: flytte til no.nav.melosys.domain.grunnlag
@XmlRootElement
public class SoeknadDokument extends BehandlingsgrunnlagData implements SaksopplysningDokument {
    // Opplysninger om juridiske arbeidsgiver i Norge
    public JuridiskArbeidsgiverNorge juridiskArbeidsgiverNorge = new JuridiskArbeidsgiverNorge();

    // Opplysninger om arbeidsinntekt
    public Arbeidsinntekt arbeidsinntekt = new Arbeidsinntekt();

    // Bekreftelser fra arbeidsgiveren
    public ArbeidsgiversBekreftelse arbeidsgiversBekreftelse = new ArbeidsgiversBekreftelse();

    @Override
    public Set<String> hentAlleOrganisasjonsnumre() {
        return Stream.of(selvstendigArbeid.hentAlleOrganisasjonsnumre(),
                         juridiskArbeidsgiverNorge.hentManueltRegistrerteArbeidsgiverOrgnumre())
                .flatMap(i -> i)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }
}