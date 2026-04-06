package com.wiyuka.fluxUI.examples;

import com.wiyuka.fluxUI.renderer.Flux;
import org.bukkit.Location;

public class FluxUtil {
    public static Flux.FluxLocation locationToFlux(Location location) {
        return new Flux.FluxLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }
}
