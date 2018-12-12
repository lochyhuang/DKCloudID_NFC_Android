package com.huang.lochy;

import com.dk.bleNfc.DeviceManager.DeviceManager;
import com.dk.bleNfc.card.CpuCard;

/**
 * Created by Administrator on 2016/5/31.
 */
public class SZTCard extends CpuCard {
    private onReceiveBalanceListener mOnReceiveBalanceListener;
    private onReceiveTradeListener mOnReceiveTradeListener;

    public SZTCard(DeviceManager deviceManager, byte[] uid, byte[] atr) {
        super(deviceManager, uid, atr);
    }

    //获取余额回调接口
    public interface onReceiveBalanceListener{
        void onReceiveBalance(boolean isSuc, long balance);
    }

    //获取交易记录回调接口
    public interface onReceiveTradeListener{
        void onReceiveTrade(boolean isSuc, String tradeString);
    }

    //选择SZT余额/交易记录文件
    public static byte[] getSelectMainFileCmdByte() {
        return new byte[]{0x00, (byte)0xa4, 0x04, 0x00, 0x07, 0x50, 0x41, 0x59, 0x2e, 0x53, 0x5a, 0x54, 0x00};
        //return new byte[]{0x00, (byte)0xa4, 0x04, 0x00, 0x08, 0x50, 0x41, 0x59, 0x2e, 0x54, 0x49, 0x43, 0x4c, 0x00};
    }

    //获取余额APDU指令
    public static byte[] getBalanceCmdByte() {
        return new byte[]{(byte)0x80, (byte)0x5c, 0x00, 0x02, 0x04};
    }

    //获取交易记录APDU指令
    public static byte[] getTradeCmdByte(byte n) {
        return new byte[]{(byte)0x00, (byte)0xB2, n, (byte)0xC4, 0x00};
    }

    public static String getBalance(byte[] apduData) {
        if ((apduData != null) && (apduData.length == 6) && (apduData[4] == (byte)0x90)&& (apduData[5] == (byte)0x00)) {
            long balance = ((long) (apduData[1] & 0x00ff) << 16)
                    | ((long) (apduData[2] & 0x00ff) << 8)
                    | ((long) (apduData[3] & 0x00ff));
            return ( (balance/100) + "." + (balance % 100));
        }
        return null;
    }

    public static String getTrade(byte[] bytes) {
        if ( (bytes != null) && (bytes.length == 25) && (bytes[24] == 0x00) && (bytes[23] == (byte) 0x90)) {
            StringBuffer displayStrBuffer = new StringBuffer();

            long money = ((long) (bytes[5] & 0x00ff) << 24)
                    | ((long) (bytes[6] & 0x00ff) << 16)
                    | ((long) (bytes[7] & 0x00ff) << 8)
                    | ((long) (bytes[8] & 0x00ff));
            String optStr = new String();
            if ((bytes[9] == 6) || (bytes[9] == 9)) {
                optStr = "扣款";
            } else {
                optStr = "充值";
            }

            displayStrBuffer.append(String.format("%02x%02x.%02x.%02x %02x:%02x:%02x %s %d.%d 元",
                    bytes[16],
                    bytes[17],
                    bytes[18],
                    bytes[19],
                    bytes[20],
                    bytes[21],
                    bytes[22],
                    optStr,
                    money / 100,
                    money % 100));

            return displayStrBuffer.toString();
        }
        return null;
    }

    public void requestSelectTradeFile() {

    }

    public void requestBalance(onReceiveBalanceListener listener) {
        mOnReceiveBalanceListener = listener;
        apduExchange(getBalanceCmdByte(), new onReceiveApduExchangeListener() {
            @Override
            public void onReceiveApduExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
                if (getBalance(bytApduRtnData) == null) {
                    if (mOnReceiveBalanceListener != null) {
                        mOnReceiveBalanceListener.onReceiveBalance(false, 0);
                    }
                    return;
                }
                long balance = ((long) (bytApduRtnData[1] & 0x00ff) << 16)
                        | ((long) (bytApduRtnData[2] & 0x00ff) << 8)
                        | ((long) (bytApduRtnData[3] & 0x00ff));
                if (mOnReceiveBalanceListener != null) {
                    mOnReceiveBalanceListener.onReceiveBalance(true, balance);
                }
            }
        });
    }

    //获取交易记录
    //n：1~10
    public void requestTrade(byte n, onReceiveTradeListener listener) {
        mOnReceiveTradeListener = listener;
        apduExchange(getTradeCmdByte(n), new onReceiveApduExchangeListener() {
            @Override
            public void onReceiveApduExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
                if (mOnReceiveTradeListener != null) {
                    mOnReceiveTradeListener.onReceiveTrade(isCmdRunSuc, getTrade(bytApduRtnData));
                }
            }
        });
    }

    public void requestSZTMsg() {

    }

    //                        //选择深圳通主文件
