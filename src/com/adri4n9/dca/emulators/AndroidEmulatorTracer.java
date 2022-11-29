package com.adri4n9.dca.emulators;

import capstone.api.Instruction;
import com.adri4n9.dca.hooks.TraceDPABlockHook;
import com.adri4n9.dca.hooks.TraceDPAMemoryReadHook;
import com.adri4n9.dca.hooks.TraceDPAMemoryWriteHook;
import com.adri4n9.dca.hooks.traceDPAAssemblyCodeDumper;
import com.adri4n9.dca.utils.Format;
import com.adri4n9.dca.utils.PrintHelper;
import com.github.unidbg.TraceHook;
import com.github.unidbg.TraceMemoryHook;
import com.github.unidbg.arm.ARM;
import com.github.unidbg.arm.InstructionVisitor;
import com.github.unidbg.arm.backend.BackendFactory;
import com.github.unidbg.arm.backend.BlockHook;
import com.github.unidbg.arm.backend.ReadHook;
import com.github.unidbg.arm.backend.WriteHook;
import com.github.unidbg.linux.android.AndroidARMEmulator;
import com.github.unidbg.listener.TraceCodeListener;
import com.github.unidbg.listener.TraceReadListener;
import com.github.unidbg.listener.TraceWriteListener;

import java.io.*;
import java.util.Collection;

/**
 * 32 bit Android DCA  emulator wrapping AndroidEmulatorTracer class defined by unidgb
 * this class offers custom hooks to trace 32 bit binaries
 * the output files are witten in side-channel marvels daredevil compatible format.
 */
public class AndroidEmulatorTracer extends AndroidARMEmulator  implements DCATracer {

    public static long bigCount =0;
    public static long blockCount =0;




    protected AndroidEmulatorTracer(String processName, File rootDir, Collection<BackendFactory> backendFactories) {
        super(processName, rootDir, backendFactories);
    }

    /**
     * defines MemoryRead hook use to trace read operations from the  target binary
     * to  generate traces of the entire memory space set begin == 1 and end == 0
     * @param begin starting address for recording
     * @param end end address for recording end > begin
     * @param listener not used
     * @return TraceHook reference to the hook
     */
    @Override
    public TraceHook traceRead(long begin, long end, TraceReadListener listener) {
        TraceMemoryHook hook = new TraceDPAMemoryReadHook(true);

        backend.hook_add_new((ReadHook) hook, begin, end, this);
        return hook;
    }

    /**
     * defines writeRead hook use to trace memory write operations from the  target binary
     * to  generate traces of the entire memory space set begin == 1 and end == 0
     * @param begin starting address for recording
     * @param end end address for recording end > begin
     * @param listener not used
     * @return TraceHook reference to the hook
     */
    @Override
    public TraceHook traceWrite(long begin, long end, TraceWriteListener listener) {
        //System.out.println("trace write from custom android arm");
        TraceMemoryHook hook = new TraceDPAMemoryWriteHook(false);
        backend.hook_add_new((WriteHook) hook, begin, end, this);
        return hook;
    }

    /**
     * defines traceCode hook use to trace instructions from the  target binary
     * to  generate traces of the entire code set begin == 1 and end == 0
     * @param begin initial PC address for recording
     * @param end end PC for recording end > begin
     * @param listener
     * @return TraceHook reference to the hook
     */
    public TraceHook traceCode(long begin, long end, TraceCodeListener listener) {
        //System.out.println("trace code call");
        traceDPAAssemblyCodeDumper hook = new traceDPAAssemblyCodeDumper(this, begin, end, listener);
        backend.hook_add_new(hook, begin, end, this);
        return hook;
    }

    /**
     * defines blockTrace hook use to block instructions from the  target binary
     * a new block instruction is executed when branching to another code block e.g. BL
     * to  generate traces of the entire code set begin == 1 and end == 0
     * @param begin initial PC address for recording
     * @param end end PC for recording end > begin
     * @param listener not used
     * @return BlockHook reference to the hook
     */
    public BlockHook traceBlock(long begin, long end, TraceWriteListener listener) {
        //System.out.println("tracewrite from customandroid arm");
        BlockHook hook =  new TraceDPABlockHook();
        backend.hook_add_new((BlockHook) hook, begin, end, this);
        return hook;
    }




