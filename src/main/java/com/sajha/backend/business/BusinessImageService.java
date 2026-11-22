package com.sajha.backend.business;

import com.sajha.backend.api.ApiProblem;
import com.sajha.backend.marketplace.media.ImageSafety;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.UUID;

/** Development-only storage. In production replace with durable object storage before enabling submissions. */
@Service
public class BusinessImageService {
    private final Environment environment;
    private final boolean enabled;
    private final Path root;
    public record Stored(String key,String mime){}
    public record Served(byte[] bytes,String mime){}
    public BusinessImageService(Environment environment,
                                @Value("${app.media.local-enabled:false}") boolean enabled,
                                @Value("${app.media.local-directory:${user.home}/.sajha-dev-media}") String directory) {
        this.environment=environment;this.enabled=enabled;
        root=Path.of(directory).toAbsolutePath().normalize().resolve("business");
    }
    private void requireLocal(){
        if(!enabled || Arrays.stream(environment.getActiveProfiles()).noneMatch("local"::equals))
            throw new ApiProblem(HttpStatus.SERVICE_UNAVAILABLE,"BUSINESS_MEDIA_DISABLED",
                "Business banners need durable storage outside local development");
    }
    private Path safe(String key){
        if(key==null || !key.matches("[a-f0-9-]{36}\\.(jpg|png)"))
            throw new ApiProblem(HttpStatus.NOT_FOUND,"IMAGE_NOT_FOUND","Business banner unavailable");
        Path result=root.resolve(key).normalize();
        if(!result.getParent().equals(root)) throw new IllegalStateException("Invalid storage location");
        return result;
    }
    public Stored save(MultipartFile upload){
        requireLocal();
        if(upload==null || upload.isEmpty() || upload.getSize()>ImageSafety.MAX_INPUT_BYTES)
            throw new ApiProblem(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_BANNER","Choose a JPEG/PNG banner under 4 MB");
        try {
            ImageSafety.Clean clean=ImageSafety.clean(upload.getBytes());
            String key=UUID.randomUUID()+"."+clean.extension();
            Files.createDirectories(root);
            Files.write(safe(key),clean.bytes(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
            return new Stored(key,clean.mime());
        } catch(IOException e){
            throw new ApiProblem(HttpStatus.INTERNAL_SERVER_ERROR,"IMAGE_STORAGE_FAILURE","Could not save banner");
        }
    }
    public void discard(Stored stored){
        if(stored==null)return;
        try {Files.deleteIfExists(safe(stored.key()));}catch(IOException ignored){}
    }
    public Served load(BusinessRequest request){
        requireLocal();
        try {return new Served(Files.readAllBytes(safe(request.getBannerStorageKey())),request.getBannerMime());}
        catch(IOException e){throw new ApiProblem(HttpStatus.NOT_FOUND,"IMAGE_NOT_FOUND","Business banner unavailable");}
    }
}
