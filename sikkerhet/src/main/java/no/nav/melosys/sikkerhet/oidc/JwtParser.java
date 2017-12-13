package no.nav.melosys.sikkerhet.oidc;

import com.nimbusds.jwt.JWT;

import java.text.ParseException;

public interface JwtParser {

    JWT parse(String jwt) throws ParseException;
}