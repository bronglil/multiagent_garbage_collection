package garbageSimulation;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.util.ContextUtils;

/**
 * GarbageBin agent that collects garbage and broadcasts when it needs to be emptied.
 */
public class GarbageBin {
    private ContinuousSpace<Object> space;
    private int id;
    private double fillLevel;      // Current amount of garbage in the bin
    private double capacity;       // Maximum capacity of the bin
    private boolean isBeingServiced = false;
    private long serviceStartTime = 0;
    
    // Area type constants
    public static final String AREA_COMMERCIAL = "COMMERCIAL";
    public static final String AREA_RESIDENTIAL = "RESIDENTIAL";
    public static final String AREA_LOW_DENSITY = "LOW_DENSITY";
    
    // The type of area this bin is located in
    private String areaType;
    
    // Broadcasting parameters
    private static final double BROADCAST_RADIUS = 20.0;
    private static final double FULLNESS_THRESHOLD = 0.7;  // 70% full threshold
    private long lastBroadcastTime = 0;
    private static final long BROADCAST_COOLDOWN = 5000;  // 5 second cooldown between broadcasts
    private static final long SERVICE_TIMEOUT = 12000;    // 12 second timeout for service (reduced from 20)
    private int serviceTimeoutCount = 0;
    
    /**
     * Create a new garbage bin.
     * 
     * @param space Continuous space the bin exists in
     * @param id Unique identifier for this bin
     * @param capacity Maximum capacity of the bin
     * @param areaType Type of area (COMMERCIAL, RESIDENTIAL, or LOW_DENSITY)
     */
    public GarbageBin(ContinuousSpace<Object> space, int id, double capacity, String areaType) {
        this.space = space;
        this.id = id;
        this.capacity = capacity;
        this.areaType = areaType;
        
        // Set initial fill level based on area type
        switch (areaType) {
            case AREA_COMMERCIAL:
                this.fillLevel = capacity * 0.6;  // Start at 60% full
                break;
            case AREA_RESIDENTIAL:
                this.fillLevel = capacity * 0.5;  // Start at 50% full
                break;
            case AREA_LOW_DENSITY:
                this.fillLevel = capacity * 0.3;  // Start at 30% full
                break;
            default:
                this.fillLevel = capacity * 0.5;  // Default to 50% full
                break;
        }
    }
    
    /**
     * Broadcast bin status to nearby vehicles when full enough.
     * Scheduled to run every 5 ticks starting from tick 1.
     */
    @ScheduledMethod(start = 1, interval = 5)
    public void broadcastStatus() {
        // Check for service timeout
        if (isBeingServiced && System.currentTimeMillis() - serviceStartTime > SERVICE_TIMEOUT) {
            serviceTimeoutCount++;
            System.out.println("Garbage Bin " + id + " (" + areaType + ") service timeout (" + 
                               serviceTimeoutCount + ") - resetting service flag");
            isBeingServiced = false;
            
            // Force release through the coordinator if we've seen multiple timeouts
            if (serviceTimeoutCount > 2) {
                TaskCoordinator.getInstance().forceReleaseAllBinAssignments(id);
                serviceTimeoutCount = 0;
            }
        }
        
        // Only broadcast if bin is above threshold, not being serviced, and cooldown has passed
        double fillPercentage = fillLevel / capacity;
        
        if (fillPercentage >= FULLNESS_THRESHOLD && !isBeingServiced && 
            System.currentTimeMillis() - lastBroadcastTime >= BROADCAST_COOLDOWN) {
            
            NdPoint myPoint = space.getLocation(this);
            Context<Object> context = ContextUtils.getContext(this);
            
            // Determine urgency based on fullness
            boolean isUrgent = fillPercentage >= 0.9;  // 90% or more is urgent
            
            // Find vehicles in broadcast range
            int vehiclesNotified = 0;
            
            for (Object obj : context) {
                if (obj instanceof Vehicle) {
                    Vehicle vehicle = (Vehicle) obj;
                    NdPoint vehiclePoint = space.getLocation(vehicle);
                    double distance = space.getDistance(myPoint, vehiclePoint);
                    
                    // Use wider broadcast range for urgent bins
                    double effectiveRange = isUrgent ? BROADCAST_RADIUS * 1.5 : BROADCAST_RADIUS;
                    
                    if (distance <= effectiveRange) {
                        // Create bin status message
                        String urgencyFlag = isUrgent ? "URGENT" : "NORMAL";
                        String content = "BIN_STATUS:" + id + ":" + myPoint.getX() + ":" + myPoint.getY() + 
                                       ":" + fillLevel + ":" + capacity + ":" + areaType + ":" + urgencyFlag;
                        Message binMsg = new Message(id, "BIN_BROADCAST", content);
                        
                        // Send to vehicle
                        vehicle.receiveMessage(binMsg);
                        vehiclesNotified++;
                    }
                }
            }
            
            if (vehiclesNotified > 0) {
                System.out.println("Garbage Bin " + id + " (" + areaType + ") is " + 
                                 String.format("%.1f", fillPercentage * 100) + "% full" + 
                                 (isUrgent ? " (URGENT)" : "") + 
                                 " - broadcasting to " + vehiclesNotified + " vehicles");
                lastBroadcastTime = System.currentTimeMillis();
            }
        }
    }
    
