/*
 * Copyright (C) 2024 claas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package at.nieslony.arachne.auth.token;

import at.nieslony.arachne.pki.Pki;
import at.nieslony.arachne.pki.PkiException;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.users.UserModel;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.util.Base64;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cms.CMSException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author claas
 */
@Service
@Slf4j
public class TokenController {

    @Autowired
    Pki pki;

    public String createToken(UserModel user, Date validUntil) {
        AuthTokenContent authTokenContent = new AuthTokenContent(validUntil, user);
        String jsonStr;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            jsonStr = objectMapper.writeValueAsString(authTokenContent);
        } catch (JacksonException ex) {
            log.error("Cannot create json object: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        String tokenAsBase64;
        try {
            byte[] encToken = Pki.encryptData(
                    jsonStr.getBytes(),
                    pki.getServerCert()
            );
            byte[] signature = Pki.createSignature(encToken, pki.getServerKey());
            ByteBuffer byteBuffer = ByteBuffer.allocate(
                    4 + signature.length + 4 + encToken.length
            );
            byteBuffer.putInt(signature.length);
            byteBuffer.put(signature);
            byteBuffer.putInt(encToken.length);
            byteBuffer.put(encToken);
            tokenAsBase64 = Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (CMSException
                | CertificateEncodingException
                | IOException
                | InvalidKeyException
                | NoSuchAlgorithmException
                | SignatureException
                | PkiException
                | SettingsException ex) {
            log.error("Cannot encrypt token: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return tokenAsBase64;
    }

    public UserModel verifyToken(String tokenAsBase64) throws IllegalArgumentException {
        ByteBuffer buffer;
        try {
            buffer = ByteBuffer.wrap(Base64.getDecoder().decode(tokenAsBase64));
        } catch (IllegalArgumentException ex) {
            log.error("Cannot decode token: " + ex.getMessage());
            return null;
        }

        int signatureLen = buffer.getInt();
        byte[] signature = new byte[signatureLen];
        buffer.get(signature);

        int dataLen = buffer.getInt();
        byte[] data = new byte[dataLen];
        buffer.get(data);

        try {
            boolean ok = Pki.verifySignature(
                    data,
                    signature,
                    pki.getServerCert().getPublicKey()
            );
            if (!ok) {
                log.error("Token signature invalid");
                return null;
            }
            String jsonStr = new String(Pki.decryptData(
                    data,
                    pki.getServerKey()
            ));
            ObjectMapper objectMapper = new ObjectMapper();
            AuthTokenContent authTokenObject = objectMapper.readValue(
                    jsonStr,
                    AuthTokenContent.class
            );
            Date now = new Date();
            Date validUntilDate = authTokenObject.getValidUntil();
            UserModel user = authTokenObject.getUser();

            if (validUntilDate.before(now)) {
                log.error("Token for user %s is expired since %s"
                        .formatted(user.getUsername(), validUntilDate.toString())
                );
                return null;
            }

            return user;
        } catch (InvalidKeyException
                | NoSuchAlgorithmException
                | PkiException
                | SettingsException
                | SignatureException
                | CMSException
                | JacksonException ex) {
            log.error("Cannot verify token: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
