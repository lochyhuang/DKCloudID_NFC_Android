package com.huang.lochy;

/**
 * Created by Administrator on 2016/11/19.
 * 金融卡
 */
public class FinancialCard {
    //选择借记卡交易文件指令
    public static byte[] getSelectDebitCardPayFileCmdBytes() {
        return new byte[]{0x00, (byte)0xA4, 0x04, 0x00, 0x08, (byte)0xA0, 0x00, 0x00, 0x03, 0x33, 0x01, 0x01, 0x01};
    }

    //选择储蓄卡交易文件指令
    public static byte[] getSelectDepositCardPayFileCmdBytes() {
        return new byte[]{0x00, (byte)0xA4, 0x04, 0x00, 0x08, (byte)0xA0, 0x00, 0x00, 0x03, 0x33, 0x01, 0x01, 0x02};
    }

    //获取银行卡交易记录指令
    //n：1-10，表示第几条交易记录
    public static byte[] getTradingRecordCmdBytes(byte n) {
        return new byte[] {(byte) 0x00, (byte) 0xb2, n, (byte) 0x5c, 0x00};
    }

    //提取交易记录信息
    public static String extractTradingRecordFromeRturnBytes(byte[] returnBytes) {
        if ((returnBytes.length > 42) &&  (returnBytes[returnBytes.length - 2] == (byte) 0x90)) {
            String optStr = "交易";
//            if (returnBytes[42] == 1) {
//                optStr = "出账";
//            } else if (returnBytes[42] == 0x34) {
//                optStr = "查询";
//            } else {
//                optStr = "入帐";
//            }

            StringBuffer displayStrBuffer = new StringBuffer();

            String moneyStr = String.format("%02x%02x%02x%02x%02x.%02x 元",
                    returnBytes[6],
                    returnBytes[7],
                    returnBytes[8],
                    returnBytes[9],
                    returnBytes[10],
                    returnBytes[11]);
            moneyStr = moneyStr.replaceAll("^(0+)", "");
            displayStrBuffer.append("\r\n" + String.format("20%02x.%02x.%02x %02x:%02x:%02x %s %s",
                    returnBytes[0],
                    returnBytes[1],
                    returnBytes[2],
                    returnBytes[3],
                    returnBytes[4],
                    returnBytes[5],
                    optStr,
                    moneyStr));

            return displayStrBuffer.toString();
        }

        return "\r\n无此记录";
    }

//    public static String extractCardNumberFromeRturnBytes (byte[] returnBytes) {
//        StringBuilder stringBuilder = new StringBuilder();
//        for(byte byteChar : returnBytes)
//            stringBuilder.append(String.format("%02X", byteChar));
//        int index = stringBuilder.indexOf("5A");
//        if ((index != -1)
//                && (returnBytes.length > index / 2 + 1)
//                && (stringBuilder.length() > (((returnBytes[index / 2 + 1] & 0x00ff) + 2) * 2 + index - 1))) {
//            String str = String.format("%s", stringBuilder.substring(index + 4, ((returnBytes[index / 2 + 1] & 0x00ff) + 2) * 2 + index));
//            return str.replace("F","");
//        }
//        return null;
//    }
}
