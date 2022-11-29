package com.adri4n9.examples;

import com.adri4n9.dca.emulators.AndroidTraceEmulatorBuilder;
import com.adri4n9.dca.emulators.DCATracer;
import com.adri4n9.dca.utils.Format;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.jni.ProxyClassFactory;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.utils.Inspector;

import java.io.*;
import java.util.Random;

public class MainActivity {
    private final AndroidEmulator emulator;

    private final DvmClass cEncryptUtil;
    private final VM vm;

    public MainActivity() {

      //set up the emulator  on this case a 32 bit emulator.
        // I do not think the process name matters that much. I would advise to  use the same as the application package
        emulator = AndroidTraceEmulatorBuilder.for32Bit()
                .setProcessName("com.adri4n9.wbctestapplication")
                .build();

        // select the proper NDK SDK uni-dgb accepts SDK 19 and 23 supported libraries can be found in unidbg-android resources
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        vm = emulator.createDalvikVM();
        vm.setDvmClassFactory(new ProxyClassFactory());

        // change if you want more information about the emulation
        vm.setVerbose(false);

        // this function loads the  target libraryTraces
        DalvikModule dm = vm.loadLibrary(new File("mystique/WBCTargets/CHOW/armeabi-v7a/libWBCexample.so"), false);
        //let´s  define the java class for the JNI interface
        cEncryptUtil = vm.resolveClass("com/adri4n9/WBCexample/MainActivity");

        //define the output file -- comment in case you want the output in the console
        //call onLoad I think this can also be traces if necessary
        dm.callJNI_OnLoad(emulator);
    }


    public void destroy() throws IOException {
        emulator.close();
    }

    /**
     * target function
     * @param input 16 byte plaintext input array
     * @param output 16 byte encrypted output
     * @return integer 0 if OK
     */
    public int encrypt(byte[] input, byte[] output) {

       // we have to indicate the type of data used by the exported method in this case the native encrypt method takes
        // two byte arrays as parameters and returns an integer.
        // I think parameter declaration is used to recognize overloaded definitions (only a hunch).
        // Older unidgb versions were stricter

        String encrypt = "encrypt([B[B)I";

        //define a vm array used to read arrays modified  by the emulated method
        ByteArray data = new ByteArray(vm, new byte[16]);

        //calls a JNI method that returns an integer in teh DvM class there are other methods for different return types
        int ret = cEncryptUtil.callStaticJniMethodInt(emulator, encrypt, input, data);

        //generic debug method (not really needed)
        Inspector.inspect(data.getValue(), "[" + emulator.getBackend() + "]aesEncryptionByteArray ");

        //copy the data returned by the emulated method to the output array
        System.arraycopy(data.getValue(),0,output,0,data.getValue().length);

        return ret;
    }

    /**
     * this function emulates de encrypt function  with randomly generated input
     * @param count number of random executions
     */
    public void run(int count){

        for (int i=0;i<count;i++){

            DCATracer dcaTracer = (DCATracer) emulator;
            //trace the entire range
            dcaTracer.setDCARange(1,0);
            //define the human-readable trace file -- comment in case you want the output in the console
            dcaTracer.setHumanReadFile("mystique/traceAnalysis/traces/lastTrace.txt");

            byte[] input = new byte[16];
            byte[] output = new byte[16];

            //generate random input
            new Random().nextBytes(input);

           int ret = this.encrypt(input,output);
            System.out.println("output::"+ Format.bytesToHex(output));

            //path for the  trace folder (as an example im using a path relative to the project)
            // I would suggest to use external path
            String tracePath = "mystique/traceAnalysis/traces/";

            //let´s write the trace file
            dcaTracer.writeTrace(tracePath, i,input, output);

            if (ret!=0){
                System.err.println("return value: "+ ret);
                break;
            }

        }
    }



    public static void main(String[] args) throws Exception {

        MainActivity main = new MainActivity();
        main.run(50);
        main.destroy();
    }

}
