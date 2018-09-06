package no.nav.melosys.domain.dokument.soeknad;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;

@XmlRootElement
public class SoeknadDokument extends SaksopplysningDokument {

    public OpplysningerOmBrukeren personOpplysninger;

    // Opplysninger om arbeid i utlandet
    public List<ArbeidUtland> arbeidUtland = new ArrayList<>();

    // Opplysninger om foretak i utlandet
    public List<ForetakUtland> foretakUtland = new ArrayList<>();

    // Opplysninger om opphold i utland
    public OppholdUtland oppholdUtland;

    // Opplysninger om arbeid i Norge
    public ArbeidNorge arbeidNorge;

    public SelvstendigArbeid selvstendigArbeid;

    // Opplysninger om juridiske arbeidsgiver i Norge
    public JuridiskArbeidsgiverNorge juridiskArbeidsgiverNorge;

    // Opplysninger om arbeidsinntekt
    public Arbeidsinntekt arbeidsinntekt;

    // Bekreftelser fra arbeidsgiveren
    public ArbeidsgiversBekreftelse arbeidsgiversBekreftelse;

    public MaritimtArbeid maritimtArbeid;

    public Bosted bosted;

}