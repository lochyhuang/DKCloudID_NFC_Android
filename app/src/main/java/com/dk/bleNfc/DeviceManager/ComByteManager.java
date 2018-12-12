package com.dk.bleNfc.DeviceManager;

/**
 * Created by lochy on 16/1/21.
 */
public class ComByteManager {
    private ComByteManagerCallback comByteManagerCallback;

    private int RcvFrameType;
    private int RcvFrameNum;
    private byte RcvCommand;
    private byte[] RcvData;
    private int RcvDataLen;
    private byte[] RcvComRunStatus = new byte[2];
    private static int last_frame_num = 0;

    //Command define
    public final static byte PAL_TEST_CHANNEL = 0x00;       //通讯协议测试通道
    public final static byte MIFARE_AUTH_COM = 0x40;        //MIFARE卡验证密钥指令
    public final static byte MIFARE_COM = 0x41;              //Mifare卡指令通道
    public final static byte ACTIVATE_PICC_COM = 0x62;            //激活卡片指令
    public final static byte AUTO_SEARCH_CARD_COM = 0x63;            //自动寻卡片指令
    public final static byte APDU_COM = 0x6F;                    //apdu指令
    public final static byte ANTENNA_OFF_COM = 0x6E;            //关闭天线指令
    public final static byte GET_BT_VALUE_COM = 0x70;            //获取电池电量
    public final static byte GET_VERSIONS_COM = 0x71;            //获取设备版本号指令
    public final static byte BEEP_OPEN_COM = 0x72;               //打开蜂鸣器指令
    public final static byte KEYBOARD_INPUT_COM = 0x73;            //键盘输入指令
    public final static byte NUMBER_DISPLAY_COM = 0x74;            //数码管显示指令
    public final static byte BUTTON_INPUT_COM = 0x75;            //按键按下指令
    public final static byte ANTI_LOST_SWITCH_COM = 0x76;            //防丢器开关指令
    public final static byte CHANGE_BLE_NAME_COM = 0x79;            //修改蓝牙名称
    public final static byte BPDU_COM = 0x7F;                     //BPFU指令
    public final static byte ISO15693_READ_SINGLE_BLOCK_COM = (byte) 0xE0;            /*ISO15693读单个块数据*/
    public final static byte ISO15693_READ_MULTIPLE_BLOCK_COM = (byte) 0xE1;            /*ISO15693读单个块数据*/
    public final static byte ISO15693_WRITE_SINGLE_BLOCK_COM = (byte) 0xE2;            /*ISO15693写单个块数据*/
    public final static byte ISO15693_WRITE_MULTIPLE_BLOCK_COM = (byte) 0xE3;            /*ISO15693写单个块数据*/
    public final static byte ISO15693_LOCK_BLOCK_COM = (byte) 0xE4;            /*ISO15693锁块*/
    public final static byte ISO15693_CMD = (byte) 0xE5;            /*ISO15693命令通道*/
    public final static byte ISO15693_LONG_READ = (byte)0xE6;      /*ISO15693任意长度读指令*/
    public final static byte GET_SUICA_BALANCE_COM = (byte) 0xF0;  //获取SUICA余额指令
    public final static byte FELICA_READ_COM = (byte) 0xF1;         //读FeliCa指令
    public final static byte FELICA_COM = (byte) 0xF2;               //FeliCa指令通道
    public final static byte ULTRALIGHT_CMD = (byte) 0xD0;          //UL卡指令通道
    public final static byte ULTRALIGHT_LONG_READ = (byte) 0xD1;            //UL卡任意长度读指令通道
    public final static byte ULTRALIGHT_LONG_WRITE = (byte) 0xD2;            //UL卡任意长度写指令通道
    public final static byte ANDROID_FAST_PARAMS_COM = (byte) 0x78;            //针对安卓手机的优化的连接参数（传输速度更快）
    public final static byte ISO7816_RESET_CMD = (byte) 0x90;            //ISO7816复位指令
    public final static byte ISO7816_POWE_OFF_CMD = (byte) 0x91;            //ISO7816掉电指令
    public final static byte ISO7816_CMD = (byte) 0x92;              //ISO7816命令传输指令