    @Override
    public Instruction[] printAssemble(PrintStream out, long address, int size, int maxLengthLibraryName, InstructionVisitor visitor) {
        Instruction[] insns = disassemble(address, size, 0);
        PrintHelper.printAssemble(this,out, insns, address, ARM.isThumb(backend), maxLengthLibraryName, visitor);
        return insns;
    }



    TraceHook hookRead, hookWrite, hookCode= null;
    BlockHook traceDPABlockHook=null;

    /**
     *  sets Read, Write and code traces for the defined range
     *  begin < end
     *  if a trace of the entire space is needed set
     *  begin ==1
     *  end == 0
     * @param begin address to start tracing
     * @param end address to end tracing
     */
    @Override
    public void setDCARange(long begin, long end) {
        hookRead  = this.traceRead(begin,end,null);
        hookWrite = this.traceWrite(begin,end,null);
        hookCode  = this.traceCode(begin,end,null);
        traceDPABlockHook = this.traceBlock(begin,end,null);
    }

    /**
     * writeTrace function is used to write the binary DCA trace files in the path given as parameter
     * this function must be called after each  emulated function execution.
     * input and output parameters will be used by daredevil for the DCA attack implementation
     * @param path path to store the trace file
     * @param traceNumber trace number preferably a counter to determine the current trace
     * @param input  input used forthe encryption or decryption function
     * @param output output of the encryption or decryption function
     */
    @Override
    public void writeTrace(String path, int traceNumber, byte [] input, byte []output){
        StringBuilder fileNameBuilder= new StringBuilder();
        fileNameBuilder.append(path);
        fileNameBuilder.append(String.format("trace_mem_addr1_rw1_%04d_",traceNumber))
                .append(Format.bytesToHex(input));
        fileNameBuilder.append("_").append(Format.bytesToHex(output)).append(".bin");

        StringBuilder dataFileNameBuilder= new StringBuilder();
        dataFileNameBuilder.append(path);
        dataFileNameBuilder.append(String.format("trace_mem_data_rw1_%04d_",traceNumber))
                .append(Format.bytesToHex(input));
        dataFileNameBuilder.append("_").append(Format.bytesToHex(output)).append(".bin");
        System.out.println(dataFileNameBuilder);

        StringBuilder stackFileNameBuilder= new StringBuilder();
        stackFileNameBuilder.append(path);
        stackFileNameBuilder.append(String.format("trace_stack_w1_%04d_",traceNumber))
                .append(Format.bytesToHex(input));
        stackFileNameBuilder.append("_").append(Format.bytesToHex(output)).append(".bin");

        try {
            DCATracer.writeToFile(fileNameBuilder.toString(),this.addressTrace);
            DCATracer.writeToFile(dataFileNameBuilder.toString(),this.dataTrace);
            DCATracer.writeToFile(stackFileNameBuilder.toString(),this.stackTrace);

            this.addressTrace.flush();
            this.dataTrace.flush();
            this.stackTrace.flush();
            this.addressTrace.reset();
            this.dataTrace.reset();
            this.stackTrace.reset();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        hookRead.stopTrace();
        hookWrite.stopTrace();
        hookCode.stopTrace();
    }

    /**
     * this function is used to define a human-readable file to output the current trace.
     * call this function before running the emulated function
     * if this function is not called the content of the trace is output to the console.
     * the file is overwritten everytime a new trace is generated.
     * if multiple file are to be generated choose a different filename
     * @param file name of the file to write the trace
     */
    public void setHumanReadFile(String file){
        try {
            File filename= new File (file);
            filename.createNewFile();
            PrintStream printStream=  new PrintStream(new FileOutputStream(filename),false);
            hookRead.setRedirect(printStream);
            hookWrite.setRedirect(printStream);
            hookCode.setRedirect(printStream);
            ((TraceDPABlockHook)traceDPABlockHook).setRedirect(printStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