//                        card.apduExchange(SZTCard.getSelectMainFileCmdByte(), new CpuCard.onReceiveApduExchangeListener() {
//                            @Override
//                            public void onReceiveApduExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
//                                if (!isCmdRunSuc || (bytApduRtnData.length <= 2) ) {
//                                    System.out.println("不是深圳通卡，当成银行卡处理！");
//                                    //选择储蓄卡交易文件
//                                    card.apduExchange(FinancialCard.getSelectDepositCardPayFileCmdBytes(), new CpuCard.onReceiveApduExchangeListener() {
//                                        @Override
//                                        public void onReceiveApduExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
//                                            if ( !isCmdRunSuc || (bytApduRtnData.length <= 2) ) {
//                                                System.out.println("不是储蓄卡，当成借记卡处理！");
//                                                //选择借记卡交易文件
//                                                card.apduExchange(FinancialCard.getSelectDebitCardPayFileCmdBytes(), new CpuCard.onReceiveApduExchangeListener() {
//                                                    @Override
//                                                    public void onReceiveApduExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
//                                                        if ( !isCmdRunSuc || (bytApduRtnData.length <= 2) ) {
//                                                            msgBuffer.append("未知CPU卡！");
//                                                            handler.sendEmptyMessage(0);
//                                                            card.close(null);
//                                                            return;
//                                                        }
//                                                        else {
//                                                            //发送获取银行卡卡号指令
//                                                            card.apduExchange(FinancialCard.getCardNumberCmdBytes(), new CpuCard.onReceiveApduExchangeListener() {
//                                                                @Override
//                                                                public void onReceiveApduExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
//                                                                    if (!isCmdRunSuc || (bytApduRtnData.length <= 2) ) {
//                                                                        msgBuffer.append("未知CPU卡！");
//                                                                        handler.sendEmptyMessage(0);
//                                                                        card.close(null);
//                                                                        return;
//                                                                    }
//
//                                                                    //提取银行卡卡号
//                                                                    String cardNumberString = FinancialCard.extractCardNumberFromeRturnBytes(bytApduRtnData);
//                                                                    if (cardNumberString == null) {
//                                                                        msgBuffer.append("未知CPU卡！");
//                                                                        handler.sendEmptyMessage(0);
//                                                                        card.close(null);
//                                                                        return;
//                                                                    }
//
//                                                                    msgBuffer.append("储蓄卡卡号：" + cardNumberString);
//                                                                    handler.sendEmptyMessage(0);
//
//                                                                    //读交易记录
//                                                                    System.out.println("发送APDU指令-读10条交易记录");
//                                                                    Handler readYHCHandler = new Handler(MainActivity.this.getMainLooper()) {
//                                                                        @Override
//                                                                        public void handleMessage(Message msg) {
//                                                                            final Handler theHandler = msg.getTarget();
//                                                                            if (msg.what <= 10) {  //循环读10条交易记录
//                                                                                final int index = msg.what;
//                                                                                card.apduExchange(FinancialCard.getTradingRecordCmdBytes((byte) msg.what), new CpuCard.onReceiveApduExchangeListener() {
//                                                                                    @Override
//                                                                                    public void onReceiveApduExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
//                                                                                        if (!isCmdRunSuc) {
//                                                                                            card.close(null);
//                                                                                            return;
//                                                                                        }
//                                                                                        msgBuffer.append(FinancialCard.extractTradingRecordFromeRturnBytes(bytApduRtnData));
//                                                                                        handler.sendEmptyMessage(0);
//                                                                                        theHandler.sendEmptyMessage(index + 1);
//                                                                                    }
//                                                                                });
//                                                                            } else if (msg.what == 11) { //读完10条交易记录，关闭天线
//                                                                                card.close(null);
//                                                                            }
//                                                                        }
//                                                                    };
//                                                                    readYHCHandler.sendEmptyMessage(1);
//                                                                }
//                                                            });
//                                                        }
//                                                    }
//                                                });
//                                            }
//                                            else {
//                                                //发送获取银行卡卡号指令
//                                                card.apduExchange(FinancialCard.getCardNumberCmdBytes(), new CpuCard.onReceiveApduExchangeListener() {
//                                                    @Override
//                                                    public void onReceiveApduExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
//                                                        if (!isCmdRunSuc || (bytApduRtnData.length <= 2) ) {
//                                                            msgBuffer.append("未知CPU卡！");
//                                                            handler.sendEmptyMessage(0);
//                                                            card.close(null);
//                                                            return;
//                                                        }
//
//                                                        //提取银行卡卡号
//                                                        String cardNumberString = FinancialCard.extractCardNumberFromeRturnBytes(bytApduRtnData);
//                                                        if (cardNumberString == null) {
//                                                            msgBuffer.append("未知CPU卡！");
//                                                            handler.sendEmptyMessage(0);
//                                                            card.close(null);
//                                                            return;
//                                                        }
//
//                                                        msgBuffer.append("信用卡卡号：" + cardNumberString);
//                                                        handler.sendEmptyMessage(0);
//
//                                                        //读交易记录
//                                                        System.out.println("发送APDU指令-读10条交易记录");
//                                                        Handler readYHCHandler = new Handler(MainActivity.this.getMainLooper()) {
//                                                            @Override
//                                                            public void handleMessage(Message msg) {
//                                                                final Handler theHandler = msg.getTarget();
//                                                                if (msg.what <= 10) {  //循环读10条交易记录
//                                                                    final int index = msg.what;
//                                                                    card.apduExchange(FinancialCard.getTradingRecordCmdBytes((byte) msg.what), new CpuCard.onReceiveApduExchangeListener() {
//                                                                        @Override
//                                                                        public void onReceiveApduExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
//                                                                            if (!isCmdRunSuc) {
//                                                                                card.close(null);
//                                                                                return;
//                                                                            }
//
//                                                                            msgBuffer.append(FinancialCard.extractTradingRecordFromeRturnBytes(bytApduRtnData));
//                                                                            handler.sendEmptyMessage(0);
//                                                                            theHandler.sendEmptyMessage(index + 1);
//                                                                        }
//                                                                    });
//                                                                } else if (msg.what == 11) { //读完10条交易记录，关闭天线
//                                                                    card.close(null);
//                                                                }
//                                                            }
//                                                        };
//                                                        readYHCHandler.sendEmptyMessage(1);
//                                                    }
//                                                });
//                                            }
//                                        }
//                                    });
//                                }
//                                else {   //深圳通卡，读取余额和交易记录
//                                    System.out.println("发送APDU指令-读余额");
//                                    card.apduExchange(SZTCard.getBalanceCmdByte(), new CpuCard.onReceiveApduExchangeListener() {
//                                        @Override
//                                        public void onReceiveApduExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
//                                            if (SZTCard.getBalance(bytApduRtnData) == null) {
//                                                msgBuffer.append("未知CPU卡！");
//                                                handler.sendEmptyMessage(0);
//                                                System.out.println("未知CPU卡！");
//                                                card.close(null);
//                                                return;
//                                            }
//                                            msgBuffer.append("深圳通余额：" + SZTCard.getBalance(bytApduRtnData));
//                                            handler.sendEmptyMessage(0);
//                                            System.out.println("余额：" + SZTCard.getBalance(bytApduRtnData));
//                                            System.out.println("发送APDU指令-读10条交易记录");
//                                            Handler readSztHandler = new Handler(MainActivity.this.getMainLooper()) {
//                                                @Override
//                                                public void handleMessage(Message msg) {
//                                                    final Handler theHandler = msg.getTarget();
//                                                    if (msg.what <= 10) {  //循环读10条交易记录
//                                                        final int index = msg.what;
//                                                        card.apduExchange(SZTCard.getTradeCmdByte((byte) msg.what), new CpuCard.onReceiveApduExchangeListener() {
//                                                            @Override
//                                                            public void onReceiveApduExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
//                                                                if (!isCmdRunSuc) {
//                                                                    card.close(null);
//                                                                    return;
//                                                                }
//                                                                msgBuffer.append("\r\n" + SZTCard.getTrade(bytApduRtnData));
//                                                                handler.sendEmptyMessage(0);
//                                                                theHandler.sendEmptyMessage(index + 1);
//                                                            }
//                                                        });
//                                                    } else if (msg.what == 11) { //读完10条交易记录，关闭天线
//                                                        card.close(null);
//                                                    }
//                                                }
//                                            };
//                                            readSztHandler.sendEmptyMessage(1);
//                                        }
//                                    });
//                                }
//                            }
//                        });
}