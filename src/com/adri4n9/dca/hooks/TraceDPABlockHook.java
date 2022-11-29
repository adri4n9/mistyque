package com.adri4n9.dca.hooks;

import com.adri4n9.dca.emulators.AndroidEmulatorTracer;
import com.alibaba.fastjson.util.IOUtils;

import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.UnHook;
import com.github.unidbg.arm.backend.unicorn.Unicorn;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.listener.TraceReadListener;
import com.github.unidbg.arm.backend.BlockHook;
import com.github.unidbg.pointer.UnidbgPointer;

import java.io.PrintStream;

/**
 * custom hook class use to trace block instructions used to generate a side-channel marvel daredevil compatible
 * trace files
 */
public class TraceDPABlockHook implements BlockHook {


    // print stream to direct the output of the trace
    private PrintStream redirect;
    TraceReadListener traceReadListener;


    public void hook(Unicorn u, long address, int size, Object user_data)
    {
        //System.out.printf(">>> Tracing basic block at 0x%x, block size = 0x%x\n", address, size);
    }


    public void setRedirect(PrintStream redirect) {
        this.redirect = redirect;
    }

    private UnHook unHook;


    public void onAttach(UnHook unHook) {
        if (this.unHook != null) {
            throw new IllegalStateException();
        }
        this.unHook = unHook;
    }


    public void detach() {
        if (unHook != null) {
            unHook.unhook();
            unHook = null;
        }
    }

    public void stopTrace() {
        detach();
        IOUtils.close(redirect);
        redirect = null;
    }

    /**
     * this function is called when a block instruction is executd by the emulator
     * @param backend backend environment
     * @param address instruction address
     * @param size instruction size
     * @param user process calling the emulator
     */
    @Override
    public void hookBlock(Backend backend, long address, int size, Object user) {

        //in case there is no redirection we will print to the console System.out
        PrintStream out = System.out;

        if (redirect != null) {
            out = redirect;
        }

        Emulator<?> emulator = (Emulator<?>) user;
        RegisterContext context = emulator.getContext();
        UnidbgPointer pc = context.getPCPointer();
        //System.out.printf(">>> Tracing basic block at 0x%x, block size = 0x%x\n", address, size);
        StringBuilder builder = new StringBuilder();
        builder.append("[B]").append(" EXEC_ID: " ).append(AndroidEmulatorTracer.blockCount);
        builder.append(" START_ADDRESS: ").append(address);
        builder.append(" END_ADDRESS: ").append(address+size);

        //builder.append(String.format("%16s","0x"+Long.toHexString(address)));

        //builder.append(" loc_").append(Long.toHexString(UnidbgPointer.nativeValue(pc))).append(":  // size=").append(size);
        AndroidEmulatorTracer.blockCount++; // incremeanting the block so we know how many times we branched
        out.println(builder.toString()); // writing into the stream
    }
}
