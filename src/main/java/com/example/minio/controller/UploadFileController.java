package com.example.minio.controller;

import com.example.minio.config.MinioConfig;
import com.example.minio.util.FilenameUtils;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("file")
@Slf4j
public class UploadFileController {
    @Autowired
    private MinioConfig minioConfig;

    @Resource
    private MinioClient minioClient;

    private String policy="{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetBucketLocation\"],\"Resource\":[\"arn:aws:s3:::demo\"]},{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:ListBucket\"],\"Resource\":[\"arn:aws:s3:::demo\"],\"Condition\":{\"StringEquals\":{\"s3:prefix\":[\"*\"]}}},{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::demo/**\"]}]}";
    @PostMapping("upload")
    public String uploadFile(@RequestParam(value = "file") MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new RuntimeException();
        }
        InputStream inputStream = null;
        String bucketName = minioConfig.getBucketName();

        //判断buket是否存在,不存在则创建
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient
                        .makeBucket(MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build());

                //设置访问策略
                String policys=policy.replaceAll("demo",bucketName);
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder().bucket(bucketName).config(policys).build()
                );
            }

            //设置文件存储名
            String datePath = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String extension = FilenameUtils.getExtension(fileName);
            fileName = datePath + "/" + uuid + "."+extension;
            String objectName = "/" + bucketName + "/" + fileName;
            inputStream = file.getInputStream();


            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .contentType(file.getContentType())
                            .object(fileName)
                            .stream(inputStream, inputStream.available(), -1)
                            .build());

            GetPresignedObjectUrlArgs presignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucketName)
                    .object(fileName)
                    .expiry(1, TimeUnit.DAYS)
                    .build();
            String url = minioClient.getPresignedObjectUrl(presignedObjectUrlArgs);
            System.out.println(url);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.debug("inputStream close IOException:" + e.getMessage());
                }
            }
        }

        return fileName;
    }

    /**
     * 文件下载
     *
     * @param fileName 文件名
     * @param delete   是否删除
     * @throws IOException
     */
    @GetMapping("/download")
    public void fileDownload(@RequestParam(name = "fileName") String fileName,
                             @RequestParam(defaultValue = "false") Boolean delete,
                             HttpServletResponse response) {
        String bucketName = "test";
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            if (!StringUtils.hasText(fileName)) {
                response.setHeader("Content-type", "text/html;charset=UTF-8");
                String data = "文件下载失败";
                OutputStream ps = response.getOutputStream();
                ps.write(data.getBytes("UTF-8"));
                return;
            }

            outputStream = response.getOutputStream();
            // 获取文件对象
            inputStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(fileName).build());
            byte buf[] = new byte[1024];
            int length = 0;

            //设置文件输出格式，文件生成时名字为2023/05/06/xxx.
            response.reset();
            response.setHeader("Content-Disposition", "attachment;filename=" +
                    URLEncoder.encode(FilenameUtils.getDownloadFileName(fileName), "UTF-8"));
            response.setContentType("image/"+FilenameUtils.getExtension(fileName));
            response.setCharacterEncoding("UTF-8");
            // 输出文件
            while ((length = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, length);
            }
            inputStream.close();

            // 判断：下载后是否同时删除minio上的存储文件
            if (delete) {
                minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(fileName).build());
            }

        } catch (Throwable ex) {
            response.setHeader("Content-type", "text/html;charset=UTF-8");
            String data = "文件下载失败";
            try {
                OutputStream ps = response.getOutputStream();
                ps.write(data.getBytes("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            try {
                outputStream.close();
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

