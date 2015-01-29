package hdx.pwm;

import android.util.Log;

public class PWMControl{
    private static final String TAG = "PWMControl";


    public static int EnableBuzze(int enable)
    {
         return buzzerControl(enable);
    }
    
    public static int SetCameraBacklightness(int br)
    {
        return cameraBacklightControl(br);
    }
    public static int RfidEnable(int on)
    {
        return SetRfidPower(on);
    }
    public static int PrinterEnable(int on)
    {
        return SetPrinterPower(on);
    }

    private native static int buzzerControl(int enable);
    private native static int cameraBacklightControl(int br);
    private native static int SetRfidPower(int enable);
    private native static int SetPrinterPower(int enable);
    static {
    	System.loadLibrary("pwmV2");
    }
}
