package com.example.minio.util;

public class FilenameUtils {

    public static String getExtension(String name){

        String substring = name.substring(name.lastIndexOf(".")+1);
        return substring;
    }

    public static String getDownloadFileName(String fileName){
        String name = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.lastIndexOf("."));
        return name;
    }
}
