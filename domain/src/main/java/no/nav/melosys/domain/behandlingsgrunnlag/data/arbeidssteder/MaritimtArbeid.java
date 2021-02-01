package no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder;

import no.nav.melosys.domain.kodeverk.Innretningstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader;

public class MaritimtArbeid {
    public String enhetNavn;
    public Fartsomrader fartsomradeKode;
    public String flaggLandkode;
    public String innretningLandkode;
    public Innretningstyper innretningstype;
    public String territorialfarvann;
}
