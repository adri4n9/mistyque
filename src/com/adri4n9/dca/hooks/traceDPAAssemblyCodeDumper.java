package com.adri4n9.dca.hooks;

import capstone.Arm64_const;
import capstone.Arm_const;
import capstone.api.Instruction;
import capstone.api.RegsAccess;
import com.adri4n9.dca.emulators.AndroidEmulatorTracer;
import com.alibaba.fastjson.util.IOUtils;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.TraceHook;
import com.github.unidbg.arm.Cpsr;
import com.github.unidbg.arm.InstructionVisitor;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.BackendException;
import com.github.unidbg.arm.backend.CodeHook;
import com.github.unidbg.arm.backend.UnHook;
import com.github.unidbg.listener.TraceCodeListener;
import com.github.unidbg.memory.Memory;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Locale;

/**
 * custom hook to trace instruction calls and store them in a side-channel marvel daredevil usable file
 */
public class traceDPAAssemblyCodeDumper implements CodeHook, TraceHook {

    private final Emulator<?> emulator;


    /**
     * contructor for the code hook
     * @param emulator android emulator
     * @param begin star address for hooking
     * @param end end address for hooking
     * @param listener custom class to be call if the hook is called
     */
    public traceDPAAssemblyCodeDumper(Emulator<?> emulator, long begin, long end, TraceCodeListener listener) {
        super();

        this.emulator = emulator;
        this.traceBegin = begin;
        this.traceEnd = end;
        this.listener = listener;

        Memory memory = emulator.getMemory();
        if (begin > end) {
            maxLengthLibraryName = memory.getMaxLengthLibraryName().length();
        } else {
            int value = 0;
            for (Module module : memory.getLoadedModules()) {
                long min = Math.max(begin, module.base);
                long max = Math.min(end, module.base + module.size);
                if (min < max) {
                    int length = module.name.length();
                    if (length > value) {
                        value = length;
                    }
                }
            }
            maxLengthLibraryName = value;
        }
    }

    private final long traceBegin, traceEnd;
    private final TraceCodeListener listener;
    private final int maxLengthLibraryName;

    private UnHook unHook;

    @Override
    public void onAttach(UnHook unHook) {
        if (this.unHook != null) {
            throw new IllegalStateException();
        }
        this.unHook = unHook;
    }

    @Override
    public void detach() {
        if (unHook != null) {
            unHook.unhook();
            unHook = null;
        }
    }

    @Override
    public void stopTrace() {
        detach();
        IOUtils.close(redirect);
        redirect = null;
    }

    private boolean canTrace(long address) {
        return (traceBegin > traceEnd || (address >= traceBegin && address <= traceEnd));
    }

    private PrintStream redirect;

    @Override
    /**
     * defines the location to print the human-readable trace is the stream is a file it will be written to the file
     */
    public void setRedirect(PrintStream redirect) {
        this.redirect = redirect;
    }

    private RegAccessPrinter lastInstructionWritePrinter;

