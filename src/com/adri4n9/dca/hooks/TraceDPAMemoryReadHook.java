package com.adri4n9.dca.hooks;

import com.adri4n9.dca.emulators.AndroidEmulator64Tracer;
import com.adri4n9.dca.emulators.AndroidEmulatorTracer;
import com.alibaba.fastjson.util.IOUtils;
import com.github.unidbg.Emulator;
import com.github.unidbg.TraceMemoryHook;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.BackendException;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.pointer.UnidbgPointer;
import org.apache.commons.codec.binary.Hex;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


/**
 * custom hook class use to trace memory Read instructions used to generate a side-channel marvel daredevil compatible
 * trace files
 */
public class TraceDPAMemoryReadHook extends TraceMemoryHook {

    private final boolean read;
    private final DateFormat dateFormat = new SimpleDateFormat("[HH:mm:ss SSS]");
    private PrintStream redirect;

    public void setRedirect(PrintStream redirect) {
        this.redirect = redirect;
    }

    public void stopTrace() {
        detach();
        IOUtils.close(redirect);
        redirect = null;
    }
    public TraceDPAMemoryReadHook(boolean read) {
        super(read);
        this.read=read;
    }

    /**
     * this function is called when a Memory read instruction is executed by the emulator
     * @param backend backend environment
     * @param address instruction address
     * @param size instruction size
     * @param user process calling the emulator
     */
    @Override
    public void hook(Backend backend, long address, int size, Object user) {
        if (!read) {
            return;
        }

        try {
            byte[] data = size == 0 ? new byte[0] : backend.mem_read(address, size);
            String value;
            if (data.length == 4) {
                value = "0x" + Long.toHexString(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffffL);
            } else if (data.length == 8) {
                value = "0x" + Long.toHexString(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getLong());
            } else {
                value = "0x" + Hex.encodeHexString(data);
            }
            Emulator<?> emulator = (Emulator<?>) user;

            //AndroidEmulatorTracer.bigcount++;
            printMsg("R", emulator, address, size, value);
        } catch (BackendException e) {
            throw new IllegalStateException(e);
        }

    }

    /**
     * this function defines the text to be printed into the trace files
     * this function also stores the addresses in the buffer containing the binary trace
     * @param type either read or write
     * @param emulator android emulator
     * @param address address of the read /write data
     * @param size size of the read/written data
     * @param value value read from memory
     */
    private void printMsg(String type, Emulator<?> emulator, long address, int size, String value) {
        RegisterContext context = emulator.getContext();
        UnidbgPointer pc = context.getPCPointer();
        ;

        UnidbgPointer lr = context.getLRPointer();
        PrintStream out = System.out;
        if (redirect != null) {
            out = redirect;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("[M] ");
        builder.append("EXEC_ID: ").append(AndroidEmulatorTracer.blockCount);
        builder.append(" INS_ADDRESS: ").append(Long.toHexString(UnidbgPointer.nativeValue(pc)));
        builder.append(" START_ADDRESS: ").append(Long.toHexString(address));
        builder.append(" LENGTH: ").append(size);
        builder.append(" MODE: ").append(type);
        builder.append(" DATA: ").append(value);
        //builder.append(type).append(String.format("%10d",AndroidEmulatorTracer.bigcount));
        //builder.append(String.format("%16s","0x"+Long.toHexString(address)));
        //builder.append(String.format("%16s","0x"+Long.toHexString(UnidbgPointer.nativeValue(pc))));
        out.println(builder.toString());
        if (size== 1) {
            if(emulator.is32Bit()){
                ((AndroidEmulatorTracer)emulator).addressTrace.write((byte) (address & 0xFF));
                ((AndroidEmulatorTracer)emulator).dataTrace.write((byte) (Long.parseLong(value.substring(2),16) & 0xFF));
            } else {
                ((AndroidEmulator64Tracer)emulator).addressTrace.write((byte) (address & 0xFF));
                ((AndroidEmulator64Tracer)emulator).dataTrace.write((byte) (Long.parseLong(value.substring(2),16) & 0xFF));
            }

        }
    }
}
