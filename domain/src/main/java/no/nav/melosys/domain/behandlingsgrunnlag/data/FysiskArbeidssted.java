package no.nav.melosys.domain.behandlingsgrunnlag.data;

import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;

public class FysiskArbeidssted {
    public String virksomhetNavn;
    public Boolean arbeidUtlandHjemmekontor;
    public StrukturertAdresse adresse = new StrukturertAdresse();
}