    /**
     * Add garbage to the bin based on area type.
     * Scheduled to run every 20 ticks starting from tick 10.
     */
    @ScheduledMethod(start = 10, interval = 20)
    public void addGarbage() {
        // Skip if being serviced
        if (isBeingServiced) return;
        
        // Determine fill rate based on area type
        double fillRate = 0;
        switch (areaType) {
            case AREA_COMMERCIAL:
                // Commercial areas fill faster (7-13% of capacity)
                fillRate = capacity * 0.10 * (0.7 + Math.random() * 0.6);
                break;
            case AREA_RESIDENTIAL:
                // Residential areas fill at medium rate (3.5-7.5% of capacity)
                fillRate = capacity * 0.05 * (0.7 + Math.random() * 0.6);
                break;
            case AREA_LOW_DENSITY:
                // Low density areas fill slower (1.4-3% of capacity)
                fillRate = capacity * 0.02 * (0.7 + Math.random() * 0.6);
                break;
            default:
                // Default fill rate
                fillRate = capacity * 0.05 * Math.random();
                break;
        }
        
        // Add garbage
        fill(fillRate);
    }
    
    /**
     * Completely empty the bin.
     * 
     * @return Amount of garbage removed
     */
    public double emptyBin() {
        double amount = this.fillLevel;
        this.fillLevel = 0;
        this.isBeingServiced = false;
        this.serviceTimeoutCount = 0;
        System.out.println("Garbage Bin " + id + " (" + areaType + ") has been completely emptied");
        return amount;
    }
    
    /**
     * Partially empty the bin by the specified amount.
     * 
     * @param amount Amount of garbage to remove
     * @return Actual amount removed
     */
    public double reduceLevel(double amount) {
        if (amount > this.fillLevel) {
            amount = this.fillLevel;
        }
        double collected = amount;
        this.fillLevel -= amount;
        this.isBeingServiced = false;
        this.serviceTimeoutCount = 0;
        System.out.println("Garbage Bin " + id + " (" + areaType + ") has been partially emptied - now at " + 
                          String.format("%.1f", getFillPercentage()) + "% capacity");
        return collected;
    }
    
    /**
     * Mark this bin as being serviced by a vehicle.
     */
    public void markAsBeingServiced() {
        this.isBeingServiced = true;
        this.serviceStartTime = System.currentTimeMillis();
        System.out.println("Garbage Bin " + id + " (" + areaType + ") is now marked for service");
    }
    
    /**
     * Add a specified amount of garbage to the bin.
     * 
     * @param amount Amount of garbage to add
     */
    public void fill(double amount) {
        double oldFillLevel = fillLevel;
        fillLevel = Math.min(capacity, fillLevel + amount);
        
        // Log when bin exceeds the threshold
        double oldFillPercentage = oldFillLevel / capacity;
        double newFillPercentage = fillLevel / capacity;
        
        if (oldFillPercentage < FULLNESS_THRESHOLD && newFillPercentage >= FULLNESS_THRESHOLD) {
            System.out.println("Garbage Bin " + id + " (" + areaType + ") has just crossed the " + 
                              (FULLNESS_THRESHOLD * 100) + "% threshold!");
        }
    }
    
    // Getter methods
    
    public int getId() {
        return id;
    }
    
    public double getFillLevel() {
        return fillLevel;
    }
    
    public double getCapacity() {
        return capacity;
    }
    
    public double getFillPercentage() {
        return (fillLevel / capacity) * 100;
    }
    
    public boolean isFull() {
        return (fillLevel / capacity) >= FULLNESS_THRESHOLD;
    }
    
    public boolean isBeingServiced() {
        return isBeingServiced;
    }
    
    public String getAreaType() {
        return areaType;
    }
}