    public final static byte SAVE_SERIAL_NUMBER_COM = (byte)0x83;            //保存序列号指令
    public final static byte GET_SERIAL_NUMBER_COM = (byte)0x84;            //获取序列号指令

    //Comand run result define
    public final static byte COMAND_RUN_SUCCESSFUL = (byte) 0x90;            //命令运行成功
    public final static byte COMAND_RUN_ERROR = 0x6E;            //命令运行出错

    //Error code defie
    public final static byte NO_ERROR_CODE = 0x00;            //运行正确时的错误码
    public final static byte DEFAULT_ERROR_CODE = (byte) 0x81;            //默认错误码

    public final static byte ISO14443_P3 = 1;
    public final static byte ISO14443_P4 = 2;
    public final static int  PH_EXCHANGE_DEFAULT = 0x0000;
    public final static int  PH_EXCHANGE_LEAVE_BUFFER_BIT = 0x4000;
    public final static int  PH_EXCHANGE_BUFFERED_BIT = 0x8000;
    public final static int PH_EXCHANGE_BUFFER_FIRST = PH_EXCHANGE_DEFAULT | PH_EXCHANGE_BUFFERED_BIT;
    public final static int PH_EXCHANGE_BUFFER_CONT = PH_EXCHANGE_DEFAULT | PH_EXCHANGE_BUFFERED_BIT | PH_EXCHANGE_LEAVE_BUFFER_BIT;
    public final static int PH_EXCHANGE_BUFFER_LAST = PH_EXCHANGE_DEFAULT | PH_EXCHANGE_LEAVE_BUFFER_BIT;


    public final static byte Start_Frame = 0;
    public final static byte Follow_Frame = 1;

    public final static byte MAX_FRAME_NUM = 63;
    public final static byte MAX_FRAME_LEN = 20;

    public final static byte Rcv_Status_Idle = 0;
    public final static byte Rcv_Status_Start = 1;
    public final static byte Rcv_Status_Follow = 2;
    public final static byte Rcv_Status_Complete = 3;

    public ComByteManager(ComByteManagerCallback callback) {
        comByteManagerCallback = callback;
    }

    public byte getCmd() {
        return RcvCommand;
    }

    //获取命令运行状态是否成功
    public boolean getCmdRunStatus() {
        return (RcvComRunStatus[0] == (byte) 0x90);
    }

    public int getRcvDataLen() {
        return RcvDataLen;
    }

    public byte[] getRcvData() {
        if (RcvData == null) {
            return null;
        }
        byte[] bytes = new byte[RcvDataLen];
        System.arraycopy(RcvData, 0, bytes, 0, RcvDataLen);
        return bytes;
    }

