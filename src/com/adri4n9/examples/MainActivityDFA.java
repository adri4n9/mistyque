package com.adri4n9.examples;

import com.adri4n9.dca.utils.Format;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.jni.ProxyClassFactory;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.utils.Inspector;

import java.io.*;
import java.nio.file.Paths;

import static java.lang.Math.min;

public class MainActivityDFA {
    private final AndroidEmulator emulator;

    private final DvmClass cEncryptUtil;
    private final VM vm;


    public MainActivityDFA() {
         emulator = AndroidEmulatorBuilder.for32Bit()
                        .setProcessName("com.adri4n9.wbctestapplication")
                        .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM();
        vm.setVerbose(false);
        vm.setDvmClassFactory(new ProxyClassFactory());
        vm.setVerbose(false);
        //iḿ setting the file relative to the path wgere the class is called from can be changed to an absolute path
        DalvikModule dm = vm.loadLibrary(new File("mystique/WBCTargets/faulted/libWBCexample.so"), false);
        cEncryptUtil = vm.resolveClass("com/adri4n9/WBCexample/MainActivity");
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

        //copy the data returned by the emulated method to the output array
        System.arraycopy(data.getValue(),0,output,0,data.getValue().length);

        return ret;
    }

    public void run( byte[] input ){
            byte[] output = new byte[16];
            int ret = this.encrypt(input,output);
            //setting the uptput in a format we can easily find using regex
        System.out.println("<<<<<"+Format.bytesToHex(output));
    }



    public static void main(String[] args) throws Exception {
        byte[] input = new byte[16];
        if(args!=null){
            byte [] param = Format.hexStringToByteArray(args[0]);

            for (int i = 0; i < min(param.length,16); i++) {
                input[i]=param[i];
            }
        }
        //System.out.println(bytesToHex(input));
        MainActivityDFA main = new MainActivityDFA();
        main.run(input);
        main.destroy();
    }

}
