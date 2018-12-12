package com.dk.bleNfc.card;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

import com.dk.bleNfc.DeviceManager.*;
import com.dk.bleNfc.Exception.CardNoResponseException;
import com.dk.bleNfc.Exception.DeviceNoResponseException;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by Administrator on 2016/9/19.
 */
public class Ntag21x extends Ultralight {
    public final static byte TYPE_NTAG213 = 0x0F;
    public final static byte TYPE_NTAG215 = 0x11;
    public final static byte TYPE_NTAG216 = 0x13;
    public final static int SIZE_NTAG213 = 144;
    public final static int SIZE_NTAG215 = 504;
    public final static int SIZE_NTAG216 = 888;

    public final static String ERR_MEMORY_OUT = "Data is too long for this tag!";
    public final static String ERR_WRITE_FAIL = "Write data fail!";
    public final static String ERR_NO_ERROR = "No error";

    public final static String NDEF_TYPE = "text/plain";

    public int size = 0;

    public onReceiveLongWriteListener mOnReceiveLongWrite;
    public onReceiveNdefTextWriteListener mOnReceiveNdefTextWriteListener;
    public onReceiveNdefTextReadListener mOnReceiveNdefTextReadListener;
    public onReceiveLongReadListener mOnReceiveLongReadListener;
    public onReceiveScheduleListener mOnReceiveReadScheduleListener;
    public onReceiveScheduleListener mOnReceiveWriteScheduleListener;
    public onReceiveScheduleListener mOnReceiveNdefWriteScheduleListener;
    public onReceiveScheduleListener mOnReceiveNdefReadScheduleListener;

    public Ntag21x(DeviceManager deviceManager) {
        super(deviceManager);
    }

    public Ntag21x(DeviceManager deviceManager, byte[] uid, byte[] atr) {
        super(deviceManager, uid, atr);
    }

    //任意长度数据写回调
    public interface onReceiveLongWriteListener {
        public void onReceiveLongWrite(String error);
    }

    //任意长度数据读回调
    public interface onReceiveLongReadListener {
        public void onReceiveLongRead(boolean isSuc, byte[] returnBytes);
    }

    //写一个NDEF文本格式到标签回调
    public interface onReceiveNdefTextWriteListener {
        public void onReceiveNdefTextWrite(String error);
    }

    //从标签中读取一个NEDF文本格式的数据回调
    public interface onReceiveNdefTextReadListener {
        public void onReceiveNdefTextRead(String eer, String returnString);
    }

    //读写进度回调
    public interface onReceiveScheduleListener{
        void onReceiveSchedule(int rate);
    }

