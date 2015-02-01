package com.neuralink.cordova.rfidplugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android_serialport_api.SerialPort;
//import android_serialportfinder_api.SerialPortFinder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.os.Message;
import hdx.pwm.PWMControl;
import android.app.Instrumentation;;


public class RFIDPlugin extends CordovaPlugin {

    private CallbackContext callback = null;
	
	boolean   opened=false;
    EditText editText;
    EditText edtWrite;
    EditText edtRead;
	private static final String TAG = "RFID";
   
    public static final int RFID_CONFIG=0;
    public static final int RFID_AUTODETECT=1;
    public static final int RFID_DECONFIG=2;

    public static final int RFID_CONFIG_RESPONSE = 100;
    public static final int RFID_CARDNUMBER = 101;
    public static final int RFID_DECONFIG_RESPONSE = 102;
    public static final int RFID_AUTODETECT_RESPONSE = 103;
    
	protected SerialPort mSerialPort;
	protected OutputStream mOutputStream;
	private InputStream mInputStream;
	private ReadThread mReadThread;    
	private TTT app;
	Button btnRead;
	Button btnWrite;
	RFID_Operation rfid_operation;
	MyHandler handler;
	
	private final int READ_RESULT = 0;
	private final int WRITE_RESULT=1;
	private final int AUTO_SEEK_RESULT=2;
	
	//mifare error
	public final int OK            =0 ;         //������óɹ�
	public final int NO_TAG_ERR    =1 ;         //����Ч������û�п�
	public final int CRC_ERR       =2 ;         //�ӿ��н��յ��˴����CRCУ���
	public final int EMPTY         =3 ;         //ֵ���
	public final int AUTH_ERR      =4 ;         //������֤
	public final int PARITY_ERR    =5 ;         //�ӿ��н��յ��˴����У��λ
	public final int CODE_ERR      =6 ;         //ͨ�Ŵ���
	public final int SERNR_ERR     =8 ;         //�ڷ���ͻʱ�����˴���Ĵ�����
	public final int KEY_ERR       =9 ;         //֤ʵ�����*****
	public final int NOT_AUTH_ERR  =10;         //��û����֤
	public final int BIT_COUNT_ERR =11;         //�ӿ��н��յ��˴���������λ
	public final int BYTE_COUNT_ERR=12;         //�ӿ��н����˴����������ֽ�
	public final int TRANS_ERR     =14;         //����Transfer�������
	public final int WRITE_ERR     =15;         //����Write�������
	public final int INCR_ERR      =16;         //����Increment�������
	public final int DECR_ERR      =17;         //����Decrment�������
	public final int READ_ERR      =18;         //����Read�������
	public final int COLL_ERR      =24;         //��ͻ��
	public final int ACCESS_TIMEOUT=27;         //���ʳ�ʱ
	public final int QUIT	       =30;         //��һ����������ʱ�����
	public final int COMM_ERR      =31;
			
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

		app = new TTT();
		handler = new MyHandler();
		PWMControl.RfidEnable(1);
		sleep(100);

		if(opened==false)
		{
			open();
		}


