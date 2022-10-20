package no.nav.melosys.saksflyt;

import java.util.HashSet;

import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.util.Land_ISO2;

public final class SaksflytTestUtils {
    private SaksflytTestUtils() {}

    public static Vilkaarsresultat lagVilkaarsresultat(Vilkaar vilkaar, boolean oppfylt, Kodeverk... vilkårbegrunnelser) {
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setOppfylt(oppfylt);
        vilkaarsresultat.setVilkaar(vilkaar);
        vilkaarsresultat.setBegrunnelser(new HashSet<>());
        for (Kodeverk begrunnelseKode : vilkårbegrunnelser) {
            VilkaarBegrunnelse begrunnelse = new VilkaarBegrunnelse();
            begrunnelse.setKode(begrunnelseKode.getKode());
            vilkaarsresultat.getBegrunnelser().add(begrunnelse);
        }
        return vilkaarsresultat;
    }

    public static UtenlandskMyndighet lagUtenlandskMyndighet() {
        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.institusjonskode = "123456";
        utenlandskMyndighet.landkode = Land_ISO2.SE;
        utenlandskMyndighet.navn = "Svenska myndighetan";
        utenlandskMyndighet.gateadresse = "Svenskegatan 38";
        utenlandskMyndighet.poststed = "Svenska stan";
        utenlandskMyndighet.postnummer = "8080";
        return utenlandskMyndighet;
    }
}
