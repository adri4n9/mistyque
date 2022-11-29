package com.adri4n9.dca.emulators;

import org.apache.commons.io.IOUtils;

import java.io.*;

public interface DCATracer {

    public ByteArrayOutputStream addressTrace= new ByteArrayOutputStream();
    public  ByteArrayOutputStream dataTrace= new ByteArrayOutputStream();

    public  ByteArrayOutputStream stackTrace= new ByteArrayOutputStream();

    public void setDCARange(long begin, long end);

    /**
     * this function is used to define a human-readable file to output the current trace.
     * call this function before running the emulated function
     * if this function is not called the content of the trace is output to the console.
     * the file is overwritten everytime a new trace is generated.
     * if multiple file are to be generated choose a different filename
     * @param file name of the file to write the trace
     */
    public void setHumanReadFile(String file);

    /**
     * generic binary file write function
     * @param fileName target file
     * @param byteArray binary data to write into the file
     * @throws IOException if case of file IO error
     */
    public static void writeToFile(String fileName, ByteArrayOutputStream byteArray) throws IOException {
        File targetFile = new File(fileName);
        OutputStream outStream = new FileOutputStream(targetFile);
        byteArray.writeTo(outStream);
        outStream.flush();
        byteArray.reset();
        IOUtils.closeQuietly(outStream);
        //System.out.println(fileName);
    }

    public void writeTrace(String path,int traceNumber, byte [] input, byte []output);
}
