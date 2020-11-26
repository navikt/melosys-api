package no.nav.melosys.domain.behandlingsgrunnlag.soeknad;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

//Ekstra opplysning for bruker
public class OpplysningerOmBrukeren {
    public List<UtenlandskIdent> utenlandskIdent = new ArrayList<>();
    public List<MedfolgendeFamilie> medfolgendeFamilie = new ArrayList<>();

    public List<String> hentFnrMedfølgendeBarn() {
        return medfolgendeFamilie.stream()
            .filter(MedfolgendeFamilie::erBarn)
            .map(MedfolgendeFamilie::getFnr)
            .collect(Collectors.toList());
    }
}
