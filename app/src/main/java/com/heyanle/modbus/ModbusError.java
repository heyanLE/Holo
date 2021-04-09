package com.heyanle.modbus;

import android.text.TextUtils;

public class ModbusError extends Exception {

    /**
     * 常见的Modbus通讯错误
     */
    public enum ModbusErrorType {
        ModbusError,
        ModbusFunctionNotSupportedError,
        ModbusDuplicatedKeyError,
        ModbusMissingKeyError,
        ModbusInvalidBlockError,
        ModbusInvalidArgumentError,
        ModbusOverlapBlockError,
        ModbusOutOfBlockError,
        ModbusInvalidResponseError,
        ModbusInvalidRequestError,
        ModbusTimeoutError
    }
    private int code;

    public ModbusError(int code, String message) {
        super(!TextUtils.isEmpty(message) ? message : "Modbus Error: Exception code = " + code);
        this.code = code;
    }

    public ModbusError(int code) {
        this(code, null);
    }

    public ModbusError(ModbusErrorType type, String message) {
        super(type.name() + ": " + message);
    }

    public ModbusError(String message) {
        super(message);
    }

    public int getCode() {
        return this.code;
    }
}