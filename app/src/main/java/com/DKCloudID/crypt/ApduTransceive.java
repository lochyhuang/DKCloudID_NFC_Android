package com.DKCloudID.crypt;

import android.nfc.tech.NfcB;
import java.io.IOException;

public class ApduTransceive {
    private static NfcB tag;

    public ApduTransceive( NfcB nfcB ) {
        tag = nfcB;
    }

    public static void setTag(NfcB theTag) {
        tag = theTag;
    }

    public static byte[] transceive(byte[] apdu) {
        try {
            if (!tag.isConnected()) {
                tag.connect();
            }
            return tag.transceive(apdu);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                tag.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return null;
        }
    }

    public static boolean restart() {
        try {
            if (tag.isConnected()) {
                tag.close();
                tag.connect();
            }
            else {
                tag.connect();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