    //接收数据处理
    public boolean rcvData(final byte[] bytes) {
        int this_frame_num = 0;
        int status = 0;

        //提取帧类型是开始帧还是后续帧
        if ((bytes[0] & 0xC0) == 0x00) {     //开始帧
            //开始帧必须大于4位
            if (bytes.length < 4) {
                return false;
            }
            RcvFrameType = Start_Frame;
            //如果是开头帧，则提取后续帧个数和命令
            RcvFrameNum = bytes[0] & 0x3F;
            RcvCommand = bytes[1];
            RcvComRunStatus[0] = bytes[2];
            RcvComRunStatus[1] = bytes[3];
            RcvDataLen = bytes.length - 4;
            RcvData = new byte[RcvFrameNum * 19 + RcvDataLen];
            System.arraycopy(bytes, 4, RcvData, 0, RcvDataLen);
            last_frame_num = 0;

            if (RcvFrameNum > 0) {
                status = Rcv_Status_Follow;
            } else {
                status = Rcv_Status_Complete;
            }
        } else if ((bytes[0] & 0xC0) == 0xC0) {   //后续帧
            //后续帧必须大于2位
            if (bytes.length < 2) {
                last_frame_num = 0;
                RcvFrameType = 0;
                RcvFrameNum = 0;
                RcvCommand = 0;
                RcvData = null;
                RcvDataLen = 0;
                RcvComRunStatus[0] = 0;
                RcvComRunStatus[1] = 0;
                return false;
            }
            this_frame_num = bytes[0] & 0x3F;
            if (this_frame_num != (last_frame_num + 1)) {        //帧序号不对
                status = Rcv_Status_Idle;
            } else if (this_frame_num == RcvFrameNum) {  //接收完成
                if (RcvData.length < (RcvDataLen + bytes.length - 1)) {
                    status = Rcv_Status_Idle;
                } else {
                    System.arraycopy(bytes, 1, RcvData, RcvDataLen, bytes.length - 1);
                    RcvDataLen += bytes.length - 1;
                    status = Rcv_Status_Complete;
                }
            } else {                                               //接收中
                if (RcvData.length < (RcvDataLen + bytes.length - 1)) {
                    status = Rcv_Status_Idle;
                } else {
                    last_frame_num = this_frame_num;
                    System.arraycopy(bytes, 1, RcvData, RcvDataLen, bytes.length - 1);
                    RcvDataLen += bytes.length - 1;
                    status = Rcv_Status_Follow;
                }
            }
        } else {
            status = Rcv_Status_Idle;
        }

        //指令接收错误
        if (status == Rcv_Status_Idle) {
            last_frame_num = 0;
            RcvFrameType = 0;
            RcvFrameNum = 0;
            RcvCommand = 0;
            RcvData = null;
            RcvDataLen = 0;
            RcvComRunStatus[0] = 0;
            RcvComRunStatus[1] = 0;
            return false;
        }

        //指令接收完成
        if (status == Rcv_Status_Complete) {  //接收完成、执行命令
            last_frame_num = 0;
            comByteManagerCallback.onRcvBytes(getCmdRunStatus(), getRcvData());
        }

        return true;
//        System.arraycopy(bytes, 0, cmdRcvBytes, cmdRcvLen, bytes.length);
//        cmdRcvLen += bytes.length;
//        if (bytes.length < 20) {
//            byte[] rvcByte = new byte[cmdRcvLen];
//            System.arraycopy(cmdRcvBytes, 0, rvcByte, 0, cmdRcvLen);
//            cmdRcvLen = 0;
//            comByteManagerCallback.onRcvBytes(true, rvcByte);
//            return true;
//        }
//
//        if (mThread != null) {
//            mThread.interrupt();
//            mThread = null;
//        }
//        mThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(300);
//                    byte[] rvcByte = new byte[cmdRcvLen];
//                    System.arraycopy(cmdRcvBytes, 0, rvcByte, 0, cmdRcvLen);
//                    cmdRcvLen = 0;
//                    comByteManagerCallback.onRcvBytes(true, rvcByte);
//                } catch (InterruptedException e) {
//                    // TODO Auto-generated catch block
//                    //e.printStackTrace();
//                }
//            }
//        });
//        mThread.start();
//
//        return false;
    }

    //单帧组帧协议
    public byte[] data_framing_single(int frame_type,
                                      byte frame_num,
                                      byte command,
                                      byte[] original_data,
                                      byte original_data_len) {
        byte[] frame_temp;
        int index = 0;
        int i;

        //帧个数判断
        if (frame_num > MAX_FRAME_NUM) {
            return null;
        }

        if ((original_data == null)) {
            return null;
        }

        //起始数据帧
        if (frame_type == Start_Frame) {
            //数据长度过长判断
            if (original_data_len > (MAX_FRAME_LEN - 2)) {
                return null;
            }

            frame_temp = new byte[original_data_len + 2];
            frame_temp[index++] = frame_num;
            frame_temp[index++] = command;
        } else {   //后续数据帧
            //数据长度过长判断
            if (original_data_len > (MAX_FRAME_LEN - 1)) {
                return null;
            }

            frame_temp = new byte[original_data_len + 1];
            frame_temp[index++] = (byte) (0xC0 | frame_num);
        }

        //数据域
        for (i = 0; i < original_data_len; i++) {
            frame_temp[index++] = original_data[i];
        }

        return frame_temp;
    }

