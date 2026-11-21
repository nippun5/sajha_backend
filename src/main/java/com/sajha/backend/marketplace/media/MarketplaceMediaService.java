package com.sajha.backend.marketplace.media;

import com.sajha.backend.api.ApiProblem;
import com.sajha.backend.marketplace.ListingRepository;
import com.sajha.backend.user.UserRepository;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** LOCAL DEVELOPMENT ONLY. Does not expose arbitrary filesystem paths. */
@Service
public class MarketplaceMediaService {
    private final ListingMediaRepository media;
    private final ListingRepository listings;
    private final UserRepository users;
    private final Environment environment;
    private final Path root;
    private final boolean localEnabled;

    public MarketplaceMediaService(ListingMediaRepository media,ListingRepository listings,
                                   UserRepository users, Environment environment,
                                   @Value("${app.media.local-enabled:false}") boolean enabled,
                                   @Value("${app.media.local-directory:${user.home}/.sajha-dev-media}") String directory) {
        this.media=media;this.listings=listings;this.users=users;this.environment=environment;
        this.localEnabled=enabled;
        this.root=Path.of(directory).toAbsolutePath().normalize();
    }
    private void ensureLocal() {
        String bind=environment.getProperty("server.address","");
        if (!localEnabled || Arrays.stream(environment.getActiveProfiles()).noneMatch("local"::equals)
                || !Set.of("127.0.0.1","localhost","::1").contains(bind))
            throw new ApiProblem(HttpStatus.SERVICE_UNAVAILABLE,"LOCAL_MEDIA_DISABLED",
                "Local image storage requires local profile and loopback-only server binding");
    }
    private Path safeFile(String key) {
        if (!key.matches("[a-f0-9-]{36}\\.(jpg|png)"))
            throw new ApiProblem(HttpStatus.NOT_FOUND,"MEDIA_NOT_FOUND","Image not found");
        Path candidate=root.resolve(key).normalize();
        if(!candidate.getParent().equals(root)) throw new IllegalStateException("Invalid media path");
        return candidate;
    }
    private ApiProblem notFound(){return new ApiProblem(HttpStatus.NOT_FOUND,"MEDIA_NOT_FOUND","Image not found");}
    private void requireVerified(String userId) {
        var user=users.findById(userId).orElseThrow(()->new ApiProblem(HttpStatus.UNAUTHORIZED,
            "UNAUTHENTICATED","Account not found"));
        if (!user.isPhoneVerified()) throw new ApiProblem(HttpStatus.FORBIDDEN,
            "PHONE_NOT_VERIFIED","Verify your phone before uploading images");
    }
    private void purgeOldStaged(String userId) {
        var stale=media.findByOwnerIdAndListingIdIsNullAndCreatedAtBefore(userId,
            Instant.now().minus(Duration.ofHours(24)));
        for (var entry:stale) {
            media.delete(entry);
            try {Files.deleteIfExists(safeFile(entry.getStorageKey()));} catch(IOException ignored) { }
        }
        media.flush();
    }
    @Transactional
    public Map<String,Object> stage(String userId,MultipartFile incoming) {
        ensureLocal();requireVerified(userId);
        purgeOldStaged(userId);
        if(media.countByOwnerIdAndListingIdIsNull(userId)>=12)
            throw new ApiProblem(HttpStatus.TOO_MANY_REQUESTS,"STAGING_LIMIT",
                "Too many pending uploads. Complete a listing or try again tomorrow");
        if(incoming==null || incoming.isEmpty() || incoming.getSize()>ImageSafety.MAX_INPUT_BYTES)
            throw new ApiProblem(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_IMAGE","Choose an image under 4 MB");
        byte[] input;
        try {input=incoming.getBytes();} catch(IOException e) {
            throw new ApiProblem(HttpStatus.BAD_REQUEST,"INVALID_IMAGE","Could not read uploaded image");
        }
        ImageSafety.Clean clean=ImageSafety.clean(input);
        String key=UUID.randomUUID()+"."+clean.extension();
        Path file=safeFile(key);
        try {
            Files.createDirectories(root);
            Files.write(file,clean.bytes(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
            try {
                var record=media.saveAndFlush(new ListingMedia(userId,key,clean.mime(),clean.bytes().length));
                return Map.of("id",record.getId(),"url","/api/v1/media/"+record.getId());
            } catch (RuntimeException e) {
                Files.deleteIfExists(file);
                throw e;
            }
        } catch(IOException e) {
            throw new ApiProblem(HttpStatus.INTERNAL_SERVER_ERROR,"MEDIA_STORAGE_FAILURE",
                "Unable to save local image");
        }
    }
    /** Only a current active listing's images are publicly readable. */
    @Transactional(readOnly=true)
    public Served publicImage(String mediaId) {
        ensureLocal();
        var image=media.findById(mediaId).orElseThrow(this::notFound);
        if(image.getListingId()==null) throw notFound();
        var listing=listings.findById(image.getListingId()).orElseThrow(this::notFound);
        if(!"active".equals(listing.getStatus())) throw notFound();
        try {return new Served(image.getMimeType(),Files.readAllBytes(safeFile(image.getStorageKey())));}
        catch(IOException e){throw notFound();}
    }
    public record Served(String contentType,byte[] body) {}
    @Transactional(readOnly=true)
    public List<String> urlsForListing(String listingId) {
        return media.findByListingIdOrderBySortOrderAsc(listingId).stream()
            .map(item->"/api/v1/media/"+item.getId()).toList();
    }
    /** Called in same transaction as listing CREATE/PATCH. Null means leave existing media unchanged. */
    public void applyListingMedia(String ownerId,String listingId,List<String> ids) {
        if(ids==null) return;
        if(ids.size()>5 || ids.size()!=new HashSet<>(ids).size())
            throw new ApiProblem(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_MEDIA","Select up to five unique images");
        if(!ids.isEmpty()) ensureLocal();
        List<ListingMedia> current=media.findByListingIdOrderBySortOrderAsc(listingId);
        Map<String,ListingMedia> selected=new LinkedHashMap<>();
        for(String id:ids) {
            ListingMedia entry=media.findById(id).orElseThrow(this::notFound);
            if (!ownerId.equals(entry.getOwnerId()) || (entry.getListingId()!=null && !listingId.equals(entry.getListingId())))
                throw new ApiProblem(HttpStatus.FORBIDDEN,"MEDIA_FORBIDDEN","Image does not belong to this account/listing");
            selected.put(id,entry);
        }
        for (ListingMedia entry:current) if(!selected.containsKey(entry.getId())) entry.detach();
        int order=0;
        for (ListingMedia entry:selected.values()) entry.attach(listingId,order++);
        media.saveAll(current);
        media.saveAll(selected.values());
        media.flush();
    }
}
