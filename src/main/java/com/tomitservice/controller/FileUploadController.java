package com.tomitservice.controller;

import com.mpatric.mp3agic.*;
import javazoom.jl.decoder.JavaLayerException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private final String UPLOAD_DIR = "src/main/resources/static/uploads/";

    @PostMapping("/mp3")
    public ResponseEntity<Resource> uploadFiles(@RequestParam("files") MultipartFile[] files, RedirectAttributes redirectAttributes) {

        try {
            removeAllFiles(UPLOAD_DIR);
            System.out.println("All files deleted successfully.");
        } catch (IOException e) {
            System.err.println("Error deleting files: " + e.getMessage());
        }

        try {
            for (MultipartFile file : files) {
                saveFile(file);
            }
            // 调用拼接函数
            String outputFilePath = mergeMp3Files(files);

            Mp3File mp3File = new Mp3File(outputFilePath);
            System.out.println(mp3File.getLengthInSeconds());

            File file = new File(outputFilePath);

            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            Resource resource = new FileSystemResource(file);
            HttpHeaders headers = new HttpHeaders();

            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");

            redirectAttributes.addFlashAttribute("message", "下载成功！");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    private void saveFile(MultipartFile file) throws IOException {
        Path path = Paths.get(UPLOAD_DIR + file.getOriginalFilename());
        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());
    }


    private String mergeMp3Files(MultipartFile[] files) throws IOException, InvalidDataException, UnsupportedTagException, InterruptedException {
        String outputFile = (new Date()).getTime()+".mp3";
//        try (FileOutputStream outputStream = new FileOutputStream(UPLOAD_DIR + filename)) {
//            for (MultipartFile file : files) {
//
//                Mp3File mp3File = new Mp3File(UPLOAD_DIR + file.getOriginalFilename());
//                System.out.println(mp3File.getLengthInSeconds());
//
//                FileInputStream inputStream = new FileInputStream(UPLOAD_DIR + file.getOriginalFilename());
//                byte[] buffer = new byte[1024];
//                int bytesRead;
//                while ((bytesRead = inputStream.read(buffer)) != -1) {
//                    outputStream.write(buffer, 0, bytesRead);
//                }
//                inputStream.close();
//            }
//        }
        String[] inputFiles = new String[files.length];
        for(int i=0; i<inputFiles.length; i++)
        {
            inputFiles[i] = UPLOAD_DIR+files[i].getOriginalFilename();
        }


        // 创建一个包含文件列表的文本文件
        File fileList = new File("filelist.txt");
        try (FileWriter writer = new FileWriter(fileList)) {
            for (String inputFile : inputFiles) {
                writer.write("file '" + inputFile + "'\n");
            }
        }

        // 使用 FFmpeg 进行合并
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-f", "concat", "-safe", "0", "-i", "filelist.txt", "-c", "copy", UPLOAD_DIR+outputFile);

        pb.inheritIO(); // 继承主进程的输入输出，方便查看 FFmpeg 输出日志
        Process process = pb.start();
        process.waitFor();

        // 删除临时的文件列表
        fileList.delete();

        return UPLOAD_DIR + outputFile;

    }


    public void removeAllFiles(String path) throws IOException {
        Path directory = Paths.get(path);

        // 检查目录是否存在
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            throw new IOException("Directory does not exist or is not a directory.");
        }

        // 使用DirectoryStream遍历目录中的文件
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                Files.delete(entry); // 删除文件
            }
        }
    }

    @GetMapping("/redirectSuccess")
    public String redirectToSuccess() {
        return "redirect:/success.html";
    }
}
