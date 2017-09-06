package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.tjenester.gui.dto.ArbeidsforholdDto;
import no.nav.melosys.tjenester.gui.dto.OrganisajonDto;
import no.nav.melosys.tjenester.gui.dto.OrganisasjonsDetaljerDto;
import no.nav.melosys.tjenester.gui.dto.PersonDto;
import no.nav.melosys.tjenester.gui.dto.view.ArbeidsforholdView;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonOrganisasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Organisasjon;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.SammensattNavn;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.UstrukturertNavn;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;

@Api(tags = {"arbeidsforhold"})
@Path("/arbeidsforhold")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class ArbeidsforholdRestTjeneste extends RestTjeneste {

    private TpsFasade tps;

    private AaregFasade aareg;

    private EregFasade ereg;

    @Autowired
    public ArbeidsforholdRestTjeneste(TpsFasade tps, AaregFasade aareg, EregFasade ereg) {
        this.tps = tps;
        this.aareg = aareg;
        this.ereg = ereg;
    }

    @GET
    @Path("{ident}")
    @ApiOperation(value = "Søk etter personopplysninger og arbeidsforhold på fødselsnummer")
    public Response hentArbeidsforhold(@PathParam("ident") String ident) {
        ArbeidsforholdView view = new ArbeidsforholdView();

        try {
            // TODO Lagre?
            HentPersonResponse hentPersonResponse = tps.hentPersonMedAdresse(ident);
            view.setPerson(PersonDto.tilDto(hentPersonResponse.getPerson()));

        } catch (HentPersonPersonIkkeFunnet hentPersonPersonIkkeFunnet) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (HentPersonSikkerhetsbegrensning hentPersonSikkerhetsbegrensning) {
            //  Det foreligger sikkerhetsbegrensinger på søket. Saksbehandleren har begrenset innsyn mot TPS databasen.
            //  For eksempel kan dette være restriksjoner knyttet til kode 6, kode7 eller Egen Ansatt
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        List<ArbeidsforholdDto> arbeidsforhold = new ArrayList<>();
        try {
            // TODO Vi har (foreløpig) sagt at vi kun skal hente arbeidsforhold rapportert på nytt regelverk.
            boolean innrapportertEtterAOrdningen = true;
            List<Arbeidsforhold> liste = aareg.finnArbeidsforholdPrArbeidstaker(ident, AaregFasade.REGELVERK_A_ORDNINGEN);

            liste.forEach(x -> arbeidsforhold.add(ArbeidsforholdDto.toDto(x)));
            arbeidsforhold.forEach(x -> x.setArbeidsforholdInnrapportertEtterAOrdningen(innrapportertEtterAOrdningen));
            view.setArbeidsforhold(arbeidsforhold);

        } catch (FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning finnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (FinnArbeidsforholdPrArbeidstakerUgyldigInput finnArbeidsforholdPrArbeidstakerUgyldigInput) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        // Henter opplysninger om arbeidsgiveren/organisasjoner fra Enhetsregisteret
        for (ArbeidsforholdDto a : arbeidsforhold) {
            try {
                hentOrganisasjoner(a);
            } catch (HentOrganisasjonUgyldigInput hentOrganisasjonUgyldigInput) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            } catch (HentOrganisasjonOrganisasjonIkkeFunnet hentOrganisasjonOrganisasjonIkkeFunnet) {
                // TODO Avklare hva skjer når en organisasjon ikke finnes
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        }

        return Response.ok(view).build();
    }

    private void hentOrganisasjoner(ArbeidsforholdDto arbeidsforhold) throws HentOrganisasjonUgyldigInput, HentOrganisasjonOrganisasjonIkkeFunnet {
        hentOrganisasjon(arbeidsforhold.getArbeidsgiver());
        hentOrganisasjon(arbeidsforhold.getOpplysningspliktig());
    }

    private void hentOrganisasjon(OrganisajonDto organisajon) throws HentOrganisasjonUgyldigInput, HentOrganisasjonOrganisasjonIkkeFunnet {
        if (organisajon == null || organisajon.getOrgnummer() == null) {
            return;
        }

        Organisasjon org = ereg.hentOrganisasjon(organisajon.getOrgnummer());

        SammensattNavn sammensattNavn = org.getNavn();
        String navn = null;
        if (sammensattNavn instanceof UstrukturertNavn) {
            UstrukturertNavn ustrukturertNavn = (UstrukturertNavn) sammensattNavn;
            List<String> navnelinje = ustrukturertNavn.getNavnelinje();
            navn = navnelinje.stream().filter(Objects::nonNull).filter(s -> !s.isEmpty()).collect(Collectors.joining(" "));
        }
        organisajon.setNavn(navn);

        organisajon.setOrganisasjonDetaljer(OrganisasjonsDetaljerDto.toDto(org.getOrganisasjonDetaljer()));

    }

}
