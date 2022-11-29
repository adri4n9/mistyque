package com.adri4n9.dca.hooks;

import com.adri4n9.dca.emulators.AndroidEmulator64Tracer;
import com.adri4n9.dca.emulators.AndroidEmulatorTracer;
import com.alibaba.fastjson.util.IOUtils;
import com.github.unidbg.Emulator;
import com.github.unidbg.TraceMemoryHook;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.BackendException;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import org.apache.commons.codec.binary.Hex;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * custom hook class use to trace memory Read instructions used to generate a side-channel marvel daredevil compatible
 * trace files
 */
public class TraceDPAMemoryWriteHook extends TraceMemoryHook {

    private final boolean read;
        private PrintStream redirect;

    public void setRedirect(PrintStream redirect) {
        this.redirect = redirect;
    }
    public TraceDPAMemoryWriteHook(boolean read) {
        super(read);
        this.read=read;
    }

    public void stopTrace() {
        detach();
        IOUtils.close(redirect);
        redirect = null;
    }

    /**
     * this function is called when a Memory write instruction is executed by the emulator
     * @param backend backend environment
     * @param address instruction address
     * @param size instruction size
     * @param readvalue value of data read
     * @param user process calling the emulator
     */
    public void hook(Backend backend, long address, int size, long readvalue, Object user) {

        if (read) {
            return;
        }
        //hook( backend,  address,  size,  user);
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

            //AndroidEmulatorTracer.bigcount++;;
            printMsg("W", emulator, address, size, value);
        } catch (BackendException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * this function is called when a Memory write instruction is executed by the emulator
     * @param backend backend environment
     * @param address instruction address
     * @param size instruction size
     * @param user process calling the emulator
     */
    @Override
    public void hook(Backend backend, long address, int size, Object user) {
        if (read) {
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

            AndroidEmulatorTracer.bigCount++;;
            printMsg("W", emulator, address, size, value);
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

        /*builder.append(type).append(String.format("%10d",AndroidEmulatorTracer.bigcount))
                .append(String.format("%16s","0x"+Long.toHexString(UnidbgPointer.nativeValue(pc))))
                .append("                                                   ")
                .append(String.format("%18s","0x"+Long.toHexString(address)))
                .append(" size=").append(String.format("%2d",size))
                .append(" value=").append(String.format("%18s",value));*/
        out.println(builder.toString());
        Memory memory = emulator.getMemory();
        if (size== 1) {
            if (emulator.is64Bit()){
                ((AndroidEmulator64Tracer) emulator).addressTrace.write((byte) (address & 0xFF));
                ((AndroidEmulator64Tracer) emulator).dataTrace.write((byte) (Long.parseLong(value.substring(2), 16) & 0xFF));


                if (address >= memory.getStackBase()&&address<=(memory.getStackBase()+memory.getStackSize())){
                    ((AndroidEmulator64Tracer) emulator).stackTrace.write((byte) (Long.parseLong(value.substring(2), 16) & 0xFF));
                }

            } else {

                ((AndroidEmulatorTracer) emulator).addressTrace.write((byte) (address & 0xFF));
                ((AndroidEmulatorTracer) emulator).dataTrace.write((byte) (Long.parseLong(value.substring(2), 16) & 0xFF));
                if (address >= memory.getStackBase()&&address<=(memory.getStackBase()+memory.getStackSize())){
                    ((AndroidEmulatorTracer) emulator).stackTrace.write((byte) (Long.parseLong(value.substring(2), 16) & 0xFF));
                }
            }
        }
    }
}
