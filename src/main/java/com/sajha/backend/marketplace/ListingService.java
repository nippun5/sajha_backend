package com.sajha.backend.marketplace;

import com.sajha.backend.marketplace.media.MarketplaceMediaService;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.sajha.backend.api.ApiProblem;
import com.sajha.backend.user.AppUser;
import com.sajha.backend.user.UserRepository;
import com.sajha.backend.user.VerifiedContactPhoneRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ListingService {
    private final ListingRepository listings;
    private final SavedListingRepository saved;
    private final UserRepository users;
    private final VerifiedContactPhoneRepository phones;
    private final JsonMapper mapper;
    private final MarketplaceMediaService media;

    public ListingService(ListingRepository listings,SavedListingRepository saved,
                          UserRepository users,VerifiedContactPhoneRepository phones,JsonMapper mapper,MarketplaceMediaService media){
        this.listings=listings;this.saved=saved;this.users=users;this.phones=phones;this.mapper=mapper;this.media=media;
    }
    private AppUser user(String id){
        return users.findById(id).orElseThrow(()->new ApiProblem(HttpStatus.UNAUTHORIZED,
            "UNAUTHENTICATED","Account not found"));
    }
    private void canPublish(AppUser user,String contactPhone){
        if(!user.isPhoneVerified()) throw new ApiProblem(HttpStatus.FORBIDDEN,
            "PHONE_NOT_VERIFIED","Verify your account phone before posting");
        if(!phones.existsByUserIdAndPhone(user.getId(),contactPhone))
            throw new ApiProblem(HttpStatus.FORBIDDEN,"CONTACT_NOT_VERIFIED",
                "The listing contact phone must be verified for your account");
    }
    private String encode(JsonNode details){
        try {return mapper.writeValueAsString(details);} catch(JacksonException e){
            throw new ApiProblem(HttpStatus.BAD_REQUEST,"INVALID_DETAILS","Invalid listing details");
        }
    }
    private JsonNode decode(String json){
        try{return mapper.readTree(json);} catch(JacksonException e){
            throw new IllegalStateException("Stored listing details are invalid",e);
        }
    }
    private String fingerprint(ListingDtos.Create input){
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            byte[] serialized=mapper.writeValueAsBytes(input);
            return HexFormat.of().formatHex(digest.digest(serialized));
        } catch (JacksonException | NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
    }
    private ListingDtos.View view(MarketplaceListing l){
        return new ListingDtos.View(l.getId(),l.getOwnerId(),l.getOwnerDisplayName(),
            l.getKind(),l.getTitle(),l.getDescription(),l.getContactPhone(),l.getLocation(),
            media.urlsForListing(l.getId()),decode(l.getDetailsJson()),l.getStatus(),l.getCreatedAt(),l.getUpdatedAt(),l.getVersion());
    }
    private ListingDtos.PageView page(Page<MarketplaceListing> entries){
        return new ListingDtos.PageView(entries.getContent().stream().map(this::view).toList(),
            entries.getNumber(),entries.getSize(),entries.getTotalElements(),entries.getTotalPages());
    }
    private Pageable paging(int page,int size){
        if(page<0 || size<1 || size>100) throw new ApiProblem(HttpStatus.BAD_REQUEST,
            "INVALID_PAGE","page must be >=0 and size must be 1–100");
        return PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt"));
    }
    @Transactional(readOnly=true)
    public ListingDtos.PageView browse(String kind,String query,int page,int size){
        if(kind!=null && !kind.isBlank() && !List.of("product","room","job").contains(kind))
            throw new ApiProblem(HttpStatus.BAD_REQUEST,"INVALID_KIND","Invalid listing kind");
        if(query!=null && query.length()>80) throw new ApiProblem(HttpStatus.BAD_REQUEST,
            "INVALID_QUERY","Search may not exceed 80 characters");
        // Escape SQL wildcard behavior by dropping %/_ rather than interpreting them as a pattern.
        String term=query==null?null:query.trim().toLowerCase(Locale.ROOT).replace("%","").replace("_","");
        if(term!=null && term.isBlank()) term=null;
        return page(listings.findPublic(kind==null||kind.isBlank()?null:kind,term,paging(page,size)));
    }
    @Transactional(readOnly=true)
    public ListingDtos.View publicOne(String id){
        MarketplaceListing l=listings.findById(id).orElseThrow(()->notFound());
        if(!"active".equals(l.getStatus())) throw notFound();
        return view(l);
    }
    @Transactional(readOnly=true)
    public ListingDtos.PageView mine(String userId,int page,int size){
        user(userId);
        return page(listings.findByOwnerIdAndStatusNot(userId,"deleted",paging(page,size)));
    }
    @Transactional
    public ListingDtos.View create(String userId,ListingDtos.Create input,String idempotencyKey){
        AppUser account=user(userId);
        ListingValidation.validateBase(input.kind(),input.title(),input.description(),input.contactPhone(),input.location());
        ListingValidation.validateDetails(input.kind(),input.details());
        canPublish(account,input.contactPhone());
        String key=null;
        if(idempotencyKey!=null && !idempotencyKey.isBlank()){
            if(!idempotencyKey.matches("[a-zA-Z0-9-]{8,80}")) throw new ApiProblem(HttpStatus.BAD_REQUEST,
                "INVALID_IDEMPOTENCY_KEY","Idempotency-Key must be 8–80 letters, numbers or dashes");
            key=idempotencyKey;
        }
        String hash=fingerprint(input);
        if(key!=null){
            var previous=listings.findByOwnerIdAndIdempotencyKey(userId,key);
            if(previous.isPresent()){
                if(!hash.equals(previous.get().getRequestFingerprint()))
                    throw new ApiProblem(HttpStatus.CONFLICT,"IDEMPOTENCY_CONFLICT","Key used for a different request");
                return view(previous.get());
            }
        }
        var created=listings.saveAndFlush(new MarketplaceListing(userId,account.getName(),input.kind(),
            input.title().trim(),input.description().trim(),input.contactPhone(),input.location().trim(),
            encode(input.details()),key,hash));
        media.applyListingMedia(userId,created.getId(),input.mediaIds());
        return view(created);
    }
    @Transactional
    public ListingDtos.View edit(String userId,String id,ListingDtos.Patch input){
        var l=ownedActiveOrHidden(userId,id);
        AppUser user=user(userId);
        String title=input.title()==null?l.getTitle():input.title();
        String description=input.description()==null?l.getDescription():input.description();
        String contact=input.contactPhone()==null?l.getContactPhone():input.contactPhone();
        String location=input.location()==null?l.getLocation():input.location();
        JsonNode details=input.details()==null?decode(l.getDetailsJson()):input.details();
        ListingValidation.validateBase(l.getKind(),title,description,contact,location);
        ListingValidation.validateDetails(l.getKind(),details);
        canPublish(user,contact);
        l.update(title.trim(),description.trim(),contact,location.trim(),encode(details));
        var changed=listings.saveAndFlush(l);
        media.applyListingMedia(userId,l.getId(),input.mediaIds());
        return view(changed);
    }
    @Transactional
    public void delete(String userId,String id){
        MarketplaceListing l=listings.findById(id).orElseThrow(()->notFound());
        if(!l.getOwnerId().equals(userId)) throw notFound();
        if(!"deleted".equals(l.getStatus())) {l.delete();listings.saveAndFlush(l);}
    }
    @Transactional
    public void save(String userId,String listingId){
        user(userId);
        MarketplaceListing l=listings.findById(listingId).orElseThrow(()->notFound());
        if(!"active".equals(l.getStatus())) throw notFound();
        saved.insertIfMissing(UUID.randomUUID().toString(),userId,listingId,Instant.now());
    }
    @Transactional
    public void unsave(String userId,String listingId){
        user(userId);
        saved.removeSaved(userId,listingId);
    }
    @Transactional(readOnly=true)
    public ListingDtos.PageView saved(String userId,int page,int size){
        user(userId);
        return page(listings.findSaved(userId,paging(page,size)));
    }
    private MarketplaceListing ownedActiveOrHidden(String userId,String id){
        MarketplaceListing l=listings.findById(id).orElseThrow(()->notFound());
        if(!l.getOwnerId().equals(userId)||"deleted".equals(l.getStatus())) throw notFound();
        return l;
    }
    private ApiProblem notFound(){return new ApiProblem(HttpStatus.NOT_FOUND,"LISTING_NOT_FOUND","Listing not found");}
}
