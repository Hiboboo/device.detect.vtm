package com.jinkeen.vtm.detect.printer.uniwin;

import static android.content.ContentValues.TAG;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;


public class UtilsTools {
    private static String hexString = "0123456789ABCDEF";
    public static Bitmap convertToBlackWhite(Bitmap bmp) {
        System.out.println(bmp.getConfig());
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        if(width>640){
            width = 640;
        }
//        if (height > 2000){
//            return null;
//        }
        int[] pixels = new int[width * height];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        int[] gray=new int[height*width];
        try{
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int grey = pixels[width * i + j];
                    int red = ((grey & 0x00FF0000 ) >>16);
                    gray[width*i+j]=red;
                }
            }
        }catch (Exception e){
            Log.e(TAG, "PrintBmp:" + e.getMessage());
        }

        int e=0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int g=gray[width*i+j];
                if (g>=128) {
                    pixels[width*i+j]=0xffffffff;
                    e=g-255;

                }else {
                    pixels[width*i+j]=0xff000000;
                    e=g-0;
                }
                if (j<width-1&&i<height-1) {
                    gray[width*i+j+1]+=3*e/8;
                    gray[width*(i+1)+j]+=3*e/8;
                    gray[width*(i+1)+j+1]+=e/4;
                }else if (j==width-1&&i<height-1) {
                    gray[width*(i+1)+j]+=3*e/8;
                }else if (j<width-1&&i==height-1) {
                    gray[width*(i)+j+1]+=e/4;
                }
            }

        }

        Bitmap newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        newBmp.setPixels(pixels, 0, width, 0, 0, width, height);

        Bitmap resizeBmp = ThumbnailUtils.extractThumbnail(newBmp, width, height);
        return resizeBmp;
    }


    protected static void writeWord(FileOutputStream stream, int value) throws IOException {
        byte[] b = new byte[2];
        b[0] = (byte) (value & 0xff);
        b[1] = (byte) (value >> 8 & 0xff);
        stream.write(b);
    }

    protected static void writeDword(FileOutputStream stream, long value) throws IOException {
        byte[] b = new byte[4];
        b[0] = (byte) (value & 0xff);
        b[1] = (byte) (value >> 8 & 0xff);
        b[2] = (byte) (value >> 16 & 0xff);
        b[3] = (byte) (value >> 24 & 0xff);
        stream.write(b);
    }

    protected static void writeLong(FileOutputStream stream, long value) throws IOException {
        byte[] b = new byte[4];
        b[0] = (byte) (value & 0xff);
        b[1] = (byte) (value >> 8 & 0xff);
        b[2] = (byte) (value >> 16 & 0xff);
        b[3] = (byte) (value >> 24 & 0xff);
        stream.write(b);
    }

    public static String readTxt(String path){
        String str = "";
        try {
            File urlFile = new File(path);
            InputStreamReader isr = new InputStreamReader(new FileInputStream(urlFile), "UTF-8");
            BufferedReader br = new BufferedReader(isr);

            String mimeTypeLine = null ;
            while ((mimeTypeLine = br.readLine()) != null) {
                str = str+mimeTypeLine;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  str;
    }

    public static String encodeCN(String data) {
        byte[] bytes;
        try {
            bytes = data.getBytes("gbk");
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (int i = 0; i < bytes.length; i++) {
                sb.append(hexString.charAt((bytes[i] & 0xf0) >> 4));
                sb.append(hexString.charAt((bytes[i] & 0x0f) >> 0));
                // sb.append(" ");
            }
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String encodeStr(String data) {
        String result = "";
        byte[] b;
        try {
            b = data.getBytes("gbk");
            for (int i = 0; i < b.length; i++) {
                result += Integer.toString((b[i] & 0xff) + 0x100, 16)
                        .substring(1);
                // result += " ";
            }
            return result;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean isCN(String data) {
        boolean flag = false;
        String regex = "^[\u4e00-\u9fa5]*$";
//		String regex = "^[һ-��]*$";
        if (data.matches(regex)) {
            flag = true;
        }
        return flag;
    }

    //获取文件后缀名
    public static String getExtensionName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return filename;
    }


    /**
     * 字符串转含0x 16进制
     * @param paramString
     * @return
     */
    private static byte[] hexStr2Bytesnoenter(String paramString) {
        String[] paramStr = paramString.split(" ");
        byte[] arrayOfByte = new byte[paramStr.length];

        for (int j = 0; j < paramStr.length; j++) {
            arrayOfByte[j] = Integer.decode("0x" + paramStr[j]).byteValue();
        }
        return arrayOfByte;
    }

    /// <summary>
    /// byte数组转int数组
    /// </summary>
    /// <param name="src">源byte数组</param>
    /// <param name="offset">起始位置</param>
    /// <returns></returns>
    public static int[] bytesToInt(byte[] src, int offset)
    {
        int[] values=new int[src.length/4];
        for (int i = 0; i < src.length / 4; i++)
        {
            int value = (int)((src[offset] & 0xFF)
                    | ((src[offset + 1] & 0xFF) << 8)
                    | ((src[offset + 2] & 0xFF) << 16)
                    | ((src[offset + 3] & 0xFF) << 24));
            values[i] = value;
            offset += 4;
        }
        return values;
    }

    /**
     * 字节数组转16进制
     * @param bytes 需要转换的byte数组
     * @return  转换后的Hex字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if(hex.length() < 2){
                sb.append(0);
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public static String  unicodeToUtf8 (String s) {
        Log.e("TAG",getEncoding(s));
        try {
            if(getEncoding(s) == "UTF-8"){
                return new String( s.getBytes("GBK") , "UTF-8");
            }else
            if(getEncoding(s) == "GBK"||getEncoding(s) == "GB2312"){
                return new String( s.getBytes("GBK") , "GBK");
            }else
            if(getEncoding(s) == "ISO-8859-1"){
                return new String( s.getBytes("ISO-8859-1") , "UTF-8");
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getEncoding(String str) {
        String encode = "GB2312";
        try {
            if (str.equals(new String(str.getBytes(encode), encode))) {
                String s = encode;
                return s;
            }
        } catch (Exception exception) {
        }
        encode = "ISO-8859-1";
        try {
            if (str.equals(new String(str.getBytes(encode), encode))) {
                String s1 = encode;
                return s1;
            }
        } catch (Exception exception1) {
        }
        encode = "UTF-8";
        try {
            if (str.equals(new String(str.getBytes(encode), encode))) {
                String s2 = encode;
                return s2;
            }
        } catch (Exception exception2) {
        }
        encode = "GBK";
        try {
            if (str.equals(new String(str.getBytes(encode), encode))) {
                String s3 = encode;
                return s3;
            }
        } catch (Exception exception3) {
        }
        return "";
    }




    /**
     * 截取byte[]
     * @param data 被截取数组
     * @param start 起始位置
     * @param length 截取长度
     * @return
     */
    public static byte[] byteSub(byte[] data, int start, int length) {
        byte[] bt = new byte[length];

        if(start + length > data.length) {
            bt = new byte[data.length-start];
        }

        for(int i = 0; i < length &&(i + start) < data.length; i++) {
            bt[i] = data[i + start];
        }
        return bt;
    }

    /**
     * 判断字符串内是不是16进制的值
     * @param str
     * @return
     */
    public static boolean isHexStrValid(String str) {
        String pattern = "^[0-9A-F]+$";
        return Pattern.compile(pattern).matcher(str).matches();
    }



    // -------------------------------------------------------
    // 转hex字符串转字节数组
    static public byte[] hexToByteArr(String inHex)// hex字符串转字节数组
    {
        int hexlen = inHex.length();
        byte[] result;
        if (isOdd(hexlen) == 1) {// 奇数
            hexlen++;
            result = new byte[(hexlen / 2)];
            inHex = "0" + inHex;
        } else {// 偶数
            result = new byte[(hexlen / 2)];
        }
        int j = 0;
        for (int i = 0; i < hexlen; i += 2) {
            result[j] = hexToByte(inHex.substring(i, i + 2));
            j++;
        }
        return result;
    }

    static public byte hexToByte(String inHex)// Hex字符串转byte
    {
        return (byte) Integer.parseInt(inHex, 16);
    }
    // 判断奇数或偶数，位运算，最后一位是1则为奇数，为0是偶数
    static public int isOdd(int num) {
        return num & 0x1;
    }


//    -----------------------------------------------------------------------------------
    /**
     * 将字符串形式表示的十六进制数转换为byte数组
     */
    public static byte[] hexStringToBytes(String hexString) {
        hexString = hexString.toLowerCase();
        String[] hexStrings = hexString.split(" ");
        byte[] bytes = new byte[hexStrings.length];
        for (int i = 0; i < hexStrings.length; i++) {
            char[] hexChars = hexStrings[i].toCharArray();
            bytes[i] = (byte) (charToByte(hexChars[0]) << 4 | charToByte(hexChars[1]));
        }
        return bytes;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789abcdef".indexOf(c);
        // 改成小写 return (byte) "0123456789ABCDEF".indexOf(c);
    }

    // -------------------------------------------------------
    static public String byteArrToHex(byte[] inBytArr)// 字节数组转转hex字符串
    {
        StringBuilder strBuilder = new StringBuilder();
        int j = inBytArr.length;
        for (int i = 0; i < j; i++) {
            strBuilder.append(Byte2Hex(inBytArr[i]));
            strBuilder.append(" ");
        }
        return strBuilder.toString();
    }
    // -------------------------------------------------------
    static public String Byte2Hex(Byte inByte)// 1字节转2个Hex字符
    {
        return String.format("%02x", inByte).toUpperCase();
    }

    /**
     * // 获得编码格式
     * @param head
     * @return
     */
    private static String codetype(byte[] head) {
        String type = "";
        byte[] codehead = new byte[3];
        System.arraycopy(head, 0, codehead, 0, 3);
        if(codehead[0] == -1 && codehead[1] == -2) {
            type = "UTF-16";
        }
        else if(codehead[0] == -2 && codehead[1] == -1) {
            type = "UNICODE";
        }
        else if(codehead[0] == -17 && codehead[1] == -69 && codehead[2] == -65) {
            type = "UTF-8";
        }
        else {
            type = "GB2312";
        }
        return type;
    }

    /**
     * 获取当前时间
     * @return
     */
    public static String data(){
        Date date = new Date();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss ");

        String sim = dateFormat.format(date);
        return sim;
    }
}
