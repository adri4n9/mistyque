package com.adri4n9.dca.utils;

import capstone.Arm_const;
import capstone.api.Instruction;
import capstone.api.OpShift;
import capstone.api.arm.MemType;
import capstone.api.arm.OpInfo;
import capstone.api.arm.Operand;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.InstructionVisitor;
import com.github.unidbg.memory.MemRegion;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.sun.jna.Pointer;
import unicorn.Arm64Const;

import java.io.PrintStream;

/**
 * helper functions to print assembly code in the DCA instruction traces
 */
public class PrintHelper {

    static String assembleDetail(Emulator<?> emulator, Instruction ins, long address, boolean thumb, int maxLengthLibraryName) {
        return assembleDetail(emulator, ins, address, thumb, false, maxLengthLibraryName);
    }
    public static String assembleDetail(Emulator<?> emulator, Instruction ins, long address, boolean thumb, boolean current, int maxLengthLibraryName) {
        SvcMemory svcMemory = emulator.getSvcMemory();
        MemRegion region = svcMemory.findRegion(address);
        Memory memory = emulator.getMemory();

        StringBuilder builder = new StringBuilder();
        Module module = region != null ? null : memory.findModuleByAddress(address);


        //ARM.appendHex(builder, ins.getAddress(), 8, '0', false);
        builder.append(Long.toHexString(address));
        builder.append(": ");
        builder.append(ins);

        capstone.api.arm.OpInfo opInfo = null;
        capstone.api.arm64.OpInfo opInfo64 = null;
        if (ins.getOperands() instanceof capstone.api.arm.OpInfo) {
            opInfo = (capstone.api.arm.OpInfo) ins.getOperands();
        }
        if (ins.getOperands() instanceof capstone.api.arm64.OpInfo) {
            opInfo64 = (capstone.api.arm64.OpInfo) ins.getOperands();
        }
        if (current && (ins.getMnemonic().startsWith("ldr") || ins.getMnemonic().startsWith("str")) && opInfo != null) {
            appendMemoryDetails32(emulator, ins, opInfo, thumb, builder);
        }
        if (current && (ins.getMnemonic().startsWith("ldr") || ins.getMnemonic().startsWith("str")) && opInfo64 != null) {
            appendMemoryDetails64(emulator, ins, opInfo64, builder);
        }
        if(module!=null) {
            builder.append(":").append(module.name);
        }
        return builder.toString();
    }

    private static void appendMemoryDetails64(Emulator<?> emulator, Instruction ins, capstone.api.arm64.OpInfo opInfo, StringBuilder sb) {
        Memory memory = emulator.getMemory();
        capstone.api.arm64.MemType mem;
        long addr = -1;
        int bytesRead = 8;
        capstone.api.arm64.Operand[] op = opInfo.getOperands();

        // str w9, [sp, #0xab] based capstone.setDetail(Capstone.CS_OPT_ON);
        if (op.length == 2 &&
                op[0].getType() == capstone.Arm64_const.ARM64_OP_REG &&
                op[1].getType() == capstone.Arm64_const.ARM64_OP_MEM) {
            int regId = ins.mapToUnicornReg(op[0].getValue().getReg());
            if (regId >= Arm64Const.UC_ARM64_REG_W0 && regId <= Arm64Const.UC_ARM64_REG_W30) {
                bytesRead = 4;
            }
            mem = op[1].getValue().getMem();

            if (mem.getIndex() == 0) {
                UnidbgPointer base = UnidbgPointer.register(emulator, ins.mapToUnicornReg(mem.getBase()));
                long base_value = base == null ? 0L : base.peer;
                addr = base_value + mem.getDisp();
            }
        }

        // ldrb r0, [r1], #1
        if (op.length == 3 &&
                op[0].getType() == capstone.Arm64_const.ARM64_OP_REG &&
                op[1].getType() == capstone.Arm64_const.ARM64_OP_MEM &&
                op[2].getType() == capstone.Arm64_const.ARM64_OP_IMM) {
            int regId = ins.mapToUnicornReg(op[0].getValue().getReg());
            if (regId >= Arm64Const.UC_ARM64_REG_W0 && regId <= Arm64Const.UC_ARM64_REG_W30) {
                bytesRead = 4;
            }
            mem = op[1].getValue().getMem();
            if (mem.getIndex() == 0) {
                UnidbgPointer base = UnidbgPointer.register(emulator, ins.mapToUnicornReg(mem.getBase()));
                addr = base == null ? 0L : base.peer;
                addr += mem.getDisp();
            }
        }
        if (addr != -1) {
            if (ins.getMnemonic().startsWith("ldrb") || ins.getMnemonic().startsWith("strb")) {
                bytesRead = 1;
            }
            if (ins.getMnemonic().startsWith("ldrh") || ins.getMnemonic().startsWith("strh")) {
                bytesRead = 2;
            }
            appendAddrValue(sb, addr, memory, emulator.is64Bit(), bytesRead);
        }
    }

