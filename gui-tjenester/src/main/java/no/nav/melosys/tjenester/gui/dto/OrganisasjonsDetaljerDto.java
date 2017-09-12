package no.nav.melosys.tjenester.gui.dto;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.melosys.tjenester.gui.dto.util.AdresseUtils;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.GeografiskAdresse;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Organisasjon;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.OrganisasjonsDetaljer;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.SammensattNavn;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.UstrukturertNavn;

// FIXME Avklare hva skal brukes fra EREG. Mangler opplysninger.
public class OrganisasjonsDetaljerDto {

    private String orgnummer;

    private String navn;

    private BostedsadresseDto forretningsadresse;

    private String postadresse;

    public OrganisasjonsDetaljerDto() {
    }

    public static OrganisasjonsDetaljerDto toDto(Organisasjon organisasjon) {
        OrganisasjonsDetaljerDto dto = new OrganisasjonsDetaljerDto();

        dto.setOrgnummer(organisasjon.getOrgnummer());

        SammensattNavn sammensattNavn = organisasjon.getNavn();
        String navn = null;
        if (sammensattNavn instanceof UstrukturertNavn) {
            UstrukturertNavn ustrukturertNavn = (UstrukturertNavn) sammensattNavn;
            List<String> navnelinje = ustrukturertNavn.getNavnelinje();
            navn = navnelinje.stream().filter(Objects::nonNull).filter(s -> !s.isEmpty()).collect(Collectors.joining(" "));
        }
        dto.setNavn(navn);

        OrganisasjonsDetaljer organisasjonDetaljer = organisasjon.getOrganisasjonDetaljer();

        List<GeografiskAdresse> forretningsadresser = organisasjonDetaljer.getForretningsadresse();
        for (GeografiskAdresse adresse : forretningsadresser) {
            // TODO hvis det finnes flere gyldige adresser?
            if (AdresseUtils.erGyldig(adresse)) {
                dto.setForretningsadresse(BostedsadresseDto.lagForretningsadresse(adresse));
                break;
            }
        }

        List<GeografiskAdresse> postadresser = organisasjonDetaljer.getPostadresse();
        for (GeografiskAdresse adresse : postadresser) {
            if (AdresseUtils.erGyldig(adresse)) {
                dto.setPostadresse(AdresseUtils.lagPostadresse(adresse));
                break;
            }
        }

        return dto;
    }

    public String getOrgnummer() {
        return orgnummer;
    }

    public void setOrgnummer(String orgnummer) {
        this.orgnummer = orgnummer;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
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

}