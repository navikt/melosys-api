package no.nav.melosys.tjenester.gui.dto.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Gateadresse;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.GeografiskAdresse;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.NoekkelVerdiAdresse;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.SemistrukturertAdresse;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.StedsadresseNorge;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.StrukturertAdresse;

public class AdresseUtils {

    public static final String ADRESSELINJE1 = "adresselinje1";
    public static final String ADRESSELINJE2 = "adresselinje2";
    public static final String ADRESSELINJE3 = "adresselinje3";
    public static final String POSTNR = "postnr";
    public static final String POSTSTED = "poststed";
    public static final String KOMMUNENR = "kommunenr";

    public static boolean erGyldig(GeografiskAdresse adresse) {
        if (adresse == null) {
            return false;
        }

        LocalDate fom = DtoUtils.tilLocalDate(adresse.getFomGyldighetsperiode());
        LocalDate tom = DtoUtils.tilLocalDate(adresse.getTomGyldighetsperiode());

        LocalDate nå = LocalDate.now();

        boolean etterFom = true;
        if (fom != null) {
            etterFom = nå.isAfter(fom);
        }

        boolean førTom = true;
        if (tom != null) {
            førTom = nå.isBefore(tom);
        }

        return etterFom && førTom;
    }

    public static String lagPostadresse(GeografiskAdresse geografiskAdresse) {

        if (geografiskAdresse instanceof SemistrukturertAdresse) {
            // Endre NoekkelVerdiAdresser fra en List til en Map slik at det er lettere å jobbe med
            Map<String, String> adresseMap = new HashMap<>();
            List<NoekkelVerdiAdresse> adresseledd = ((SemistrukturertAdresse) geografiskAdresse).getAdresseledd();
            adresseledd.forEach(n -> adresseMap.put(n.getNoekkel().getKodeRef(), n.getVerdi()));

            // Lage en liste over nøklene som brukes i adressen
            List<String> nøkkler = new ArrayList<>();
            nøkkler.add(ADRESSELINJE1);
            nøkkler.add(ADRESSELINJE2);
            nøkkler.add(ADRESSELINJE3);
            nøkkler.add(POSTNR);
            nøkkler.add(POSTSTED);

            String s = nøkkler.stream().map(x -> adresseMap.get(x)).filter(Objects::nonNull).collect(Collectors.joining(" "));

            if (geografiskAdresse.getLandkode() != null) {
                s = s.concat(" " + geografiskAdresse.getLandkode().getKodeRef());
            }

            return s;

        } else if (geografiskAdresse instanceof StrukturertAdresse) {
            StringBuilder sb = new StringBuilder();

            if (geografiskAdresse instanceof Gateadresse) {
                Gateadresse gateadresse = (Gateadresse) geografiskAdresse;

                if (gateadresse.getGatenavn() != null) {
                    sb.append(gateadresse.getGatenavn() + " ");
                }
                if (gateadresse.getGatenummer() != null) {
                    sb.append(gateadresse.getGatenummer() + " ");
                }
                if (gateadresse.getHusnummer() != null) {
                    sb.append(gateadresse.getHusnummer() + " ");
                }
                if (gateadresse.getHusbokstav() != null) {
                    sb.append(gateadresse.getHusbokstav() + " ");
                }

            }

            if (geografiskAdresse instanceof StedsadresseNorge) {
                StedsadresseNorge adresse = (StedsadresseNorge) geografiskAdresse;

                sb.append(adresse.getPoststed() + " "); // TODO Kodeverk
            }

            if (geografiskAdresse.getLandkode() != null) {
                sb.append(geografiskAdresse.getLandkode().getKodeRef()); // TODO Kodeverk
            }

            return sb.toString();
        }

        throw new IllegalArgumentException("geografiskAdresse må være en SemistrukturertAdresse eller en StrukturertAdresse");
    }
}
