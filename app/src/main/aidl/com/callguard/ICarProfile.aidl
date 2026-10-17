package com.callguard;
interface ICarProfile {
    int readPolicy(String address) = 2;
    boolean writePolicy(String address, int policy) = 3;
    int connectionState(String address) = 4;
    void destroy() = 16777114;
}
