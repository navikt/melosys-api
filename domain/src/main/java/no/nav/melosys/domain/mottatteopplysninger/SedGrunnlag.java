package no.nav.melosys.domain.mottatteopplysninger;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Overgangsregelbestemmelser;

public class SedGrunnlag extends MottatteOpplysningerData {
    public List<Overgangsregelbestemmelser> overgangsregelbestemmelser = new ArrayList<>();
    public String ytterligereInformasjon;
}
