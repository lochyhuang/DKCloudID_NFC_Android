package com.DKCloudID.crypt;

import android.nfc.tech.NfcB;
import java.io.IOException;
import com.huang.lochy.StringTool;

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
            System.out.println("APDU >> " + StringTool.byteHexToSting(apdu));
            byte[] rcv_bytes = tag.transceive(apdu);
            System.out.println("APDU << " + StringTool.byteHexToSting(rcv_bytes));
            return rcv_bytes;
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
