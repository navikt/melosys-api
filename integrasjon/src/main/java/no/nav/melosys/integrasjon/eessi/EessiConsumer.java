package no.nav.melosys.integrasjon.eessi;

import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.eessi.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto;

public interface EessiConsumer {

    OpprettSedDto opprettBucOgSed(SedDataDto sedDataDto, Collection<Vedlegg> vedlegg, BucType bucType, boolean forsøkSend) throws MelosysException;

    void sendSedPåEksisterendeBuc(SedDataDto sedDataDto, String rinaSaksnummer, SedType sedType) throws MelosysException;

    List<BucInformasjon> hentTilknyttedeBucer(long gsakSaksnummer, List<String> statuser) throws MelosysException;

    List<Institusjon> hentMottakerinstitusjoner(String bucType, String landkode) throws MelosysException;

    MelosysEessiMelding hentMelosysEessiMeldingFraJournalpostID(String journalpostID) throws MelosysException;

    void lagreSaksrelasjon(SaksrelasjonDto saksrelasjonDto) throws MelosysException;

    List<SaksrelasjonDto> hentSakForRinasaksnummer(String rinaSaksnummer) throws MelosysException;

    byte[] genererSedPdf(SedDataDto sedDataDto, SedType sedType) throws MelosysException;

    SedGrunnlagDto hentSedGrunnlag(String rinaSaksnummer, String rinaDokumentID) throws MelosysException;
}
