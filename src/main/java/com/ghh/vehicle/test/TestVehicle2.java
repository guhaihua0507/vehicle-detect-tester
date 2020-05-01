package com.ghh.vehicle.test;

import cn.hutool.http.HttpUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Hello world!
 *
 */
public class TestVehicle2
{
    public static void main(String[] args) throws IOException, InterruptedException {
        Properties prop = new Properties();
        try (FileInputStream fin = new FileInputStream("params.txt")) {
            prop.load(fin);
        }

        String url = prop.getProperty("url");
        int nThread = Integer.valueOf(prop.getProperty("nThread"));
        String picDir = prop.getProperty("picDir");
        String outputFilePath = prop.getProperty("outputFilePath");
        File outputDir = new File(outputFilePath);
        outputDir.mkdirs();
        System.out.println(outputDir.getAbsolutePath() + "\n" + outputDir.getAbsoluteFile());
        if (nThread < 1) {
            System.out.println("线程数不能小于1");
            return;
        }

        File[] files = new File(picDir).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("jpg");
            }
        });

        ExecutorService es = Executors.newFixedThreadPool(nThread);
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            es.execute(() -> {
                System.out.println("sending file " + f.getName());
                try {
                    Map<String, Object> map = new HashMap<>();
                    map.put("GCXH", "111111");
                    map.put("TPLX", "1");
                    map.put("TPXX", Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(f)));
                    String response = HttpUtil.post(url, map);
                    try (OutputStream fout = new FileOutputStream(outputDir.getAbsolutePath() + File.separator + f.getName() + ".json")) {
                        fout.write(response.getBytes("UTF-8"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        es.shutdown();
        while(!es.awaitTermination(1, TimeUnit.SECONDS)) {

        }
        System.out.println("执行完成");
    }

    private static void printUsage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Usage:   java -cp ./* com.ghh.vehicle.test.TestVehicle url nThread picDir outputFile");
        sb.append("\nurl:           接口地址");
        sb.append("\nnThread:       启动的线程数");
        sb.append("\npicDir:        图片目录");
        sb.append("\noutputFile:    结果输出文件");
        sb.append("\n\nExample: java -cp ./* com.ghh.vehicle.test.TestVehicle http://localhost/detect 100 d:/pictures d:/result.txt");
        System.out.println(sb);
    }
}
