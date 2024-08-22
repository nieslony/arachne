/*
 * Copyright (C) 2023 claas
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
package at.nieslony.arachne.auth;

import at.nieslony.arachne.pki.Pki;
import at.nieslony.arachne.pki.PkiException;
import at.nieslony.arachne.settings.SettingsException;
import jakarta.annotation.security.RolesAllowed;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.cms.CMSException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 *
 * @author claas
 */
@RestController
public class AuthRestController {

    @Autowired
    Pki pki;

    @Getter
    @Setter
    @AllArgsConstructor
    public class AuthResult {

        private String status;
        private String tokenValidUntil;
        private String token;
    }

    private static final Logger logger = LoggerFactory.getLogger(AuthRestController.class);

    private Date createValidUntilDate(String vu) {
        Calendar validUntilCal = Calendar.getInstance();
        logger.info("User authenticated for VPN access");
        try {
            if (vu == null) {
                validUntilCal.add(Calendar.MINUTE, 10);
            } else if (vu.endsWith("min")) {
                validUntilCal.add(
                        Calendar.MINUTE,
                        Integer.parseInt(
                                vu.substring(0, vu.length() - 3)
                        )
                );
            } else if (vu.endsWith("sec")) {
                validUntilCal.add(
                        Calendar.SECOND,
                        Integer.parseInt(
                                vu.substring(0, vu.length() - 3)
                        )
                );
            } else if (vu.endsWith("h")) {
                validUntilCal.add(
                        Calendar.HOUR,
                        Integer.parseInt(
                                vu.substring(0, vu.length() - 1)
                        )
                );
            } else {
                String msg = "Invalid time unit. Only sec, min and h are allowed";
                logger.error(msg);
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        msg
                );
            }
        } catch (NumberFormatException ex) {
            String msg = "Unvalid number format: " + vu;
            logger.error(msg);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    msg
            );
        }
        Calendar latest = Calendar.getInstance();
        latest.add(Calendar.HOUR, 24);
        if (validUntilCal.after(latest)) {
            String msg = "Requested tocked validity time too long";
            logger.error(msg);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    msg
            );
        }

        return validUntilCal.getTime();
    }

    public String verifyToken(byte[] token) {
        ByteBuffer buffer = ByteBuffer.wrap(token);

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
                logger.error("Token signature invalid");
                return null;
            }
            String jsonStr = new String(Pki.decryptData(
                    data,
                    pki.getServerKey()
            ));
            JSONObject json = new JSONObject(jsonStr);
            String username = json.getString("authenticatedUser");
            long validUntil = json.getLong("validUntil");

            Date now = new Date();
            Date validUntilDate = new Date(validUntil * 1000);
            if (validUntilDate.before(now)) {
                logger.error("Token for user %s is expired since %s"
                        .formatted(username, validUntilDate.toString())
                );
                return null;
            }
            return username;
        } catch (InvalidKeyException
                | NoSuchAlgorithmException
                | PkiException
                | SettingsException
                | SignatureException
                | CMSException
                | JSONException ex) {
            logger.error("Cannot verify token: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/api/auth")
    @RolesAllowed(value = {"USER"})
    public AuthResult auth(
            @RequestParam(required = false) String validTime,
            Principal principal
    ) {
        Date validUntil = createValidUntilDate(validTime);

        JSONObject json = new JSONObject();
        try {
            json.put("validUntil", validUntil.getTime());
            json.put("authenticatedUser", principal.getName());
        } catch (JSONException ex) {
            logger.error("Cannot create json object: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        String jsonStr = json.toString();
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

            String ret = verifyToken(Base64.getDecoder().decode(tokenAsBase64));
            logger.info("Got token user: " + ret);
        } catch (CMSException
                | CertificateEncodingException
                | IOException
                | InvalidKeyException
                | NoSuchAlgorithmException
                | SignatureException
                | PkiException
                | SettingsException ex) {
            logger.error("Cannot encrypt token: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        AuthResult authResult = new AuthResult(
                "Authenticated",
                validUntil.toString(),
                tokenAsBase64
        );
        return authResult;
    }
}
