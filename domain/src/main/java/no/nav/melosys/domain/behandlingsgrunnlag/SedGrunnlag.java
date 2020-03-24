package no.nav.melosys.domain.behandlingsgrunnlag;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.eessi.Organisasjon;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Overgangsregelbestemmelser;

public class SedGrunnlag extends BehandlingsgrunnlagData {
    public List<Overgangsregelbestemmelser> overgangsregelbestemmelser = new ArrayList<>();
    public List<Organisasjon> norskeArbeidsgivere = new ArrayList<>();
    public String ytterligereInformasjon;
}
