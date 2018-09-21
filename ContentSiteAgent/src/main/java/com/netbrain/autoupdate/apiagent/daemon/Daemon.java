package com.netbrain.autoupdate.apiagent.daemon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import com.netbrain.autoupdate.apiagent.constant.Constant;

@Component
public class Daemon {
    private  BlockingQueue<Map<String,byte[]>> fileQueue = new LinkedBlockingQueue<>();
    
    public void putFileQueue(Map<String,byte[]> map) throws Exception {
        this.fileQueue.put(map);
    }
    @PostConstruct
    public void writeFile() {
        WriteFileTask task = new WriteFileTask();
        FutureTask<Void> writeFutureTask = new FutureTask<Void>(task);
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.submit(writeFutureTask);
        
    }
    class WriteFileTask implements Callable<Void>{
        @Override
        public Void call() throws Exception {
            while(true) {
                Map<String,byte[]> map =fileQueue.take();
                for(Entry<String, byte[]> entry:map.entrySet()) {
                    String filePath=entry.getKey();
                    filePath=filePath.substring(0,filePath.indexOf(Constant.CPKG));
                    filePath=filePath+Constant.BAK;
                    writeByteToFile(entry.getValue(),filePath);
                    File file = new File(filePath);
                    file.renameTo(new File(entry.getKey()));
                }
            }
        }
    }

    private File writeByteToFile(byte[] b, String outputFile)throws IOException  {
        ByteBuffer buffer = ByteBuffer.wrap(b);
        File file = null;
        FileOutputStream os = null;
        FileChannel fc =null;
        try {
            file = new File(outputFile);
            File fileParent = file.getParentFile();
            if (!fileParent.exists()) {
                fileParent.mkdirs();
            }
            if(!file.exists()) {
                os = new FileOutputStream(file,false);
                fc = os.getChannel();
                fc.write(buffer);
            }
        }finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (fc != null) {
                    fc.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return file;
    }
}