    private static void appendMemoryDetails32(Emulator<?> emulator, Instruction ins, OpInfo opInfo, boolean thumb, StringBuilder sb) {
        Memory memory = emulator.getMemory();
        MemType mem = null;
        long addr = -1;
        Operand[] op = opInfo.getOperands();

        // ldr rx, [pc, #0xab] or ldr.w rx, [pc, #0xcd] based capstone.setDetail(Capstone.CS_OPT_ON);
        if (op.length == 2 &&
                op[0].getType() == Arm_const.ARM_OP_REG &&
                op[1].getType() == Arm_const.ARM_OP_MEM) {
            mem = op[1].getValue().getMem();

            if (mem.getIndex() == 0 && mem.getScale() == 1 && mem.getLshift() == 0) {
                UnidbgPointer base = UnidbgPointer.register(emulator, ins.mapToUnicornReg(mem.getBase()));
                long base_value = base == null ? 0L : base.peer;
                addr = base_value + mem.getDisp();
            }

            // ldr.w r0, [r2, r0, lsl #2]
            OpShift shift;
            if (mem.getIndex() > 0 && mem.getScale() == 1 && mem.getLshift() == 0 && mem.getDisp() == 0 &&
                    (shift = op[1].getShift()) != null) {
                UnidbgPointer base = UnidbgPointer.register(emulator, ins.mapToUnicornReg(mem.getBase()));
                long base_value = base == null ? 0L : base.peer;
                UnidbgPointer index = UnidbgPointer.register(emulator, mem.getIndex());
                int index_value = index == null ? 0 : (int) index.peer;
                if (shift.getType() == Arm_const.ARM_OP_IMM) {
                    addr = base_value + ((long) index_value << shift.getValue());
                } else if (shift.getType() == Arm_const.ARM_OP_INVALID) {
                    addr = base_value + index_value;
                }
            }
        }

        // ldrb r0, [r1], #1
        if (op.length == 3 &&
                op[0].getType() == Arm_const.ARM_OP_REG &&
                op[1].getType() == Arm_const.ARM_OP_MEM &&
                op[2].getType() == Arm_const.ARM_OP_IMM) {
            mem = op[1].getValue().getMem();
            if (mem.getIndex() == 0 && mem.getScale() == 1 && mem.getLshift() == 0) {
                UnidbgPointer base = UnidbgPointer.register(emulator, ins.mapToUnicornReg(mem.getBase()));
                addr = base == null ? 0L : base.peer;
            }
        }
        if (addr != -1) {
            if (ins.mapToUnicornReg(mem.getBase()) == Arm_const.ARM_REG_PC) {
                addr += (thumb ? 4 : 8);
            }
            int bytesRead = 4;
            if (ins.getMnemonic().startsWith("ldrb") || ins.getMnemonic().startsWith("strb")) {
                bytesRead = 1;
            }
            if (ins.getMnemonic().startsWith("ldrh") || ins.getMnemonic().startsWith("strh")) {
                bytesRead = 2;
            }
            appendAddrValue(sb, addr, memory, emulator.is64Bit(), bytesRead);
            return;
        }

        // ldrd r2, r1, [r5, #4]
        if ("ldrd".equals(ins.getMnemonic()) && op.length == 3 &&
                op[0].getType() == Arm_const.ARM_OP_REG &&
                op[1].getType() == Arm_const.ARM_OP_REG &&
                op[2].getType() == Arm_const.ARM_OP_MEM) {
            mem = op[2].getValue().getMem();
            if (mem.getIndex() == 0 && mem.getScale() == 1 && mem.getLshift() == 0) {
                UnidbgPointer base = UnidbgPointer.register(emulator, ins.mapToUnicornReg(mem.getBase()));
                long base_value = base == null ? 0L : base.peer;
                addr = base_value + mem.getDisp();
                if (ins.mapToUnicornReg(mem.getBase())== Arm_const.ARM_REG_PC) {
                    addr += (thumb ? 4 : 8);
                }
                appendAddrValue(sb, addr, memory, emulator.is64Bit(), 4);
                appendAddrValue(sb, addr + emulator.getPointerSize(), memory, emulator.is64Bit(), 4);
            }
        }
    }

