package com.booksharing.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Локальне сховище файлів на диску - навмисно НЕ хмарний сервіс (S3
 * тощо), щоб не ускладнювати навчальний проєкт зовнішніми залежностями.
 * Кожен файл отримує УНІКАЛЬНЕ ім'я (UUID + оригінальне розширення),
 * щоб уникнути колізій, якщо два користувачі завантажать файли з
 * однаковою назвою одночасно.
 */
@Service
public class FileStorageService {

    private final Path uploadPath;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Не вдалося створити директорію для завантажень: " + uploadPath, ex);
        }
    }

    /**
     * Зберігає файл на диск, повертає лише ІМ'Я файлу (не повний шлях і
     * не URL) - решту (базовий URL сервера, префікс /uploads/) додає
     * викликач, який знає контекст (де саме буде доступний цей файл).
     */
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл порожній");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String filename = UUID.randomUUID() + extension;

        try {
            Path targetLocation = this.uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Не вдалося зберегти файл " + filename, ex);
        }

        return filename;
    }
}