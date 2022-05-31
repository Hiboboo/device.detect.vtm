package com.jinkeen.vtm.detect.printer.uniwin;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;

public class UsbUtil {
    static UsbDevice mUsbDevice = null;
    private static UsbUtil usbUtil;
    public static UsbUtil getInstance(Context context){
        if (usbUtil == null){
            synchronized (UsbUtil.class){
                usbUtil = new UsbUtil(context);
            }
        }
        return usbUtil;
    }

    public UsbUtil(Context context){

    }
    /**
     * 获取usb权限
     */
    public static int usbDriverCheck(Context context, UsbDriver usbDriver) {
        int iResult = -1;
        try {

            if (!usbDriver.isUsbPermission()) {
                UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
                mUsbDevice = null;
                Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
                while (deviceIterator.hasNext()) {
                    UsbDevice device = deviceIterator.next();
                    Log.e("UsbUtil","productId-->"+device.getProductId()+"");
                    Log.e("UsbUtil","vendorId-->"+device.getVendorId()+"");
                    if ((device.getProductId() == 8211 && device.getVendorId() == 1305)
                            || (device.getProductId() == 8213 && device.getVendorId() == 1305)) {
                        mUsbDevice = device;
                    }
                }
                if (mUsbDevice != null) {
                    iResult = 1;
                    if (usbDriver.usbAttached(mUsbDevice)) {
                        if (usbDriver.openUsbDevice(mUsbDevice))
                            iResult = 0;
                    }
                }
            } else {
                if (!usbDriver.isConnected()) {
                    if (usbDriver.openUsbDevice(mUsbDevice))
                        iResult = 0;
                } else {
                    iResult = 0;
                }
            }
        } catch (Exception e) {

        }

        return iResult;
    }
}
