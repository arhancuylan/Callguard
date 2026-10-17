package com.callguard;

/** Notification reply supports a discovered port, or explicit "port code" if discovery is unavailable. */
final class PairingReply {
    final int port;final String code;
    private PairingReply(int port,String code){this.port=port;this.code=code;}
    static PairingReply parse(CharSequence input,int discoveredPort){
        if(input==null||input.length()>32)throw new IllegalArgumentException();
        String[] parts=input.toString().trim().split("\\s+");
        int port;String code;
        if(parts.length==1){port=discoveredPort;code=parts[0];}
        else if(parts.length==2){port=CarCommandProtocol.port(parts[0]);code=parts[1];}
        else throw new IllegalArgumentException();
        if(port<1||port>65535||!code.matches("[0-9]{6}"))throw new IllegalArgumentException();
        return new PairingReply(port,code);
    }
}
