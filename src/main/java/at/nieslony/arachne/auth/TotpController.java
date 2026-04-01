/*
 * Copyright (C) 2026 claas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package at.nieslony.arachne.auth;

import at.nieslony.arachne.users.UserModel;
import at.nieslony.arachne.users.UserRepository;
import at.nieslony.arachne.utils.components.ShowNotification;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base32;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 *
 * @author claas
 */
@Controller
@Slf4j
public class TotpController {

    @Autowired
    UserRepository userRepository;

    private static final int TOTP_CODE_DIGITS = 6;
    private static final int TIME_STEP_MILLIS = 30 * 1000;
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    public static byte[] generateSecret(int length) {
        Random random = new Random();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    public static BufferedImage generateQRCodeImage(String barcodeText) throws Exception {
        QRCodeWriter barcodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix
                = barcodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, 200, 200);

        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    private String createToptUrl(String label, byte[] secret) {
        // ec. otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example
        //return "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example";

        return "otpauth://totp/%s?secret=%s".formatted(
                label,
                Base32.toBase32String(secret)
        );
    }

    public String generateTOTP(byte[] secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secret, HMAC_ALGORITHM);
            mac.init(keySpec);

            // Calculate the number of time steps since the Unix epoch
            long timeStep = System.currentTimeMillis() / TIME_STEP_MILLIS;

            // Convert time step to byte array
            byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array();

            // Compute the HMAC on the time step data
            byte[] hmacResult = mac.doFinal(data);

            // Extract the dynamic offset from the last byte
            int offset = hmacResult[hmacResult.length - 1] & 0xF;

            // Extract the 4 bytes starting at the offset and apply the truncation function
            int binaryCode = ((hmacResult[offset] & 0x7F) << 24)
                    | ((hmacResult[offset + 1] & 0xFF) << 16)
                    | ((hmacResult[offset + 2] & 0xFF) << 8)
                    | (hmacResult[offset + 3] & 0xFF);

            // Generate the TOTP code by reducing the binary code to a 6-digit number
            int totpCode = binaryCode % (int) Math.pow(10, TOTP_CODE_DIGITS);

            // Return the code, zero-padded to 6 digits
            return String.format("%06d", totpCode);
        } catch (InvalidKeyException | NoSuchAlgorithmException ex) {
            return "";
        }
    }

    public Component create2FAView(UserModel user) {
        return create2FAView(user, () -> {
        });
    }

    public Component create2FAView(UserModel user, Runnable onAttachAuthenticator) {
        byte[] secret = generateSecret(32);
        VerticalLayout layout = new VerticalLayout();

        H4 installHeader = new H4("Step 1: Authenticator App");
        Text installInstructions = new Text(
                """
                Install and open an autrhenticatir app (eg. Microsoft
                Authenticator or Google Authenticatoir) on your mobile phone.
                """
        );

        H4 addOtpAccountHeader = new H4("Step 2: Add OTP generator");
        Text addOtpAccountInstructions = new Text(
                """
                Add account and scan QR code.
                """
        );
        String label = "ArachneOpenVPN";
        String url = createToptUrl(
                label,
                secret
        );

        AtomicReference<String> urlRef = new AtomicReference<>(url);
        DownloadHandler dlh = (DownloadEvent de) -> {
            try (OutputStream out = de.getOutputStream()) {
                ImageIO.write(generateQRCodeImage(urlRef.toString()), "png", out);
            } catch (Exception ex) {
                log.warn("Cannot write QRCode: " + ex.getMessage());
            }
        };
        Image addOtpAccountImage = new Image(dlh, "");
        log.debug("Secret URL: " + url);

        H4 verifyOtpHeader = new H4("Step 3: Verify OTP");
        TextField verifyOtpField = new TextField("Enter OTP");
        verifyOtpField.setPattern("[0-9]{6}");
        verifyOtpField.setValueChangeMode(ValueChangeMode.EAGER);
        Button verifyOtpButton = new Button("Verity OTP");
        HorizontalLayout verifyOtpLayout = new HorizontalLayout(
                verifyOtpField,
                verifyOtpButton
        );
        verifyOtpLayout.setMargin(false);
        verifyOtpLayout.setPadding(false);
        verifyOtpLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        H4 attachAuthenticatorHeader = new H4("Step 4: Attach Authenticator");
        Button attachAuthenticatorButton = new Button("Attach Authenticator");
        attachAuthenticatorButton.setEnabled(false);
        attachAuthenticatorButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(
                installHeader, installInstructions,
                new Hr(),
                addOtpAccountHeader,
                addOtpAccountInstructions,
                addOtpAccountImage,
                new Hr(),
                verifyOtpHeader, verifyOtpLayout,
                new Hr(),
                attachAuthenticatorHeader, attachAuthenticatorButton
        );

        verifyOtpButton.addClickListener(e -> {
            if (verifyOtpField.getValue().equals(generateTOTP(secret))) {
                attachAuthenticatorButton.setEnabled(true);
            } else {
                ShowNotification.error("Error", "OTP does not match");
            }
        });

        attachAuthenticatorButton.addClickListener(e -> {
            user.setOtpSecret(secret);
            userRepository.save(user);
            onAttachAuthenticator.run();
        });

        return layout;
    }
}