    /**
     * 任意长度写，异步回调方式
     * @param startAddress 要写入的起始地址
     * @param writeBytes 要写入的数据
     * @param listener 写入结果会通过listener回调，如果回调错误为null则写入成功
     */
    public void longWrite(byte startAddress, final byte[] writeBytes, onReceiveLongWriteListener listener) {
        mOnReceiveLongWrite = listener;
        final byte[] writeBytesTemp = writeBytes;
        final byte startAddressTemp = startAddress;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean isSuc = longWrite(startAddressTemp, writeBytesTemp);
                    if (isSuc) {
                        if (mOnReceiveLongWrite != null) {
                            mOnReceiveLongWrite.onReceiveLongWrite(null);
                        }
                    }
                    else {
                        if (mOnReceiveLongWrite != null) {
                            mOnReceiveLongWrite.onReceiveLongWrite(ERR_WRITE_FAIL);
                        }
                    }
                } catch (CardNoResponseException e) {
                    e.printStackTrace();
                    if (mOnReceiveLongWrite != null) {
                        mOnReceiveLongWrite.onReceiveLongWrite(e.getMessage());
                    }
                }
            }
        }).start();
    }

    /**
     * 任意长度写，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param startAddress 要写入的起始地址
     * @param writeBytes 要写入的数据
     * @return         true：写入成功，  false：写入失败
     * @throws CardNoResponseException
     *                  卡片无响应时会抛出异常
     */
    public boolean longWrite(byte startAddress, byte[] writeBytes) throws CardNoResponseException {
        final byte startAddressTemp;
        startAddressTemp = startAddress;

        byte[] returnBytes;
        returnBytes = read((byte) 0);
        switch (returnBytes[14]) {
            case 0x12:
                size = SIZE_NTAG213;
                break;
            case 0x3e:
                size = SIZE_NTAG215;
                break;
            case 0x6d:
            case 0x6f:
                size = SIZE_NTAG216;
                break;
            default:
                size = SIZE_NTAG213;
                break;
        }

        //写入数据长度超过卡片容量
        if (writeBytes.length + (startAddressTemp & 0x00ff) * 4 > (size + 16)) {
            throw new CardNoResponseException(ERR_MEMORY_OUT);
        }

        int currentWriteAddress = startAddress & 0x00ff;
        byte[] writeByteTemp = new byte[LONG_READ_MAX_NUMBER * 4];
        int i = 0;
        for (i = 0; (i+LONG_READ_MAX_NUMBER) <= (writeBytes.length / 4); i += LONG_READ_MAX_NUMBER) {
            System.arraycopy(writeBytes, i * 4, writeByteTemp, 0, LONG_READ_MAX_NUMBER * 4);
            boolean isSuc = longWriteSingle((byte) (currentWriteAddress & 0x00ff), writeByteTemp);
            if (!isSuc) {
                return false;
            }
            currentWriteAddress += LONG_READ_MAX_NUMBER;
        }

        if (writeBytes.length % (LONG_READ_MAX_NUMBER * 4) > 0) {
            writeByteTemp = new byte[writeBytes.length % (LONG_READ_MAX_NUMBER * 4)];
            System.arraycopy(writeBytes, i * 4, writeByteTemp, 0, writeBytes.length % (LONG_READ_MAX_NUMBER * 4));
            boolean isSuc = longWriteSingle((byte) (currentWriteAddress & 0x00ff), writeByteTemp);
            if (!isSuc) {
                return false;
            }
        }
        return true;
    }

    /**
     * 任意长度写，带写进度回调，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param startAddress 要写入的起始地址
     * @param writeBytes 要写入的数据
     * @param listener 写的过程中会不断回调写入进度
     * @return         true：写入成功，  false：写入失败
     * @throws CardNoResponseException
     *                  卡片无响应时会抛出异常
     */
    public boolean longWriteWithScheduleCallback(byte startAddress, byte[] writeBytes, onReceiveScheduleListener listener) throws CardNoResponseException {
        mOnReceiveWriteScheduleListener = listener;
        final byte startAddressTemp;
        startAddressTemp = startAddress;

        byte[] returnBytes;
        returnBytes = read((byte) 0);
        switch (returnBytes[14]) {
            case 0x12:
                size = SIZE_NTAG213;
                break;
            case 0x3e:
                size = SIZE_NTAG215;
                break;
            case 0x6d:
            case 0x6f:
                size = SIZE_NTAG216;
                break;
            default:
                size = SIZE_NTAG213;
                break;
        }

        //写入数据长度超过卡片容量
        if (writeBytes.length + (startAddressTemp & 0x00ff) * 4 > (size + 16)) {
            throw new CardNoResponseException(ERR_MEMORY_OUT);
        }

        int currentWriteAddress = startAddress & 0x00ff;
        byte[] writeByteTemp = new byte[LONG_READ_MAX_NUMBER * 4];
        int i = 0;
        for (i = 0; (i+LONG_READ_MAX_NUMBER) <= (writeBytes.length / 4); i += LONG_READ_MAX_NUMBER) {
            System.arraycopy(writeBytes, i * 4, writeByteTemp, 0, LONG_READ_MAX_NUMBER * 4);
            boolean isSuc = longWriteSingle((byte) (currentWriteAddress & 0x00ff), writeByteTemp);
            if (!isSuc) {
                return false;
            }
            if ((i != 0) && (mOnReceiveWriteScheduleListener != null)) {
                mOnReceiveWriteScheduleListener.onReceiveSchedule(i * 400 / writeBytes.length);
            }
            currentWriteAddress += LONG_READ_MAX_NUMBER;
        }

        if (writeBytes.length % (LONG_READ_MAX_NUMBER * 4) > 0) {
            writeByteTemp = new byte[writeBytes.length % (LONG_READ_MAX_NUMBER * 4)];
            System.arraycopy(writeBytes, i * 4, writeByteTemp, 0, writeBytes.length % (LONG_READ_MAX_NUMBER * 4));
            boolean isSuc = longWriteSingle((byte) (currentWriteAddress & 0x00ff), writeByteTemp);
            if (!isSuc) {
                return false;
            }
        }

        if (mOnReceiveWriteScheduleListener != null) {
            mOnReceiveWriteScheduleListener.onReceiveSchedule(100);
        }
        return true;
    }

    /**
     * 任意长度读，异步回调方式
     * @param startAddress 要读取的起始地址
     * @param endAddress 要读取的结束地址
     * @param listener 读取结果会通过listener回调
     */
    public void longRead(byte startAddress, byte endAddress, onReceiveLongReadListener listener) {
        mOnReceiveLongReadListener = listener;

        final byte startAddressTemp = startAddress;
        final byte endAddressTemp = endAddress;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] returnBytes = longRead(startAddressTemp, endAddressTemp);
                    if (mOnReceiveLongReadListener != null) {
                        mOnReceiveLongReadListener.onReceiveLongRead(true, returnBytes);
                    }
                } catch (CardNoResponseException e) {
                    e.printStackTrace();
                    if (mOnReceiveLongReadListener != null) {
                        mOnReceiveLongReadListener.onReceiveLongRead(false, null);
                    }
                }
            }
        }).start();
    }

    /**
     * 任意长度读，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param startAddress 要读取的起始地址
     * @param endAddress 要读取的结束地址
     * @return         读取到的数据
     * @throws CardNoResponseException
     *                  卡片无响应时会抛出异常
     */
    public byte[] longRead(byte startAddress, byte endAddress) throws CardNoResponseException {
        if ( (startAddress & 0x00ff) > (endAddress & 0x00ff) ) {
            throw new CardNoResponseException("Start Address must be smaller than end Address");
        }

        byte[] readDataBytes = new byte[((endAddress & 0x00ff) - (startAddress & 0x00ff) + 1) * 4];
        int readDataLen = 0;

        int currentStartAddress = startAddress & 0x00ff;
        int currentEndAddress = currentStartAddress + LONG_READ_MAX_NUMBER - 1;
        byte[] returnBytes;

        if ( ((endAddress & 0x00ff) - (startAddress & 0x00ff) + 1) >=  LONG_READ_MAX_NUMBER) {
            while ((currentEndAddress & 0x00ff) <= (endAddress & 0x00ff)) {
                returnBytes = longReadSingle((byte) currentStartAddress, LONG_READ_MAX_NUMBER);
                System.arraycopy(returnBytes, 0, readDataBytes, readDataLen, returnBytes.length);
                readDataLen += LONG_READ_MAX_NUMBER * 4;
                currentStartAddress = (currentEndAddress & 0x00ff) + 1;
                currentEndAddress += LONG_READ_MAX_NUMBER;
            }
        }

        int surplusBlock = ((endAddress & 0x00ff) - (startAddress & 0x00ff) + 1) % LONG_READ_MAX_NUMBER;
        if ( surplusBlock != 0 ) {
            returnBytes = longReadSingle((byte)(currentStartAddress & 0x00ff), surplusBlock);
            System.arraycopy(returnBytes, 0, readDataBytes, readDataLen, surplusBlock * 4);
        }
        return readDataBytes;
    }

    /**
     * 任意长度读，带读进度回调，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param startAddress 要读取的起始地址
     * @param endAddress 要读取的结束地址
     * @param listener 读取过程中会不断回调读取进度
     * @return         读取到的数据
     * @throws CardNoResponseException
     *                  卡片无响应时会抛出异常
     */
    public byte[] longReadWithScheduleCallback(byte startAddress, byte endAddress, onReceiveScheduleListener listener) throws CardNoResponseException {
        mOnReceiveReadScheduleListener = listener;

        if ( (startAddress & 0x00ff) > (endAddress & 0x00ff) ) {
            throw new CardNoResponseException("Start Address must be smaller than end Address");
        }

        byte[] readDataBytes = new byte[((endAddress & 0x00ff) - (startAddress & 0x00ff) + 1) * 4];
        int readDataLen = 0;

        int currentStartAddress = startAddress & 0x00ff;
        int currentEndAddress = currentStartAddress + LONG_READ_MAX_NUMBER - 1;
        byte[] returnBytes;

        if ( ((endAddress & 0x00ff) - (startAddress & 0x00ff) + 1) >=  LONG_READ_MAX_NUMBER) {
            while ((currentEndAddress & 0x00ff) <= (endAddress & 0x00ff)) {
                returnBytes = longReadSingle((byte) currentStartAddress, LONG_READ_MAX_NUMBER);
                System.arraycopy(returnBytes, 0, readDataBytes, readDataLen, returnBytes.length);
                readDataLen += LONG_READ_MAX_NUMBER * 4;
                currentStartAddress = (currentEndAddress & 0x00ff) + 1;
                currentEndAddress += LONG_READ_MAX_NUMBER;

                //回调读取进度
                if (mOnReceiveReadScheduleListener != null) {
                    if ((readDataLen != 0) && (readDataLen < readDataBytes.length)) {
                        mOnReceiveReadScheduleListener.onReceiveSchedule(readDataLen * 100 / readDataBytes.length);
                    }
                }
            }
        }

        int surplusBlock = ((endAddress & 0x00ff) - (startAddress & 0x00ff) + 1) % LONG_READ_MAX_NUMBER;
        if ( surplusBlock != 0 ) {
            returnBytes = longReadSingle((byte)(currentStartAddress & 0x00ff), surplusBlock);
            System.arraycopy(returnBytes, 0, readDataBytes, readDataLen, surplusBlock * 4);
        }

        //回调读取进度
        if (mOnReceiveReadScheduleListener != null) {
            mOnReceiveReadScheduleListener.onReceiveSchedule(100);
        }
        return readDataBytes;
    }

    /**
     * 写一个NDEF文本格式到标签，异步回调方式
     * @param text 要写的文本
     * @param listener 写入结果会通过listener回调
     */
    public void NdefTextWrite(String text, onReceiveNdefTextWriteListener listener) {
        mOnReceiveNdefTextWriteListener = listener;

        final String textTemp = text;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean isSuc = NdefTextWrite(textTemp);
                    if (isSuc) {
                        if (mOnReceiveNdefTextWriteListener != null) {
                            mOnReceiveNdefTextWriteListener.onReceiveNdefTextWrite(null);
                        }
                    }
                    else {
                        if (mOnReceiveNdefTextWriteListener != null) {
                            mOnReceiveNdefTextWriteListener.onReceiveNdefTextWrite(ERR_WRITE_FAIL);
                        }
                    }
                } catch (CardNoResponseException e) {
                    e.printStackTrace();
                    if (mOnReceiveNdefTextWriteListener != null) {
                        mOnReceiveNdefTextWriteListener.onReceiveNdefTextWrite(e.getMessage());
                    }
                }
            }
        }).start();
    }

    /**
     * 写一个NDEF文本格式到标签，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param text 要写的文本
     * @return         true:写入成功  false：写入失败
     * @throws CardNoResponseException
     *                  卡片无响应时会抛出异常
     */
    public boolean NdefTextWrite(String text) throws CardNoResponseException {
        byte[] rececrdByte = createTextRecord(text).getPayload();
        System.out.println(rececrdByte);

        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[] {createTextRecord(text)});
        byte[] NDEFTextByte = ndefMessage.toByteArray();
        byte[] NDEFHandleByte;
        if (NDEFTextByte.length >= 0xff) {
            NDEFHandleByte = new byte[] {0x03, (byte) 0xff, (byte) ((NDEFTextByte.length >> 8) & 0x00ff), (byte) (NDEFTextByte.length & 0x00ff)};
        }
        else {
            NDEFHandleByte = new byte[] {0x03, (byte) NDEFTextByte.length};
        }

        byte[] writeBytes = new byte[NDEFHandleByte.length + NDEFTextByte.length + 1];

        int index = 0;
        System.arraycopy(NDEFHandleByte, 0, writeBytes, index, NDEFHandleByte.length);
        index += NDEFHandleByte.length;
        System.arraycopy(NDEFTextByte, 0, writeBytes, index, NDEFTextByte.length);
        writeBytes[writeBytes.length - 1] = (byte) 0xFE;

        return longWrite((byte) 4, writeBytes);
    }

    /**
     * 写一个NDEF文本格式到标签，带读进度回调，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param text 要写的文本
     * @param listener 写入过程中会不断回调写入进度
     * @return         true:写入成功  false：写入失败
     * @throws CardNoResponseException
     *                  卡片无响应时会抛出异常
     */
    public boolean NdefTextWriteWithScheduleCallback(String text, onReceiveScheduleListener listener) throws CardNoResponseException {
        mOnReceiveNdefWriteScheduleListener = listener;
        byte[] rececrdByte = createTextRecord(text).getPayload();
        System.out.println(rececrdByte);

        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[] {createTextRecord(text)});
        byte[] NDEFTextByte = ndefMessage.toByteArray();
        byte[] NDEFHandleByte;
        if (NDEFTextByte.length >= 0xff) {
            NDEFHandleByte = new byte[] {0x03, (byte) 0xff, (byte) ((NDEFTextByte.length >> 8) & 0x00ff), (byte) (NDEFTextByte.length & 0x00ff)};
        }
        else {
            NDEFHandleByte = new byte[] {0x03, (byte) NDEFTextByte.length};
        }

        byte[] writeBytes = new byte[NDEFHandleByte.length + NDEFTextByte.length + 1];

        int index = 0;
        System.arraycopy(NDEFHandleByte, 0, writeBytes, index, NDEFHandleByte.length);
        index += NDEFHandleByte.length;
        System.arraycopy(NDEFTextByte, 0, writeBytes, index, NDEFTextByte.length);
        writeBytes[writeBytes.length - 1] = (byte) 0xFE;

        return longWriteWithScheduleCallback((byte) 4, writeBytes, new onReceiveScheduleListener() {
            @Override
            public void onReceiveSchedule(int rate) {
                if (mOnReceiveNdefWriteScheduleListener != null) {
                    mOnReceiveNdefWriteScheduleListener.onReceiveSchedule(rate);
                }
            }
        });
    }

    /**
     * 从标签中读取一个NEDF文本格式的数据，异步回调方式
     * @param listener 读取结果会通过listener回调
     */
    public void NdefTextRead(onReceiveNdefTextReadListener listener) {
        mOnReceiveNdefTextReadListener = listener;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String text = NdefTextRead();
                    mOnReceiveNdefTextReadListener.onReceiveNdefTextRead(null, text);
                } catch (CardNoResponseException e) {
                    e.printStackTrace();
                    if (mOnReceiveNdefTextReadListener != null) {
                        mOnReceiveNdefTextReadListener.onReceiveNdefTextRead(e.getMessage(), null);
                    }
                }
            }
        }).start();
    }

    /**
     * 从标签中读取一个NEDF文本格式的数据，带读进度回调，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @return         获取到的文本
     * @throws CardNoResponseException
     *                  卡片无响应时会抛出异常
     */
    public String NdefTextRead() throws CardNoResponseException {
        byte[] returnBytes = read((byte)4);
        if ( (returnBytes == null) || (returnBytes.length != 16) ) {
            throw new CardNoResponseException("Read card fail");
        }
        if ((returnBytes[0] == 0x03) || (returnBytes[1] == 0x03) ) {
            int j;
            int i;
            byte[] imageBytes = NDEF_TYPE.getBytes();
            boolean searchFlag = false;
            for (i = 0; i < 15; i++) {
                searchFlag = true;
                for (j = i; (j < 16) && ((j - i) < 10); j++) {
                    if (returnBytes[j] != imageBytes[j - i]) {
                        searchFlag = false;
                        break;
                    }
                }
                if (searchFlag) {
                    break;
                }
            }
            if (searchFlag) {
                int imageStartAddr = i;
                int textLen;
                if ((imageStartAddr > 4) && (returnBytes[i-3] == 0x00) && (returnBytes[i-4] == 0x00)) {
                    textLen = (returnBytes[i - 1] & 0x00ff) + ((returnBytes[i - 2] & 0x00ff) << 8);
                }
                else {
                    textLen = returnBytes[i - 1] & 0x00ff;
                }
                int recordLen = textLen + i + 10;
                byte recordEndAddress = (byte) ((recordLen + 3) / 4 + 4);
                returnBytes = longRead((byte) 4, recordEndAddress);

                if ( (returnBytes == null) || (returnBytes.length < recordLen) || (returnBytes.length < textLen + imageStartAddr + 10)) {
                    throw new CardNoResponseException("Read card fail");
                }
                try {
                    return new String(returnBytes, imageStartAddr + 10,
                            textLen, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new CardNoResponseException("No NDEF text payload!");
                }
            }
            else {
                throw new CardNoResponseException("No NDEF text payload!");
            }
        }
        else {
            throw new CardNoResponseException("No NDEF text payload!");
        }
    }

    /**
     * 从标签中读取一个NEDF文本格式的数据，带读进度回调，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param listener 写入过程中会不断回调写入进度
     * @return         获取到的文本
     * @throws CardNoResponseException
     *                  卡片无响应时会抛出异常
     */
    public String NdefTextReadWithScheduleCallback(onReceiveScheduleListener listener) throws CardNoResponseException {
        mOnReceiveNdefReadScheduleListener = listener;
        byte[] returnBytes = read((byte)4);
        if ( (returnBytes == null) || (returnBytes.length != 16) ) {
            throw new CardNoResponseException("Read card fail");
        }
        if ((returnBytes[0] == 0x03) || (returnBytes[1] == 0x03) ) {
            int j;
            int i;
            byte[] imageBytes = NDEF_TYPE.getBytes();
            boolean searchFlag = false;
            for (i = 0; i < 15; i++) {
                searchFlag = true;
                for (j = i; (j < 16) && ((j - i) < 10); j++) {
                    if (returnBytes[j] != imageBytes[j - i]) {
                        searchFlag = false;
                        break;
                    }
                }
                if (searchFlag) {
                    break;
                }
            }
            if (searchFlag) {
                int imageStartAddr = i;
                int textLen;
                if ((imageStartAddr > 4) && (returnBytes[i-3] == 0x00) && (returnBytes[i-4] == 0x00)) {
                    textLen = (returnBytes[i - 1] & 0x00ff) + ((returnBytes[i - 2] & 0x00ff) << 8);
                }
                else {
                    textLen = returnBytes[i - 1] & 0x00ff;
                }
                int recordLen = textLen + i + 10;
                byte recordEndAddress = (byte) ((recordLen + 3) / 4 + 4);
                returnBytes = longReadWithScheduleCallback((byte) 4, recordEndAddress, new onReceiveScheduleListener() {
                    @Override
                    public void onReceiveSchedule(int rate) {
                        if (mOnReceiveNdefReadScheduleListener != null) {
                            mOnReceiveNdefReadScheduleListener.onReceiveSchedule(rate);
                        }
                    }
                });

                if ( (returnBytes == null) || (returnBytes.length < recordLen) || (returnBytes.length < textLen + imageStartAddr + 10)) {
                    throw new CardNoResponseException("Read card fail");
                }
                try {
                    return new String(returnBytes, imageStartAddr + 10,
                            textLen, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new CardNoResponseException("No NDEF text payload!");
                }
            }
            else {
                throw new CardNoResponseException("No NDEF text payload!");
            }
        }
        else {
            throw new CardNoResponseException("No NDEF text payload!");
        }
    }

    //创建一个封装要写入的文本的NdefRecord对象
    private NdefRecord createTextRecord(String text) {
        //生成语言编码的字节数组，中文编码
        byte[] langBytes = Locale.CHINA.getLanguage().getBytes(
                Charset.forName("US-ASCII"));
        //将要写入的文本以UTF_8格式进行编码
        Charset utfEncoding = Charset.forName("UTF-8");
        //由于已经确定文本的格式编码为UTF_8，所以直接将payload的第1个字节的第7位设为0
        byte[] textBytes = text.getBytes(utfEncoding);
        int utfBit = 0;
        //定义和初始化状态字节
        char status = (char) (utfBit + langBytes.length);
        //创建存储payload的字节数组
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        //设置状态字节
        data[0] = (byte) status;
        //设置语言编码
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        //设置实际要写入的文本
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length,
                textBytes.length);
        //根据前面设置的payload创建NdefRecord对象
        NdefRecord record = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
                NDEF_TYPE.getBytes(), new byte[] {}, text.getBytes(utfEncoding));
        return record;
    }
}