    //完整的组帧协议
    public byte[] data_framing_full(byte command, byte[] pSend_data, int send_data_len) {
        byte[] frame_temp = new byte[MAX_FRAME_LEN];
        byte[] returnFrame;
        int frame_num = 0;
        int frame_len = 0;
        int index = 0;
        int copy_data_len;
        int i = 0;

        //计算帧的个数
        if (send_data_len <= (MAX_FRAME_LEN - 2)) {
            frame_num = 0;
            returnFrame = new byte[send_data_len + 2];
        } else {
            frame_num = (send_data_len - (MAX_FRAME_LEN - 2)) / (MAX_FRAME_LEN - 1);
            if (((send_data_len - (MAX_FRAME_LEN - 2)) % (MAX_FRAME_LEN - 1)) > 0) {
                returnFrame = new byte[frame_num * 20/*中间帧*/ + 20/*第一帧*/ + ((send_data_len - (MAX_FRAME_LEN - 2)) % (MAX_FRAME_LEN - 1)) + 1/*最后一帧*/];
                frame_num++;
            } else {
                returnFrame = new byte[frame_num * 20/*后续帧*/ + 20/*第一帧*/];
            }
        }


        //发送第一帧数据
        for (index = 0; (index < send_data_len) && (index < (MAX_FRAME_LEN - 2)); index++) {
            frame_temp[index] = pSend_data[index];
        }

        byte[] frameSingleTemp = data_framing_single(Start_Frame,
                (byte) frame_num,
                command,
                frame_temp,
                (byte) index);
        //将组好的帧发送出去
        if ((frameSingleTemp != null) && (frameSingleTemp.length != 0) && (frameSingleTemp.length <= MAX_FRAME_LEN)) {
            if (frameSingleTemp.length > returnFrame.length) {
                return null;
            }
            frame_len = frameSingleTemp.length;
            System.arraycopy(frameSingleTemp, 0, returnFrame, 0, frameSingleTemp.length);
        } else {
            return null;
        }

        //如果还有后续帧
        if (frame_num > 0) {
            index = MAX_FRAME_LEN - 2;
            for (i = 0; (i < frame_num) && (index < send_data_len); i++) {
                if ((index + (MAX_FRAME_LEN - 1)) > send_data_len) {
                    copy_data_len = ((send_data_len - (MAX_FRAME_LEN - 2)) % (MAX_FRAME_LEN - 1));
                } else {
                    copy_data_len = MAX_FRAME_LEN - 1;
                }

                System.arraycopy(pSend_data, index, frame_temp, 0, copy_data_len);
                index += copy_data_len;
                //组帧
                byte[] frameSingleTemp1 = data_framing_single(Follow_Frame,
                        (byte) (i + 1),
                        (byte) 0,
                        frame_temp,
                        (byte) copy_data_len);
                //将组好的帧发送出去
                if ((frameSingleTemp1 != null) && (frameSingleTemp1.length != 0) && (frameSingleTemp1.length <= MAX_FRAME_LEN)) {
                    if ((frameSingleTemp1.length + frame_len) > returnFrame.length) {
                        return null;
                    }
                    System.arraycopy(frameSingleTemp1, 0, returnFrame, frame_len, frameSingleTemp1.length);
                    frame_len += frameSingleTemp1.length;
                } else {
                    return null;
                }
            }
        }
        return returnFrame;
    }

    //A卡激活指令
    public byte[] AActivityComByte() {
        return new byte[]{0x00, ACTIVATE_PICC_COM};
    }
    public byte[] AActivityComByte(byte protocolLayer) {
        return new byte[]{0x00, ACTIVATE_PICC_COM, protocolLayer};
    }

    //A卡去激活指令
    public byte[] rfPowerOffComByte() {
        return new byte[] {0x00, ANTENNA_OFF_COM};
    }

