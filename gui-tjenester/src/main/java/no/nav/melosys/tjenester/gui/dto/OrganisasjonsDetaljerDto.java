package no.nav.melosys.tjenester.gui.dto;

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
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.OrganisasjonsDetaljer;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.SemistrukturertAdresse;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.StedsadresseNorge;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.StrukturertAdresse;

// FIXME Avklare hva skal brukes fra EREG. Mangler opplysninger.
public class OrganisasjonsDetaljerDto {

    private static final String ADRESSELINJE1 = "adresselinje1";
    private static final String ADRESSELINJE2 = "adresselinje2";
    private static final String ADRESSELINJE3 = "adresselinje3";
    private static final String POSTNR = "postnr";
    private static final String POSTSTED = "poststed";
    private static final String KOMMUNENR = "kommunenr";


    private BostedsadresseDto forretningsadresse;

    private String postadresse;

    public OrganisasjonsDetaljerDto() {
    }

    public static OrganisasjonsDetaljerDto toDto(OrganisasjonsDetaljer organisasjonDetaljer) {
        OrganisasjonsDetaljerDto dto = new OrganisasjonsDetaljerDto();

        List<GeografiskAdresse> forretningsadresser = organisasjonDetaljer.getForretningsadresse();
        for (GeografiskAdresse adresse : forretningsadresser) {
            // TODO hvis det finnes flere gyldige adresser?
            if (erGyldig(adresse)) {
                dto.setForretningsadresse(lagForretningsadresse(adresse));
                break;
            }
        }

        List<GeografiskAdresse> postadresser = organisasjonDetaljer.getPostadresse();
        for (GeografiskAdresse adresse : postadresser) {
            if (erGyldig(adresse)) {
                dto.setPostadresse(lagPostadresse(adresse));
                break;
            }
        }

        return dto;
    }

    public BostedsadresseDto getForretningsadresse() {
        return forretningsadresse;
    }

    public void setForretningsadresse(BostedsadresseDto forretningsadresse) {
        this.forretningsadresse = forretningsadresse;
    }

    public String getPostadresse() {
        return postadresse;
    }

    public void setPostadresse(String postadresse) {
        this.postadresse = postadresse;
    }

    private static boolean erGyldig(GeografiskAdresse adresse) {
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

    private static BostedsadresseDto lagForretningsadresse(GeografiskAdresse adresse) {

        if (adresse instanceof SemistrukturertAdresse) {
            // Endre NoekkelVerdiAdresser fra en List til en Map slik at det er lettere å jobbe med
            Map<String, String> adresseMap = new HashMap<>();
            List<NoekkelVerdiAdresse> adresseledd = ((SemistrukturertAdresse) adresse).getAdresseledd();
            adresseledd.forEach(n -> adresseMap.put(n.getNoekkel().getKodeRef(), n.getVerdi()));

            BostedsadresseDto dto = new BostedsadresseDto();
            GateadresseDto gateadresseDto = new GateadresseDto();

            StringBuilder adresseBuilder = new StringBuilder();

            String linje1 = adresseMap.get(ADRESSELINJE1);
            if ((linje1 != null) && !(linje1.isEmpty())) {
                adresseBuilder.append(linje1 + " ");
            }
            String linje2 = adresseMap.get(ADRESSELINJE2);
            if ((linje2 != null) && !(linje2.isEmpty())) {
                adresseBuilder.append(linje2 + " ");
            }
            String linje3 = adresseMap.get(ADRESSELINJE3);
            if ((linje3 != null) && !(linje3.isEmpty())) {
                adresseBuilder.append(linje3 + " ");
            }

            gateadresseDto.setGatenavn(adresseBuilder.toString());

            dto.setGateadresse(gateadresseDto);
            dto.setPostnr(adresseMap.get(POSTNR));
            dto.setPoststed(adresseMap.get(POSTSTED));

            if (adresse.getLandkode() != null) {
                dto.setLand(adresse.getLandkode().getKodeRef());
            }

            return dto;

        } else if (adresse instanceof StrukturertAdresse) {
            BostedsadresseDto dto = new BostedsadresseDto();

            if (adresse instanceof Gateadresse) {
                Gateadresse gateadresse = (Gateadresse) adresse;
                GateadresseDto gateadresseDto = new GateadresseDto();

                gateadresseDto.setGatenavn(gateadresse.getGatenavn());
                gateadresseDto.setGatenummer(gateadresse.getGatenummer());
                gateadresseDto.setHusnummer(gateadresse.getHusnummer());
                gateadresseDto.setHusbokstav(gateadresse.getHusbokstav());

                dto.setGateadresse(gateadresseDto);
            }

            if (adresse instanceof StedsadresseNorge) {
                StedsadresseNorge adresseNorge = (StedsadresseNorge) adresse;

                dto.setPostnr(adresseNorge.getPoststed().getValue());
                //dto.setPoststed(); TODO Kodeverk
            }

            if (adresse.getLandkode() != null) {
                dto.setLand(adresse.getLandkode().getKodeRef());
            }

            return  dto;
        }

        throw new IllegalArgumentException("geografiskAdresse må være en SemistrukturertAdresse eller en StrukturertAdresse");
    }

    private static String lagPostadresse(GeografiskAdresse geografiskAdresse) {

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