		callback = callbackContext;
		//callback.success("SCANNED RFID");
		return true;
    }    



	private class TTT
	{
		//public SerialPortFinder mSerialPortFinder = new SerialPortFinder();
		private SerialPort mSerialPort = null;
	
		public SerialPort getSerialPort() throws SecurityException, IOException, InvalidParameterException {
			if (mSerialPort == null) {
				//String path = "/dev/ttyS1";  //053
				String path = "/dev/ttyS3";  //028
				int baudrate = 9600;
	
				/* Open the serial port */
				//mSerialPort = new SerialPort(new File(path), baudrate, 0);
				mSerialPort = new SerialPort(path, baudrate, 0);
			}
			return mSerialPort;
		}
	
		public void closeSerialPort() {
			if (mSerialPort != null) {
				mSerialPort.close();
				mSerialPort = null;
			}
		}	
	}
	
	private void DisplayError(String resourceId) {
		callback.success(resourceId);
	}    


    boolean open()
    {
    	boolean enable=true;

		try {
			mSerialPort = app.getSerialPort();
			mOutputStream = mSerialPort.getOutputStream();
			mInputStream = mSerialPort.getInputStream();
			PWMControl.RfidEnable(1);

			//Create a receiving thread 
			
			mReadThread = new ReadThread();
			mReadThread.start();
			sleep(100);

			//////SendConfigCmd();
			enable = false;
		} catch (SecurityException e) {
			DisplayError("ERROR SECURITY");
		} catch (IOException e) {
			DisplayError("ERROR UNKNOWN");
		} catch (InvalidParameterException e) {
			DisplayError("ERROR CONFIGURATION");
		}

		rfid_operation = new AutoSeekCard();
		try {
			rfid_operation.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
    }
	private void sleep(int ms) {

		try
		{ 
		  java.lang.Thread.sleep(ms);
		}
		catch (Exception e)
		{
		  e.printStackTrace();
		}
	}    
    boolean close()
    {
    	byte cmd[]={(byte)0x20,(byte)0x00,(byte)0x4f,(byte)0x00,(byte)0xb0,(byte)0x03};
		if (mReadThread != null)
		{
			mReadThread.interrupt();
		}
		try
		{
			Log.d(TAG,"write close cmd");
			mOutputStream.write(cmd);
		} catch (IOException e) {
		}   
		sleep(100);
		app.closeSerialPort();
		mSerialPort = null;
		PWMControl.RfidEnable(0);
		return true;
    }
	
	
	private class ReadThread extends Thread {

		private byte[] response=new byte[1024];
		
		private void porcessPacket(byte buffer[],int size)
		{
			try {
				rfid_operation.OnDateReceived(buffer, size);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		@Override
		public void run() {
			super.run();
			int resp_size=0;
			while(!isInterrupted()) {
				int size;
				Log.d(TAG,"resp_size1 "+resp_size);
				try {
					byte[] buffer = new byte[128];
					if (mInputStream == null) return;
					size = mInputStream.read(buffer);
					int i;
					
					Log.d(TAG,"size "+size);
					for(i=0;i<size;i++)
					{
						Log.d(TAG,"rece "+Byte.toString(buffer[i]));
						response[resp_size]=buffer[i];
						resp_size++;
					}
					
					/*for(i=0;i<resp_size;i++)
					{
						Log.d(TAG,"response "+Byte.toString(response[i]));
					}*/
					
					while(resp_size!=0)
					{
						i=0;
						while(response[i]!=(byte)0x20 && i<resp_size)
						{
							i++;
						}
						if(i==resp_size)
						{
							resp_size = 0;
						}
						else if(i!=0)
						{
							int j;
							for(j=0;i<resp_size;j++,i++)
							{
								response[j]=response[i];
							}
							resp_size -= i-j;
						}
						int packet_len;
						
						if(resp_size>=6 && 
								response[3]+6<=resp_size)
						{
							packet_len =response[3]+6;
							
							if(response[packet_len-1]==3)
							{
								//һ���������ݰ�
								/*byte bcc=0;
								for(i=1;i<packet_len-2;i++)
								{
									bcc ^= response[i];
								}
								bcc ^= 0xff;*/
								byte bcc=CalcBCC(response,1,packet_len-2-1);
								if(bcc == response[packet_len-2])
								{
									int j;
									
									Log.d(TAG,"receive packet");
									porcessPacket(response,packet_len);
									for(j=0,i=packet_len;i<resp_size;j++,i++)
									{
										response[j]=response[i];
									}
									resp_size -= packet_len;	
								}
							}
							else
							{
								int j;
								for(j=0,i=1;i<resp_size;j++,i++)
								{
									response[j]=response[i];
								}
								resp_size -= 1;
							}
						}
						else
						{
							break;
						}
					}
					
				} catch (IOException e) {
					e.printStackTrace();
					Log.d(TAG,"read exception");
					return;
				}
			}
			Log.d(TAG,"quit readthrad");
		}
	}
	private abstract class RFID_Operation{
		protected int nState;
		public void RFID_operate(){
			nState=0;
		}
		abstract void SwitchNextState() throws IOException;
		abstract void start() throws IOException;
		abstract void OnDateReceived(byte buffer[],int size) throws IOException;
	};
	private class AutoSeekCard extends RFID_Operation{

		@Override
		void SwitchNextState() throws IOException {
			nState++;
			byte bcc;
			switch(nState){
			case 0:
				break;
			case 1:	//�Զ�Ѱ��
				sleep(500);
				byte cmd[]={(byte)0x20,(byte)0x00,(byte)0x80,(byte)0x04,(byte)0x07,(byte)0x03,
            			(byte)0x01,(byte)0x00,(byte)0x7e,(byte)0x03};
				bcc = CalcBCC(cmd,1,cmd.length-3);
				cmd[8]=bcc;
				mOutputStream.write(cmd);	
				mOutputStream.flush();
				Log.d(TAG,"auto seek");
				break;
			default:
				break;
			}
		}

		@Override
		void start() throws IOException {
			nState=0;
			byte cmd[]={(byte)0x20,(byte)0x00,(byte)0x52,(byte)0x00,(byte)0xad,(byte)0x03};
			mOutputStream.write(cmd);	
			Log.d(TAG,"tx config init");
		}

		@Override
		void OnDateReceived(byte buffer[],int size)throws IOException {
			int i;
			Message msg = new Message();
			StringBuilder sn=new StringBuilder();
			for(i=0;i<size;i++)
				sn.append(String.format("%02x", buffer[i]));
			Log.d(TAG,"AutoSeekCard receive:"+nState+ " " +sn.toString());
			switch(nState){
			case 0:
			case 1:
				SwitchNextState();
				break;
			default:
				msg.what = AUTO_SEEK_RESULT;
				msg.arg1 = buffer[2];
				msg.obj = buffer;
				msg.arg2 = size;
				handler.sendMessage(msg);				
				break;
			}
		}
	}
	
	private class ReadState extends RFID_Operation{
		public void ReadState(){
			super.RFID_operate();
		}
		public void SwitchNextState() throws IOException
		{
			byte bcc;
			nState++;
			switch(nState){
			case 0:
				break;
			case 1:	//ȡ���Զ�Ѱ��
				byte cmd1[]={(byte)0x20,(byte)0x01,(byte)0x4f,(byte)0x00,(byte)0xb0,(byte)0x03};
				bcc = CalcBCC(cmd1,1,cmd1.length-3);
				cmd1[cmd1.length-2]=bcc;
				mOutputStream.write(cmd1);
				mOutputStream.flush();
				Log.d(TAG,"read cdm1;");
				break;
			case 2:	//TX_Get_CardSnr ѡ��
				byte cmd2[]={(byte)0x20,(byte)0x01,(byte)0x10,(byte)0x01,(byte)0x00,(byte)0xee,(byte)0x03};
				bcc = CalcBCC(cmd2,1,cmd2.length-3);
				cmd2[cmd2.length-2]=bcc;
				mOutputStream.write(cmd2);
				mOutputStream.flush();
				Log.d(TAG,"read cdm2");
				break;
			case 3:	//load_key
				byte cmd3[]={(byte)0x20,(byte)0x01,(byte)0x4c,(byte)0x08,(byte)0x00,(byte)0x02,
						(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xff,
						(byte)0x00,(byte)0x03};
				bcc = CalcBCC(cmd3,1,cmd3.length-3);
				cmd3[12]=bcc;
				mOutputStream.write(cmd3);
				mOutputStream.flush();
				Log.d(TAG,"read cdm3");
				break;
				
			case 4:	//auth2
				byte cmd4[]={(byte)0x20,(byte)0x01,(byte)0x72,(byte)0x03,(byte)0x00,(byte)0x00,
						(byte)0x00,(byte)0x8e,(byte)0x03};
				bcc = CalcBCC(cmd4,1,cmd4.length-3);
				cmd4[cmd4.length-2]=bcc;
				mOutputStream.write(cmd4);
				mOutputStream.flush();
				Log.d(TAG,"read cdm4");
				break;
			case 5:	//����
				byte cmd5[]={(byte)0x20,(byte)0x01,(byte)0x46,(byte)0x01,(byte)0x02,
						(byte)0xba,(byte)0x03};
				bcc = CalcBCC(cmd5,1,cmd5.length-3);
				cmd5[cmd5.length-2]=bcc;
				mOutputStream.write(cmd5);
				mOutputStream.flush();
				Log.d(TAG,"read cdm5");
				break;
			}
		}
		
		public void start() throws IOException{
//			byte cmd2[]={(byte)0x20,(byte)0x00,(byte)0x4e,(byte)0x01,(byte)0x01,(byte)0xb0,(byte)0x03};
//			mOutputStream.write(cmd2);
//			mOutputStream.flush();
			nState = 0;
			SwitchNextState();
		}
		
		public void OnDateReceived(byte buffer[],int size) throws IOException{
			Message msg = new Message();
			int i;
			StringBuilder sn=new StringBuilder();
			for(i=0;i<size;i++)
				sn.append(String.format("%02x", buffer[i]));
			Log.d(TAG,"receive:"+nState+ " " +sn.toString());
			if(buffer[1]!=1){
				Log.d(TAG,"not read response");
				return;
			}
			if(buffer[2]!=OK){
				Log.d(TAG,"cmd nState error"+ buffer[2]);
				msg.what = READ_RESULT;
				msg.arg1 = buffer[2];
				handler.sendMessage(msg);
				return;
			}			
			switch(nState){
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
				SwitchNextState();
				break;
			case 5:
			defalut:
				msg.what = READ_RESULT;
				msg.arg1 = buffer[2];
				msg.obj = buffer;
				msg.arg2 = size;
				handler.sendMessage(msg);
				break;
			}
		}
	}
	
	private class WriteState extends RFID_Operation{
		
		@Override
		void SwitchNextState() throws IOException {
			byte bcc;
			nState++;
			switch(nState){
			case 0:
				break;
			case 1:	//ȡ���Զ�Ѱ��
				byte cmd1[]={(byte)0x20,(byte)0x02,(byte)0x4f,(byte)0x00,(byte)0xb0,(byte)0x03};
				bcc = CalcBCC(cmd1,1,cmd1.length-3);
				cmd1[cmd1.length-2]=bcc;
				mOutputStream.write(cmd1);
				mOutputStream.flush();
				Log.d(TAG,"read cdm1;");
				break;
			case 2:	//TX_Get_CardSnr ѡ��
				byte cmd2[]={(byte)0x20,(byte)0x02,(byte)0x10,(byte)0x01,(byte)0x00,(byte)0xee,(byte)0x03};
				bcc = CalcBCC(cmd2,1,cmd2.length-3);
				cmd2[cmd2.length-2]=bcc;				
				mOutputStream.write(cmd2);
				mOutputStream.flush();
				Log.d(TAG,"read cdm2");
				break;
			case 3:	//load_key
				byte cmd3[]={(byte)0x20,(byte)0x02,(byte)0x4c,(byte)0x08,(byte)0x00,(byte)0x02,
						(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xff,
						(byte)0x00,(byte)0x03};
				bcc = CalcBCC(cmd3,1,cmd3.length-3);
				cmd3[12]=bcc;
				mOutputStream.write(cmd3);
				mOutputStream.flush();
				Log.d(TAG,"read cdm3");
				break;
				
			case 4:	//auth2
				byte cmd4[]={(byte)0x20,(byte)0x02,(byte)0x72,(byte)0x03,(byte)0x00,(byte)0x00,
						(byte)0x00,(byte)0x8e,(byte)0x03};
				bcc = CalcBCC(cmd4,1,cmd4.length-3);
				cmd4[cmd4.length-2]=bcc;
				mOutputStream.write(cmd4);
				mOutputStream.flush();
				Log.d(TAG,"read cdm4");
				break;
			case 5:	//д��
				byte[] cmd5=new byte[23];
				cmd5[0]=0x20;
				cmd5[1]=0x02;
				cmd5[2]=0x47;
				cmd5[3]=0x11;
				cmd5[4]=2;//bank
				cmd5[22]=0x03;
				String sWrite=edtWrite.getText().toString();
				byte[] data=getByteAry(sWrite);
				System.arraycopy(data, 0, cmd5, 5, data.length);

				bcc = CalcBCC(cmd5,1,cmd5.length-3);
				cmd5[cmd5.length-2]=bcc;
				mOutputStream.write(cmd5);
				mOutputStream.flush();
				Log.d(TAG,"read cdm5");
				break;
			}
		}

		public void start() throws IOException{
			/*byte cmd2[]={(byte)0x20,(byte)0x00,(byte)0x45,(byte)0x00,(byte)0xba,(byte)0x03};
			mOutputStream.write(cmd2);*/
			SwitchNextState();
		}

		@Override
		void OnDateReceived(byte[] buffer, int size) throws IOException {
			Message msg = new Message();
			int i;
			StringBuilder sn=new StringBuilder();
			for(i=0;i<size;i++)
				sn.append(String.format("%02x", buffer[i]));
			Log.d(TAG,"write receive:"+nState+ " " +sn.toString());
			if(buffer[1]!=2){
				Log.d(TAG,"not write response");
				return;
			}
			if(buffer[2]!=OK){
				Log.d(TAG,"cmd nState error"+ buffer[2]);
				msg.what = WRITE_RESULT;
				msg.arg1 = buffer[2];
				handler.sendMessage(msg);
				return;
			}

			switch(nState){
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
				SwitchNextState();
				break;
			case 5:
				msg.what = WRITE_RESULT;
				msg.arg1 = buffer[2];
				handler.sendMessage(msg);
				break;
			}
		}			
	}
	private String bytearraytoString(byte[]data,int offset,int size){
		StringBuilder sn=new StringBuilder();
		int i;
		for(i=offset;i<size+offset && i<data.length;i++)
		{
			sn.append(String.format("%02x", data[i]));
		}	
		return sn.toString();
	}
	private class MyHandler extends Handler{
		public void handleMessage(Message msg) {
			switch(msg.what){
			case READ_RESULT:
				if(msg.arg1!=OK)
					callback.error("READ ERROR");
					//Toast.makeText(getApplicationContext(), "read error ",Toast.LENGTH_SHORT).show();
				else{
					//edtRead.setText(bytearraytoString((byte[])msg.obj,4,16));
					callback.success(bytearraytoString((byte[])msg.obj,4,16));
				}
				break;
			case WRITE_RESULT:
				callback.success("WRITE RESULT");
				//Toast.makeText(getApplicationContext(), "write "+ (msg.arg1==OK?"ok":"error"),Toast.LENGTH_SHORT).show();			
				break;
			case AUTO_SEEK_RESULT:
				if(msg.arg1==OK){
					byte tmp[] = (byte[])msg.obj;
					//editText.setText(bytearraytoString((byte[])msg.obj,9,tmp[8]));
					callback.success(bytearraytoString((byte[])msg.obj,9,tmp[8]));
				}
				break;
				
			}
		}
	}  
	
	private byte[] getByteAry(String str)
	{
		for(int i=str.length();i<32;i++)
			str=str+"0";
		return hexStringToBytes(str);
	}
	
	
	private byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}	
	private byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}
	private byte CalcBCC(byte[] data,int offset,int size){
		byte bcc=0;
		int i;
		for(i=offset;i<size+offset;i++)
		{
			bcc ^= data[i];
		}
		bcc ^= 0xff;
		return bcc;
	}
}