    //获取蓝牙读卡器电池电压指令
    public byte[] getBtValueComByte() {
        return new byte[] {0x00, GET_BT_VALUE_COM};
    }

    //获取设备版本号指令
    public byte[] getVersionsComByte() {
        return new byte[] {0x00, GET_VERSIONS_COM};
    }

    //非接接口Apdu指令
    public byte[] rfApduCmdByte(byte[] adpuCmd) {
        return data_framing_full(APDU_COM, adpuCmd, adpuCmd.length);
    }

    //Felica读余额指令通道
    public byte[] getSuicaBalanceCmdByte() {
        return new byte[] {0x00, GET_SUICA_BALANCE_COM};
    }

    //Felica读指令通道
    public byte[] readFeliCaCmdByte(byte[] systemCode, byte[] blockAddr) {
        return new byte[] {0x00, FELICA_READ_COM, systemCode[0], systemCode[1], blockAddr[0], blockAddr[1]};
    }

    //Felica指令通道
    public byte[] felicaCmdByte(int wOption, int wN, byte[] dataBytes) {
        byte[] bytesTem = new byte[dataBytes.length + 4];
        bytesTem[0] = (byte) ((wOption >> 8) & 0x00ff);
        bytesTem[1] = (byte) (wOption & 0x00ff);
        bytesTem[2] = (byte) ((wN >> 8) & 0x00ff);
        bytesTem[3] = (byte) (wN & 0x00ff);
        System.arraycopy(dataBytes, 0, bytesTem, 4, dataBytes.length );
        return data_framing_full(FELICA_COM, bytesTem, bytesTem.length);
    }

    //打开蜂鸣器指令
    //openTimeMs: 打开蜂鸣器时间：0~0xffff，单位ms
    public byte[] openBeepCmdBytes(int openTimesMs) {
        byte[] timesBytes = new byte[2];
        timesBytes[0] = (byte)((openTimesMs & 0x0000ff00) >> 8);
        timesBytes[1] = (byte)(openTimesMs & 0x000000ff);
        return new byte[] {0x00, BEEP_OPEN_COM, timesBytes[0], timesBytes[1]};
    }

    //打开蜂鸣器指令
    //onDelayMs: 打开蜂鸣器时间：0~0xffff，单位ms
    //offDelayMs：关闭蜂鸣器时间：0~0xffff，单位ms
    //n：蜂鸣器响多少声：0~255
    public byte[] openBeepCmdBytes(int onDelayMs, int offDelayMs, int n) {
        byte[] dataBytes = new byte[5];
        dataBytes[0] = (byte)((onDelayMs & 0x0000ff00) >> 8);
        dataBytes[1] = (byte)(onDelayMs & 0x000000ff);
        dataBytes[2] = (byte)((offDelayMs & 0x0000ff00) >> 8);
        dataBytes[3] = (byte)(offDelayMs & 0x000000ff);
        dataBytes[4] = (byte) (n & 0x000000ff);
        return data_framing_full(BEEP_OPEN_COM, dataBytes, dataBytes.length);
    }

    //UL指令通道
    public byte[] ultralightCmdByte(byte[] ulCmd) {
        return data_framing_full(ULTRALIGHT_CMD, ulCmd, ulCmd.length);
    }

    //UL卡快速读指令通道
    //startAddress：要读的起始地址
    //number：要读的块数量（一个块4 byte）， 0 < number < 0x3f
    public byte[] ultralightLongReadCmdBytes(byte startAddress, int number) {
        if (number < 0 || number > 0x3f) {
            return null;
        }
        return new byte[] {0x00, ULTRALIGHT_LONG_READ, startAddress, (byte) (number & 0x00ff)};
    }

    //UL卡快速写指令通道
    //startAddress：要写的起始地址
    //data：要写的数据
    public byte[] ultralightLongWriteCmdBytes(byte startAddress, byte[] data) {
        byte cmdBytes[] = new byte[data.length + 1];
        cmdBytes[0] = startAddress;
        System.arraycopy(data, 0, cmdBytes, 1, data.length);
        return data_framing_full(ULTRALIGHT_LONG_WRITE, cmdBytes, cmdBytes.length);
    }

