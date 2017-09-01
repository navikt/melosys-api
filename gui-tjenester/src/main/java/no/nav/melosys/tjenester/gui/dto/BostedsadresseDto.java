package no.nav.melosys.tjenester.gui.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nav.melosys.tjenester.gui.dto.util.AdresseUtils;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.GeografiskAdresse;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.NoekkelVerdiAdresse;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.SemistrukturertAdresse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bostedsadresse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.StedsadresseNorge;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.StrukturertAdresse;

public class BostedsadresseDto {

    private GateadresseDto gateadresse;

    private String postnr;

    private String poststed;

    private String land;

    public static BostedsadresseDto tilDto(Bostedsadresse bostedsadresse) {
        BostedsadresseDto dto = new BostedsadresseDto();

        StrukturertAdresse strukturertAdresse =
                bostedsadresse.getStrukturertAdresse();

        if (strukturertAdresse instanceof StedsadresseNorge) {
            StedsadresseNorge adresseNorge = ((StedsadresseNorge) strukturertAdresse);
            String postNr = adresseNorge.getPoststed().getValue();
            dto.setPostnr(postNr);

            //TODO Kodeverk
            String postSted = "Kodeverk for " + postNr;
            dto.setPoststed(postSted);
        }

        if (strukturertAdresse instanceof Gateadresse) {
            Gateadresse gateadresse = (Gateadresse) strukturertAdresse;
            GateadresseDto gateDto = GateadresseDto.tilDto(gateadresse);
            dto.setGateadresse(gateDto);
        }

        return dto;
    }

    public static BostedsadresseDto lagForretningsadresse(GeografiskAdresse adresse) {

        if (adresse instanceof SemistrukturertAdresse) {
            // Endre NoekkelVerdiAdresser fra en List til en Map slik at det er lettere å jobbe med
            Map<String, String> adresseMap = new HashMap<>();
            List<NoekkelVerdiAdresse> adresseledd = ((SemistrukturertAdresse) adresse).getAdresseledd();
            adresseledd.forEach(n -> adresseMap.put(n.getNoekkel().getKodeRef(), n.getVerdi()));

            BostedsadresseDto dto = new BostedsadresseDto();
            GateadresseDto gateadresseDto = new GateadresseDto();

            StringBuilder adresseBuilder = new StringBuilder();

            String linje1 = adresseMap.get(AdresseUtils.ADRESSELINJE1);
            if ((linje1 != null) && !(linje1.isEmpty())) {
                adresseBuilder.append(linje1 + " ");
            }
            String linje2 = adresseMap.get(AdresseUtils.ADRESSELINJE2);
            if ((linje2 != null) && !(linje2.isEmpty())) {
                adresseBuilder.append(linje2 + " ");
            }
            String linje3 = adresseMap.get(AdresseUtils.ADRESSELINJE3);
            if ((linje3 != null) && !(linje3.isEmpty())) {
                adresseBuilder.append(linje3 + " ");
            }

            gateadresseDto.setGatenavn(adresseBuilder.toString());

            dto.setGateadresse(gateadresseDto);
            dto.setPostnr(adresseMap.get(AdresseUtils.POSTNR));
            dto.setPoststed(adresseMap.get(AdresseUtils.POSTSTED));

            if (adresse.getLandkode() != null) {
                dto.setLand(adresse.getLandkode().getKodeRef());
            }

            return dto;

        } else if (adresse instanceof no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.StrukturertAdresse) {
            BostedsadresseDto dto = new BostedsadresseDto();

            if (adresse instanceof no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Gateadresse) {
                no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Gateadresse gateadresse = (no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Gateadresse) adresse;
                GateadresseDto gateadresseDto = new GateadresseDto();

                gateadresseDto.setGatenavn(gateadresse.getGatenavn());
                gateadresseDto.setGatenummer(gateadresse.getGatenummer());
                gateadresseDto.setHusnummer(gateadresse.getHusnummer());
                gateadresseDto.setHusbokstav(gateadresse.getHusbokstav());

                dto.setGateadresse(gateadresseDto);
            }

            if (adresse instanceof no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.StedsadresseNorge) {
                no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.StedsadresseNorge adresseNorge = (no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.StedsadresseNorge) adresse;

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

    public GateadresseDto getGateadresse() {
        return gateadresse;
    }

    public void setGateadresse(GateadresseDto gateadresse) {
        this.gateadresse = gateadresse;
    }

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }

    public String getPostnr() {
        return postnr;
    }

    public void setPostnr(String postnr) {
        this.postnr = postnr;
    }

    public String getPoststed() {
        return poststed;
    }

    public void setPoststed(String poststed) {
        this.poststed = poststed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BostedsadresseDto)) return false;

        BostedsadresseDto that = (BostedsadresseDto) o;

        if (gateadresse != null ? !gateadresse.equals(that.gateadresse) : that.gateadresse != null) return false;
        if (postnr != null ? !postnr.equals(that.postnr) : that.postnr != null) return false;
        if (poststed != null ? !poststed.equals(that.poststed) : that.poststed != null) return false;
        return land != null ? land.equals(that.land) : that.land == null;
    }

    @Override
    public int hashCode() {
        int result = gateadresse != null ? gateadresse.hashCode() : 0;
        result = 31 * result + (postnr != null ? postnr.hashCode() : 0);
        result = 31 * result + (poststed != null ? poststed.hashCode() : 0);
        result = 31 * result + (land != null ? land.hashCode() : 0);
        return result;
    }
}
