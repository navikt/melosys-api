package no.nav.melosys.domain.behandlingsgrunnlag.soeknad;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

//Ekstra opplysning for bruker
public class OpplysningerOmBrukeren {

    public List<UtenlandskIdent> utenlandskIdent = new ArrayList<>();
    public List<String> medfolgendeFamilie = new ArrayList<>();
    public List<String> medfolgendeBarn = new ArrayList<>();
    public String medfolgendeAndre;

    public Stream<String> hentAllePersonnummer() {
        return Stream.concat(
            medfolgendeFamilie.stream(),
            Stream.concat(
                medfolgendeBarn.stream(),
                Stream.of(medfolgendeAndre)));
    }
}