    /**
     * this function is called when a  hooked instruction is executed by the emulator
     * @param backend backend environment
     * @param address instruction address
     * @param size instruction size
     * @param user process calling the emulator
     */
    @Override
    public void hook(final Backend backend, final long address, final int size, Object user) {
        if (canTrace(address)) {
            try {
                PrintStream out = System.err;
                if (redirect != null) {
                    out = redirect;
                }
                AndroidEmulatorTracer.bigCount++;
                //System.out.print("2");
                Instruction[] insns = emulator.printAssemble(out, address, size, maxLengthLibraryName, new InstructionVisitor() {
                    @Override
                    public void visitLast(StringBuilder builder) {
                        if (lastInstructionWritePrinter != null) {
                            //lastInstructionWritePrinter.print(emulator, backend, builder, address);
                        }
                    }
                    @Override
                    public void visit(StringBuilder builder, Instruction ins) {
                        RegsAccess regsAccess = ins.regsAccess();
                        if (regsAccess != null) {
                            short[] regsRead = regsAccess.getRegsRead();
                            RegAccessPrinter readPrinter = new RegAccessPrinter(address, ins, regsRead, false);
                            readPrinter.print(emulator, backend, builder, address);

                            short[] regWrite = regsAccess.getRegsWrite();
                            if (regWrite.length > 0) {
                                lastInstructionWritePrinter = new RegAccessPrinter(address + size, ins, regWrite, true);
                            }
                        }
                    }
                });
                if (listener != null) {
                    if (insns == null || insns.length != 1) {
                        throw new IllegalStateException("insns=" + Arrays.toString(insns));
                    }
                    listener.onInstruction(emulator, address, insns[0]);
                }
            } catch (BackendException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    final private class RegAccessPrinter {

        private final long address;
        private final Instruction instruction;
        private final short[] accessRegs;
        private boolean forWriteRegs;

        public RegAccessPrinter(long address, Instruction instruction, short[] accessRegs, boolean forWriteRegs) {
            this.address = address;
            this.instruction = instruction;
            this.accessRegs = accessRegs;
            this.forWriteRegs = forWriteRegs;
        }

        public void print(Emulator<?> emulator, Backend backend, StringBuilder builder, long address) {
            if (this.address != address) {
                return;
            }
            for (short reg : accessRegs) {
                if (emulator.is32Bit()) {
                    if ((reg >= Arm_const.ARM_REG_R0 && reg <= Arm_const.ARM_REG_R12) ||
                            reg == Arm_const.ARM_REG_LR || reg == Arm_const.ARM_REG_SP ||
                            reg == Arm_const.ARM_REG_CPSR) {
                        if (forWriteRegs) {
                            builder.append(" =>");
                            forWriteRegs = false;
                        }
                        if (reg == Arm_const.ARM_REG_CPSR) {
                            Cpsr cpsr = Cpsr.getArm(backend);
                            builder.append(String.format(Locale.US, " cpsr: N=%d, Z=%d, C=%d, V=%d",
                                    cpsr.isNegative() ? 1 : 0,
                                    cpsr.isZero() ? 1 : 0,
                                    cpsr.hasCarry() ? 1 : 0,
                                    cpsr.isOverflow() ? 1 : 0));
                        } else {
                            int value = backend.reg_read(reg).intValue();
                            builder.append(' ').append(instruction.regName(reg)).append("=0x").append(Long.toHexString(value & 0xffffffffL));
                        }
                    }
                } else {
                    if ((reg >= Arm64_const.ARM64_REG_X0 && reg <= Arm64_const.ARM64_REG_X28) ||
                            (reg >= Arm64_const.ARM64_REG_X29 && reg <= Arm64_const.ARM64_REG_SP)) {
                        if (forWriteRegs) {
                            builder.append(" =>");
                            forWriteRegs = false;
                        }
                        if (reg == Arm64_const.ARM64_REG_NZCV) {
                            Cpsr cpsr = Cpsr.getArm64(backend);
                            if (cpsr.isA32()) {
                                builder.append(String.format(Locale.US, " cpsr: N=%d, Z=%d, C=%d, V=%d",
                                        cpsr.isNegative() ? 1 : 0,
                                        cpsr.isZero() ? 1 : 0,
                                        cpsr.hasCarry() ? 1 : 0,
                                        cpsr.isOverflow() ? 1 : 0));
                            } else {
                                builder.append(String.format(Locale.US, " nzcv: N=%d, Z=%d, C=%d, V=%d",
                                        cpsr.isNegative() ? 1 : 0,
                                        cpsr.isZero() ? 1 : 0,
                                        cpsr.hasCarry() ? 1 : 0,
                                        cpsr.isOverflow() ? 1 : 0));
                            }
                        } else {
                            long value = backend.reg_read(reg).longValue();
                            builder.append(' ').append(instruction.regName(reg)).append("=0x").append(Long.toHexString(value));
                        }
                    } else if (reg >= Arm64_const.ARM64_REG_W0 && reg <= Arm64_const.ARM64_REG_W30) {
                        if (forWriteRegs) {
                            builder.append(" =>xxxx");
                            forWriteRegs = false;
                        }
                        int value = backend.reg_read(reg).intValue();
                        builder.append(' ').append(instruction.regName(reg)).append("=0x").append(Long.toHexString(value & 0xffffffffL));
                    }
                }
            }
        }

    }

}
