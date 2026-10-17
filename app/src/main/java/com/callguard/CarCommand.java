package com.callguard;

import android.os.Looper;

/** Short-lived, shell-owned entry point loaded from the installed APK. No exported IPC server. */
public final class CarCommand {
    public static void main(String[] args) {
        if(android.os.Process.myUid()!=2000&&android.os.Process.myUid()!=0){System.exit(1);return;}
        Looper.prepareMainLooper();
        new Thread(()->{
            try {
                CarCommandProtocol.validate(args);
                CarProfileService service=new CarProfileService();
                int result;
                switch(args[0]) {
                    case "probe": result=android.os.Process.myUid();break;
                    case "read": result=service.readPolicy(args[1]);break;
                    case "state": result=service.connectionState(args[1]);break;
                    case "write": result=service.writePolicy(args[1],Integer.parseInt(args[2]))?1:0;break;
                    default: throw new IllegalArgumentException();
                }
                System.out.println("CGCAR1:"+result);System.out.flush();System.exit(0);
            } catch(Exception ex){System.out.println("CGCAR1:ERROR");System.out.flush();System.exit(1);}
        },"CallGuard-car-command").start();
        Looper.loop();
    }
    private CarCommand(){}
}
