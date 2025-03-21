package garbageSimulation;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;

/**
 * Builder for the garbage collection simulation.
 * Sets up the simulation environment with bins and vehicles.
 */
public class GarbageCollectionBuilder implements ContextBuilder<Object> {
    
    @Override
    public Context<Object> build(Context<Object> context) {
        context.setId("GarbageSimulation");
        
        // Create a continuous space for vehicle movement
        ContinuousSpaceFactory spaceFactory = 
            ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
        ContinuousSpace<Object> space = 
            spaceFactory.createContinuousSpace("space", context, 
                                               new RandomCartesianAdder<Object>(),
                                               new repast.simphony.space.continuous.WrapAroundBorders(),
                                               50, 50);
        
        // Create several garbage bins at various locations with different area types
        
        // Commercial area bins (upper right quadrant)
        GarbageBin bin1 = new GarbageBin(space, 1, 100.0, GarbageBin.AREA_COMMERCIAL);
        context.add(bin1);
        space.moveTo(bin1, 35, 40);
        
        GarbageBin bin2 = new GarbageBin(space, 2, 120.0, GarbageBin.AREA_COMMERCIAL);
        context.add(bin2);
        space.moveTo(bin2, 40, 30);
        
        GarbageBin bin3 = new GarbageBin(space, 3, 150.0, GarbageBin.AREA_COMMERCIAL);
        context.add(bin3);
        space.moveTo(bin3, 45, 35);
        
        // Residential area bins (bottom left quadrant)
        GarbageBin bin4 = new GarbageBin(space, 4, 90.0, GarbageBin.AREA_RESIDENTIAL);
        context.add(bin4);
        space.moveTo(bin4, 15, 15);
        
        GarbageBin bin5 = new GarbageBin(space, 5, 80.0, GarbageBin.AREA_RESIDENTIAL);
        context.add(bin5);
        space.moveTo(bin5, 10, 20);
        
        GarbageBin bin6 = new GarbageBin(space, 6, 100.0, GarbageBin.AREA_RESIDENTIAL);
        context.add(bin6);
        space.moveTo(bin6, 20, 10);
        
        // Low density area bins (upper left and bottom right quadrants)
        GarbageBin bin7 = new GarbageBin(space, 7, 70.0, GarbageBin.AREA_LOW_DENSITY);
        context.add(bin7);
        space.moveTo(bin7, 10, 40);
        
        GarbageBin bin8 = new GarbageBin(space, 8, 60.0, GarbageBin.AREA_LOW_DENSITY);
        context.add(bin8);
        space.moveTo(bin8, 40, 10);
        
        GarbageBin bin9 = new GarbageBin(space, 9, 80.0, GarbageBin.AREA_LOW_DENSITY);
        context.add(bin9);
        space.moveTo(bin9, 5, 30);
        
        // Create collection vehicles - all with standard capabilities
        // The type names are kept for display purposes only
        Vehicle vehicle1 = new Vehicle(space, 1, "Collector 1", 1.0);
        Vehicle vehicle2 = new Vehicle(space, 2, "Collector 2", 1.0);
        Vehicle vehicle3 = new Vehicle(space, 3, "Collector 3", 1.0);
        Vehicle vehicle4 = new Vehicle(space, 4, "Collector 4", 1.0);
        
        // Add the vehicles to the context
        context.add(vehicle1);
        context.add(vehicle2);
        context.add(vehicle3);
        context.add(vehicle4);
        
        // Place the vehicles at different starting positions
        space.moveTo(vehicle1, 25, 25); // Center
        space.moveTo(vehicle2, 10, 10); // Near residential area
        space.moveTo(vehicle3, 40, 40); // Near commercial area
        space.moveTo(vehicle4, 5, 45);  // Near low density area
        
        return context;
    }
}