    //身份证指令接口
    public byte[] rfBpduCmdByte(byte[] bpuCmd) {
        return data_framing_full(BPDU_COM, bpuCmd, bpuCmd.length);
    }

    //Mifare卡验证密码指令
    public byte[] rfMifareAuthCmdByte(byte bBlockNo, byte bKeyType, byte[] pKey, byte[] pUid) {
        byte[] returnByte = new byte[2 + 1 + 1 + 6 + 4];
        returnByte[0] = 0x00;
        returnByte[1] = MIFARE_AUTH_COM;
        returnByte[2] = bBlockNo;
        returnByte[3] = bKeyType;
        System.arraycopy(pKey,0, returnByte, 4, 6);
        System.arraycopy(pUid, 0, returnByte, 10, 4);
        return returnByte;
    }

    //Mifarek卡数据交换指令
    public byte[] rfMifareDataExchangeCmdByte(byte[] dataBytes) {
        return data_framing_full(MIFARE_COM, dataBytes, dataBytes.length);
    }

    //通信协议测试通道指令
    public byte[] getTestChannelBytes(byte[] dataBytes) {
        return data_framing_full(PAL_TEST_CHANNEL, dataBytes, dataBytes.length);
    }

    //ISO15693读单个块数据指令
    //uid:要读的卡片的uid，必须4个字节
    //addr：要读的块地址
    public byte[] iso15693ReadSingleBlockCmdBytes(byte uid[], byte addr) {
        if (uid.length < 4) {
            return null;
        }
        byte[] dataBytes = new byte[5];
        System.arraycopy(uid, 0, dataBytes, 0, 4);
        dataBytes[4] = addr;

        return data_framing_full(ISO15693_READ_SINGLE_BLOCK_COM, dataBytes, dataBytes.length);
    }

    //ISO15693读多个块数据指令
    //uid:要读的卡片的uid，必须4个字节
    //addr：要读的块地址
    //number:要读的块数量,必须大于0
    public byte[] iso15693ReadMultipleBlockCmdBytes(byte uid[], byte addr, byte number) {
        if (uid.length < 4) {
            return null;
        }
        if (number == 0) {
            return null;
        }
        byte[] dataBytes = new byte[6];
        System.arraycopy(uid, 0, dataBytes, 0, 4);
        dataBytes[4] = addr;
        dataBytes[5] = (byte) ((number & 0xff) - 1);

        return data_framing_full(ISO15693_READ_MULTIPLE_BLOCK_COM, dataBytes, dataBytes.length);
    }

    //ISO15693写一个块
    //uid:要写的卡片的uid，必须4个字节
    //addr：要写卡片的块地址
    //writeData:要写的数据，必须4个字节
    public byte[] iso15693WriteSingleBlockCmdBytes(byte uid[], byte addr, byte writeData[]) {
        if ( (writeData.length < 4) || (uid.length < 4) ) {
            return null;
        }
        byte[] dataBytes = new byte[9];
        System.arraycopy(uid, 0, dataBytes, 0, 4);
        dataBytes[4] = addr;
        System.arraycopy(writeData, 0, dataBytes, 5, 4);

        return data_framing_full(ISO15693_WRITE_SINGLE_BLOCK_COM, dataBytes, dataBytes.length);
    }

    //ISO15693写多个块
    //uid:要写的卡片的uid，必须4个字节
    //addr：要写的块地址
    //number:要写的块数量,必须大于0
    //writeData: 要写的数据，必须number * 4字节
    public byte[] iso15693WriteMultipleBlockCmdBytes(byte uid[], byte addr, byte number, byte writeData[]) {
        if (uid.length < 4) {
            return null;
        }
        if (number == 0) {
            return null;
        }
        if (writeData.length != number * 4) {
            return null;
        }
        byte[] dataBytes = new byte[6 + writeData.length];
        System.arraycopy(uid, 0, dataBytes, 0, 4);
        dataBytes[4] = addr;
        dataBytes[5] = (byte) ((number & 0xff) - 1);
        System.arraycopy(writeData, 0, dataBytes, 6, writeData.length);

        return data_framing_full(ISO15693_WRITE_MULTIPLE_BLOCK_COM, dataBytes, dataBytes.length);
    }

