package com.ghh.vehicle.test;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hello world!
 */
public class TestVehicle2 {
    private static AtomicInteger errorImages = new AtomicInteger(0);

    public static void main(String[] args) throws IOException, InterruptedException {
        Map<String, String> params = parseParams(args);
        String url = params.get("url");
        Integer nThread = params.get("thread") == null ? null : Integer.valueOf(params.get("thread"));
        String picDir = params.get("picDir");
        String outputDir = params.get("outputDir");
        Boolean sendName = Boolean.valueOf(Optional.ofNullable(params.get("sendName")).orElse("false"));
        int loop = Integer.valueOf(Optional.ofNullable(params.get("loop")).orElse("1"));
        if (url == null) {
            System.out.println("必须输入url");
            printUsage();
            return;
        }
        if (nThread == null || nThread < 1) {
            System.out.println("thread 必须大于0");
            printUsage();
            return;
        }
        if (picDir == null) {
            System.out.println("必须输入picDir");
            printUsage();
            return;
        }
        if (loop <= 0) {
            System.out.println("循环次数必须大于0");
            printUsage();
            return;
        }
        System.out.println(params);
        final File outputFilePath = outputDir == null ? null : new File(outputDir);
        if (outputFilePath != null) {
            outputFilePath.mkdirs();
        }
        if (nThread < 1) {
            System.out.println("线程数不能小于1");
            return;
        }

//        File[] files = new File(picDir).listFiles(new FilenameFilter() {
//            @Override
//            public boolean accept(File dir, String name) {
//                return name.endsWith("jpg");
//            }
//        });
        List<File> files = listFilesRecursively(new File(picDir));

        if (files == null || files.size() == 0) {
            return;
        }

        errorImages.set(0);
        long totalImages = 0L;
        AtomicInteger successImages = new AtomicInteger(0);
        Date startTime = new Date();

        ExecutorService es = Executors.newFixedThreadPool(nThread);

        for (int t = 0; t < loop; t++) {
            for (int i = 0; i < files.size(); i++) {
                totalImages++;
                File f = files.get(i);
                int cur = t;
                es.execute(() -> {
                    System.out.println("sending file " + f.getName());
                    try {
                        Map<String, Object> map = new HashMap<>();
                        map.put("GCXH", "111111");
                        map.put("TPLX", "1");
                        map.put("TPWJ", f);
                        if (sendName == true) {
                            map.put("TPMC", f.getName());
                        }
                        String response = HttpUtil.post(url, map);

                        map.clear();
                        map = null;

                        if (isSuccessRequest(response)) {
                            successImages.incrementAndGet();
                        }
                        if (outputFilePath != null) {
                            try (OutputStream fout = new FileOutputStream(outputFilePath.getAbsolutePath() + File.separator + f.getName() + "_" + cur + ".json")) {
                                fout.write(response.getBytes("UTF-8"));
                            }
                        }
                    } catch (Exception e) {
                        errorImages.incrementAndGet();
                        e.printStackTrace();
                    }
                });
            }
        }
        es.shutdown();
        while(!es.awaitTermination(1, TimeUnit.SECONDS)) {
        }
        System.out.println("执行完成");

        Date endTime = new Date();

        /*
         * print result
         */
        long elapseTime = endTime.getTime() - startTime.getTime();
        long imagesPerSec = totalImages * 1000 / elapseTime;
        long successPerSec = successImages.get() * 1000 / elapseTime;
        System.out.println("开始时间:       " + DateFormatUtils.format(startTime, "yyyy-MM-dd HH:mm:ss"));
        System.out.println("结束时间:       " + DateFormatUtils.format(endTime, "yyyy-MM-dd HH:mm:ss"));
        System.out.println("经过时间:       " + elapseTime + "毫秒");
        System.out.println("总图片数量:      " + totalImages);
        System.out.println("错误图片数量:      " + errorImages.get());
        System.out.println("每秒处理图片:     " + imagesPerSec);
        System.out.println("成功返回结果数量:   " + successImages.get());
        System.out.println("每秒成功处理图片:   " + successPerSec);
    }

    private static boolean isSuccessRequest(String response) {
        try {
            JSONObject json = JSONUtil.parseObj(response);
            Integer code = json.getInt("CODE");
            if (Integer.valueOf(0).equals(code)) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static List<File> listFilesRecursively(File dir) {
        List<File> fileList = new ArrayList<>();
        File[] files = dir.listFiles(pathname -> {
            if (pathname.isDirectory()) {
                return true;
            } else {
                return pathname.getName().toLowerCase().endsWith(".jpg");
            }
        });

        if (files != null && files.length > 0) {
            for (File f : files) {
                if (f.isDirectory()) {
                    fileList.addAll(listFilesRecursively(f));
                } else {
                    fileList.add(f);
                }
            }
        }
        return fileList;
    }

    private static void printUsage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Usage:   java -cp ./* com.ghh.vehicle.test.TestVehicle2 -url=http://localhost:8080/detect -thread=3 -picDir=/pic -outputDir=/output -loop=1 -sendName=true");
        sb.append("\nurl:           必须，接口地址");
        sb.append("\nthread:        必须，启动的线程数");
        sb.append("\npicDir:        必须，图片目录");
        sb.append("\noutputDir:     结果输出目录，不传则不输出文件");
        sb.append("\nloop:          循环次数，默认为1");
        sb.append("\nsendName:      是否发送图片名称，值为true|false, 默认为false");
        sb.append("\n\nExample: java -cp ./* com.ghh.vehicle.test.TestVehicle2 -url=http://localhost:8080/detect -thread=3 -picDir=/pic -outputDir=/output -loop=1 -sendName=false");
        System.out.println(sb);
    }

    private static Map<String, String> parseParams(String[] args) {
        Map<String, String> params = new HashMap<>();
        if (args != null) {
            Pattern p = Pattern.compile("^-(.+?)=(.*)$");
            for (String arg : args) {
                Matcher m = p.matcher(arg);
                if (m.matches()) {
                    String paramName = m.group(1);
                    String paramValue = StringUtils.trim(m.group(2));
                    if (StringUtils.isNotBlank(paramValue)) {
                        params.put(paramName, paramValue);
                    }
                }
            }
        }
        return params;
    }
}
