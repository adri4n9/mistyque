package com.adri4n9.dca.emulators;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;

/**
 * AndroidTraceEmulatorBuilder provides a wrapper around AndroidEmulatorBuilder defined by unidgb
 * the functionality has been extended to support two additional emulators
 * AndroidEmulator64Tracer
 * AndroidEmulatorTracer
 *  selection between unidgb default emulators and tracer emulators is done in the constructor
 */
public class AndroidTraceEmulatorBuilder extends AndroidEmulatorBuilder {

    // flag to choose between unidgb emulators and tracer amulators
    private boolean isDCA =false;

    /**
     *  builder for 32 bit traces emulator
     * @return AndroidEmulatorTracer
     */
    public static AndroidEmulatorBuilder for32Bit() {
        return new AndroidTraceEmulatorBuilder(false,true);
    }

    /**
     *  builder for 64 bit tracer emulator
     * @return AndroidEmulator64Tracer
     */
    public static AndroidEmulatorBuilder for64Bit() {
        return new AndroidTraceEmulatorBuilder(true,true);
    }

    /**
     * wrapper for the default unidgb emulators constructor
     *
     * @param is64Bit indicates whether the built emulators will be 64 bit =>True or 32bit => false
     */
    public AndroidTraceEmulatorBuilder(boolean is64Bit) {
        super(is64Bit);
        isDCA =false;
    }

    /**
     * contructor for the DCA tracer emulator buidler
     * @param is64Bit 64 bit emulator if True 32 otherwise
     * @param isTracer unidgb emulator if true DCA emulator if false
     */
    public AndroidTraceEmulatorBuilder(boolean is64Bit, boolean isTracer) {
        super(is64Bit);
        isDCA =isTracer;

    }

    /**
     * the build function returns an emulators based on the flags used for the constructor
     * @return AndroidEmulator [AndroidARMEmulator, AndroidARM64Emulator] or [AndroidEmulatorTracer, AndroidEmulator64Tracer] for DCA emulators
     */
    public AndroidEmulator build() {
        return !isDCA ?super.build(): is64Bit? new AndroidEmulator64Tracer(processName, rootDir, backendFactories):new AndroidEmulatorTracer(processName, rootDir, backendFactories);
    }
}