    //ISO15693锁住一个块
    //uid：要写的卡片的UID，必须4个字节
    //addr：要锁住的块地址
    public byte[] iso15693LockBlockCmdBytes(byte uid[], byte addr) {
        if (uid.length < 4) {
            return null;
        }
        byte[] dataBytes = new byte[5];
        System.arraycopy(uid, 0, dataBytes, 0, 4);
        dataBytes[4] = addr;

        return data_framing_full(ISO15693_LOCK_BLOCK_COM, dataBytes, dataBytes.length);
    }

    //ISO15693指令通道
    public byte[] iso15693CmdBytes(byte[] cmdBytes) {
        return data_framing_full(ISO15693_CMD, cmdBytes, cmdBytes.length);
    }

    //ISO15693快速读指令通道
    //startAddress：要读的起始地址
    //number：要读的块数量（一个块4 byte）， 0 < number < 0x3f
    public byte[] iso15693LongReadCmdBytes(byte startAddress, int number) {
        if (number < 0 || number > 0x3f) {
            return null;
        }
        return new byte[] {0x00, ISO15693_LONG_READ, startAddress, (byte) (number & 0x00ff)};
    }

    //防丢器开关指令
    //s：true：打开防丢器功能 false：关闭防丢器功能
    public byte[] antiLostSwitchCmdBytes(boolean s) {
        byte[] dataBytes = new byte[1];
        dataBytes[0] = s ? (byte)1 : (byte)0;
        return data_framing_full(ANTI_LOST_SWITCH_COM, dataBytes, dataBytes.length);
    }

    //开启/关闭快速传输，只对安卓手机有效
    //s：true：打开快速传输 false：关闭快速传输
    public byte[] androidFastParamsCmdBytes(boolean s) {
        byte[] dataBytes = new byte[1];
        dataBytes[0] = s ? (byte)1 : (byte)0;
        return data_framing_full(ANDROID_FAST_PARAMS_COM, dataBytes, dataBytes.length);
    }

    //PSam上电复位指令
    public byte[] resetPSamCmdBytes() {
        return new byte[] {0x00, ISO7816_RESET_CMD};
    }

    //PSam掉电指令
    public byte[] PSamPowerDownCmdBytes() {
        return new byte[] {0x00, ISO7816_POWE_OFF_CMD};
    }

    //PSam APDU传输命令
    public byte[] PSamApduCmdBytes(byte[] data) {
        return data_framing_full(ISO7816_CMD, data, data.length);
    }

    //自动寻卡
    //en：true-开启自动寻卡，false：关闭自动寻卡
    //delayMs：寻卡间隔,单位 10毫秒
    //bytCardType: ISO14443_P3-寻M1/UL卡，ISO14443_P4-寻CPU卡
    public byte[] autoSearchCardCmdBytes(boolean en, byte delayMs, byte bytCardType) {
        byte[] bytes = {en?(byte)0xff:0x00, delayMs, bytCardType};
        return data_framing_full(AUTO_SEARCH_CARD_COM, bytes, bytes.length);
    }

    //修改蓝牙名称
    //bytes：转换成bytes后的名称
    public byte[] changeBleNameCmdBytes(byte[] bytes) {
        return data_framing_full(CHANGE_BLE_NAME_COM, bytes, bytes.length);
    }

    //保存序列号
    //serialNumberBytes：序列号，小于等于32字节
    public byte[] saveSerialNumberCmdBytes(byte[] serialNumberBytes) {
        return data_framing_full(SAVE_SERIAL_NUMBER_COM, serialNumberBytes, serialNumberBytes.length);
    }

    //获取序列号
    public byte[] getSerialNumberCmdBytes() {
        return new byte[] {0x00, GET_SERIAL_NUMBER_COM};
    }
}
