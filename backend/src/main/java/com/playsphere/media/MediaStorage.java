package com.playsphere.media;import org.springframework.web.multipart.MultipartFile;public interface MediaStorage{StoredMedia upload(String folder,MultipartFile file);void delete(String publicId);}
