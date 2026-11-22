package com.sajha.backend.business;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class BusinessRequestStateTest {
    private static final Map<String,String> DISPLAY=Stream.of("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
        .collect(Collectors.toMap(d->d,d->"09:00 – 17:00"));
    private static final Map<String,Map<String,Object>> STRUCTURED=DISPLAY.keySet().stream()
        .collect(Collectors.toMap(d->d,d->Map.of("mode","open","opens","09:00","closes","17:00")));
    private static final BusinessDtos.Submission DATA=new BusinessDtos.Submission(
        "Himalayan Cafe","Restaurant & cafe","+13145550124","","Fresh food and coffee",
        true,"",null,null,null,null,null,null,DISPLAY,STRUCTURED);
    private BusinessRequest request(){return new BusinessRequest("owner",DATA,"{}","{}","key.jpg","image/jpeg");}
    @Test void newlySubmittedMustBePendingAndUnpaid(){
        var r=request();assertEquals("pending",r.getStatus());assertFalse(r.isPaymentConfirmed());assertEquals(1,r.getRevision());
    }
    @Test void cannotApproveWithoutPayment(){assertThrows(IllegalStateException.class,()->request().approve("admin"));}
    @Test void confirmedPaymentAllowsApproval(){
        var r=request();r.confirmPayment("admin","invoice-1","");r.approve("admin");
        assertEquals("approved",r.getStatus());assertEquals("active",r.getVisibility());
    }
    @Test void declineMustBlockApproval(){
        var r=request();r.decline("admin","Missing details");
        assertThrows(IllegalStateException.class,()->r.confirmPayment("admin","", ""));
    }
    @Test void correctionClearsPaymentAndReturnsToPending(){
        var r=request();r.confirmPayment("admin","abc","");r.decline("admin","Missing details");
        r.resubmit(DATA,"{}","{}","updated.jpg","image/jpeg");
        assertEquals("pending",r.getStatus());assertFalse(r.isPaymentConfirmed());assertEquals(2,r.getRevision());
    }
    @Test void suspensionIsSeparateFromApproval(){
        var r=request();r.confirmPayment("admin","","");r.approve("admin");r.suspend("Policy review needed");
        assertEquals("approved",r.getStatus());assertEquals("suspended",r.getVisibility());
        r.restore();assertEquals("active",r.getVisibility());
    }
}