    private static void appendAddrValue(StringBuilder sb, long addr, Memory memory, boolean is64Bit, int bytesRead) {
        long mask = -bytesRead;
        Pointer pointer = memory.pointer(addr & mask);
        sb.append(" [0x").append(Long.toHexString(addr)).append(']');
        try {
            if (is64Bit) {
                if (pointer != null) {
                    long value;
                    switch (bytesRead) {
                        case 1:
                            value = pointer.getByte(0) & 0xff;
                            break;
                        case 2:
                            value = pointer.getShort(0) & 0xffff;
                            break;
                        case 4:
                            value = pointer.getInt(0);
                            break;
                        case 8:
                            value = pointer.getLong(0);
                            break;
                        default:
                            throw new IllegalStateException("bytesRead=" + bytesRead);
                    }
                    sb.append(" => 0x").append(Long.toHexString(value));
                    if (value < 0) {
                        sb.append(" (-0x").append(Long.toHexString(-value)).append(')');
                    } else if((value & 0x7fffffff00000000L) == 0) {
                        int iv = (int) value;
                        if (iv < 0) {
                            sb.append(" (-0x").append(Integer.toHexString(-iv)).append(')');
                        }
                    }
                } else {
                    sb.append(" => null");
                }
            } else {
                int value;
                switch (bytesRead) {
                    case 1:
                        value = pointer.getByte(0) & 0xff;
                        break;
                    case 2:
                        value = pointer.getShort(0) & 0xffff;
                        break;
                    case 4:
                        value = pointer.getInt(0);
                        break;
                    default:
                        throw new IllegalStateException("bytesRead=" + bytesRead);
                }
                sb.append(" => 0x").append(Long.toHexString(value & 0xffffffffL));
                if (value < 0) {
                    sb.append(" (-0x").append(Integer.toHexString(-value)).append(")");
                }
            }
        } catch (RuntimeException exception) {
            sb.append(" => ").append(exception.getMessage());
        }
    }

    public static void printAssemble( Emulator<?> emulator,PrintStream out, Instruction[] insns, long address, boolean thumb, int maxLengthLibraryName, InstructionVisitor visitor) {
        StringBuilder builder = new StringBuilder();
        for (Instruction ins : insns) {

            builder.append("[I] ");
            //builder.append(String.format("%10d",AndroidEmulatorTracer.bigcount));
            builder.append(assembleDetail(emulator, ins, address, thumb, maxLengthLibraryName));
            if (visitor != null) {
                visitor.visit(builder, ins);
            }
            address += ins.getSize();
        }
        out.println(builder);
    }
}
