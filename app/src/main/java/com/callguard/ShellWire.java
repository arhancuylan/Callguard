package com.callguard;

/** No arbitrary command, path, Intent, Binder forwarding or network endpoint in this protocol. */
final class ShellWire {
    static final String PACKAGE="com.callguard.mobile",AUTHORITY=PACKAGE+".shell";
    static final String DESCRIPTOR="com.callguard.HfpShell.v1",ATTACH="attach-v1",BINDER="hfp-binder";
    static final int PROBE=1,READ=2,STATE=3,WRITE=4,STOP=5;
    private ShellWire(){}
}
