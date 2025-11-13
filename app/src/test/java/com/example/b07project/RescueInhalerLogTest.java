package com.example.b07project;

import static org.junit.Assert.*;
import com.example.b07project.models.RescueInhalerLog;
import org.junit.Before;
import org.junit.Test;
import java.util.Date;

public class RescueInhalerLogTest {
    
    private RescueInhalerLog log;
    private String testUserId;
    private Date testTimestamp;
    private int testDoseCount;
    private String testNotes;
    
    @Before
    public void setUp() {
        testUserId = "user123";
        testTimestamp = new Date();
        testDoseCount = 2;
        testNotes = "Felt wheezy after exercise";
        
        log = new RescueInhalerLog(testUserId, testTimestamp, testDoseCount, testNotes);
    }
    
    @Test
    public void testConstructorWithAllParameters() {
        assertNotNull("Log should not be null", log);
        assertEquals("User ID should match", testUserId, log.getUserId());
        assertEquals("Timestamp should match", testTimestamp, log.getTimestamp());
        assertEquals("Dose count should match", testDoseCount, log.getDoseCount());
        assertEquals("Notes should match", testNotes, log.getNotes());
    }
    
    @Test
    public void testEmptyConstructor() {
        RescueInhalerLog emptyLog = new RescueInhalerLog();
        assertNotNull("Empty log should not be null", emptyLog);
        assertNull("User ID should be null", emptyLog.getUserId());
        assertNull("Timestamp should be null", emptyLog.getTimestamp());
        assertEquals("Dose count should be 0", 0, emptyLog.getDoseCount());
        assertNull("Notes should be null", emptyLog.getNotes());
    }
    
    @Test
    public void testSettersAndGetters() {
        RescueInhalerLog newLog = new RescueInhalerLog();
        
        String id = "doc123";
        String userId = "user456";
        Date timestamp = new Date();
        int doseCount = 3;
        String notes = "Test notes";
        
        newLog.setId(id);
        newLog.setUserId(userId);
        newLog.setTimestamp(timestamp);
        newLog.setDoseCount(doseCount);
        newLog.setNotes(notes);
        
        assertEquals("ID should match", id, newLog.getId());
        assertEquals("User ID should match", userId, newLog.getUserId());
        assertEquals("Timestamp should match", timestamp, newLog.getTimestamp());
        assertEquals("Dose count should match", doseCount, newLog.getDoseCount());
        assertEquals("Notes should match", notes, newLog.getNotes());
    }
    
    @Test
    public void testDoseCountBoundaries() {
        log.setDoseCount(1);
        assertEquals("Minimum dose count should be 1", 1, log.getDoseCount());
        
        log.setDoseCount(10);
        assertEquals("Maximum dose count should be 10", 10, log.getDoseCount());
    }
    
    @Test
    public void testNullNotes() {
        RescueInhalerLog logWithoutNotes = new RescueInhalerLog(
            testUserId, testTimestamp, testDoseCount, null
        );
        assertNull("Notes can be null", logWithoutNotes.getNotes());
    }
    
    @Test
    public void testEmptyNotes() {
        RescueInhalerLog logWithEmptyNotes = new RescueInhalerLog(
            testUserId, testTimestamp, testDoseCount, ""
        );
        assertEquals("Empty notes should be empty string", "", logWithEmptyNotes.getNotes());
    }
    
    @Test
    public void testTimestampNotNull() {
        assertNotNull("Timestamp should not be null", log.getTimestamp());
    }
    
    @Test
    public void testUserIdNotNull() {
        assertNotNull("User ID should not be null", log.getUserId());
        assertFalse("User ID should not be empty", log.getUserId().isEmpty());
    }
    
    @Test
    public void testDoseCountPositive() {
        assertTrue("Dose count should be positive", log.getDoseCount() > 0);
    }
}
